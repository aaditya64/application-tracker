package com.gradapp.tracker.web;

import com.gradapp.tracker.dto.ApplicationResponse;
import com.gradapp.tracker.dto.StageHistoryEntry;
import com.gradapp.tracker.dto.StageTransitionRequest;
import com.gradapp.tracker.dto.UpdateApplicationRequest;
import com.gradapp.tracker.model.Application;
import com.gradapp.tracker.model.OutcomeStatus;
import com.gradapp.tracker.model.StageHistory;
import com.gradapp.tracker.model.StageType;
import com.gradapp.tracker.repository.ApplicationRepository;
import com.gradapp.tracker.repository.StageHistoryRepository;
import com.gradapp.tracker.service.PageSnapshotService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private static final Set<StageType> OUTCOME_TRACKED_STAGES =
            EnumSet.of(StageType.OA_TEST, StageType.HIREVUE, StageType.INTERVIEW, StageType.FINAL_STAGE);

    private final ApplicationRepository repository;
    private final StageHistoryRepository stageHistoryRepository;
    private final PageSnapshotService pageSnapshotService;

    public ApplicationController(
            ApplicationRepository repository,
            StageHistoryRepository stageHistoryRepository,
            PageSnapshotService pageSnapshotService
    ) {
        this.repository = repository;
        this.stageHistoryRepository = stageHistoryRepository;
        this.pageSnapshotService = pageSnapshotService;
    }

    @GetMapping
    public List<ApplicationResponse> list(
            @RequestParam(required = false) StageType status,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String search
    ) {
        return repository.search(status, blankToNull(company), blankToNull(search)).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> create(@RequestBody Application application) {
        Application saved = repository.save(application);
        StageHistory initialHistory = stageHistoryRepository.save(new StageHistory(saved, saved.getCurrentStage()));
        if (saved.getJobUrl() != null && !saved.getJobUrl().isBlank()) {
            captureSnapshotInBackground(saved.getId(), saved.getJobUrl());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved, initialHistory.getEnteredAt()));
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable UUID id) {
        return toResponse(findOrThrow(id));
    }

    @PatchMapping("/{id}")
    public ApplicationResponse update(@PathVariable UUID id, @RequestBody UpdateApplicationRequest request) {
        Application application = findOrThrow(id);
        applyUpdates(application, request);
        return toResponse(repository.save(application));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Application application = findOrThrow(id);
        repository.delete(application);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/stage")
    public ApplicationResponse transitionStage(@PathVariable UUID id, @RequestBody StageTransitionRequest request) {
        if (request.getNewStage() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "newStage is required");
        }
        Application application = findOrThrow(id);
        application.setCurrentStage(request.getNewStage());
        application.setOutcomeStatus(OUTCOME_TRACKED_STAGES.contains(request.getNewStage())
                ? OutcomeStatus.ACTION_NEEDED
                : null);
        Application saved = repository.save(application);
        StageHistory history = stageHistoryRepository.save(new StageHistory(saved, request.getNewStage()));
        return toResponse(saved, history.getEnteredAt());
    }

    @GetMapping("/{id}/history")
    public List<StageHistoryEntry> history(@PathVariable UUID id) {
        findOrThrow(id);
        return stageHistoryRepository.findByApplicationIdOrderByEnteredAtAsc(id).stream()
                .map(StageHistoryEntry::from)
                .toList();
    }

    @PostMapping("/{id}/snapshot")
    public ResponseEntity<Void> captureSnapshot(@PathVariable UUID id) {
        Application application = findOrThrow(id);
        if (application.getJobUrl() == null || application.getJobUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application has no job URL to capture");
        }
        captureSnapshotInBackground(id, application.getJobUrl());
        return ResponseEntity.accepted().build();
    }

    @PostMapping(value = "/{id}/snapshot/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApplicationResponse uploadSnapshot(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        Application application = findOrThrow(id);

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }
        boolean looksLikePdf = MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType())
                || (file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".pdf"));
        if (!looksLikePdf) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file must be a PDF");
        }

        try {
            PageSnapshotService.SnapshotResult result = pageSnapshotService.saveManualUpload(id, file.getBytes());
            application.setSnapshotFilePath(result.filePath());
            application.setSnapshotCapturedAt(result.capturedAt());
            application.setSnapshotManuallyUploaded(true);
            application.setSnapshotLikelyFaulty(false);
            return toResponse(repository.save(application));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @GetMapping("/{id}/snapshot")
    public ResponseEntity<Resource> downloadSnapshot(@PathVariable UUID id) {
        Application application = findOrThrow(id);
        String storedPath = application.getSnapshotFilePath();
        if (storedPath == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No snapshot has been captured for this application");
        }
        Path file = pageSnapshotService.resolveSnapshotPath(storedPath);
        if (!Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Snapshot file is missing on disk");
        }

        String filename = (application.getCompany() + "-" + application.getRole())
                .replaceAll("[^a-zA-Z0-9-]+", "_") + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(new FileSystemResource(file));
    }

    private void captureSnapshotInBackground(UUID applicationId, String jobUrl) {
        pageSnapshotService.captureAsync(applicationId, jobUrl).thenAccept(result ->
                result.ifPresent(snapshot -> repository.findById(applicationId).ifPresent(app -> {
                    app.setSnapshotFilePath(snapshot.filePath());
                    app.setSnapshotCapturedAt(snapshot.capturedAt());
                    app.setSnapshotManuallyUploaded(false);
                    app.setSnapshotLikelyFaulty(snapshot.likelyFaulty());
                    repository.save(app);
                })));
    }

    private Application findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: " + id));
    }

    private ApplicationResponse toResponse(Application application) {
        Instant currentStageEnteredAt = stageHistoryRepository
                .findTopByApplicationIdOrderByEnteredAtDesc(application.getId())
                .map(StageHistory::getEnteredAt)
                .orElse(application.getUpdatedAt());
        return toResponse(application, currentStageEnteredAt);
    }

    private ApplicationResponse toResponse(Application application, Instant currentStageEnteredAt) {
        return ApplicationResponse.from(application, currentStageEnteredAt);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private void applyUpdates(Application application, UpdateApplicationRequest request) {
        if (request.getCompany() != null) {
            application.setCompany(request.getCompany());
        }
        if (request.getRole() != null) {
            application.setRole(request.getRole());
        }
        if (request.getJobUrl() != null) {
            application.setJobUrl(request.getJobUrl());
        }
        if (request.getLocation() != null) {
            application.setLocation(request.getLocation());
        }
        if (request.getDateApplied() != null) {
            application.setDateApplied(request.getDateApplied());
        }
        if (request.getCvSubmitted() != null) {
            application.setCvSubmitted(request.getCvSubmitted());
        }
        if (request.getCoverLetterSubmitted() != null) {
            application.setCoverLetterSubmitted(request.getCoverLetterSubmitted());
        }
        if (request.getApplicationAnswersSubmitted() != null) {
            application.setApplicationAnswersSubmitted(request.getApplicationAnswersSubmitted());
        }
        if (request.getNotes() != null) {
            application.setNotes(request.getNotes());
        }
        if (request.getOutcomeStatus() != null) {
            application.setOutcomeStatus(request.getOutcomeStatus());
        }
        if (request.getActionDueDate() != null) {
            application.setActionDueDate(request.getActionDueDate());
        }
        if (request.getActionNotes() != null) {
            application.setActionNotes(request.getActionNotes());
        }
        if (request.getActionEmailLink() != null) {
            application.setActionEmailLink(request.getActionEmailLink());
        }
    }
}
