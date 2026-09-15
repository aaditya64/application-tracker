package com.gradapp.tracker.web;

import com.gradapp.tracker.dto.ApplicationResponse;
import com.gradapp.tracker.dto.StageHistoryEntry;
import com.gradapp.tracker.dto.StageTransitionRequest;
import com.gradapp.tracker.dto.UpdateApplicationRequest;
import com.gradapp.tracker.model.Application;
import com.gradapp.tracker.model.StageHistory;
import com.gradapp.tracker.model.StageType;
import com.gradapp.tracker.repository.ApplicationRepository;
import com.gradapp.tracker.repository.StageHistoryRepository;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationRepository repository;
    private final StageHistoryRepository stageHistoryRepository;

    public ApplicationController(ApplicationRepository repository, StageHistoryRepository stageHistoryRepository) {
        this.repository = repository;
        this.stageHistoryRepository = stageHistoryRepository;
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
        if (request.getJobDescription() != null) {
            application.setJobDescription(request.getJobDescription());
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
    }
}
