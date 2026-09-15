package com.gradapp.tracker.web;

import com.gradapp.tracker.dto.UpdateApplicationRequest;
import com.gradapp.tracker.model.Application;
import com.gradapp.tracker.repository.ApplicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationRepository repository;

    public ApplicationController(ApplicationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Application> list() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Application> create(@RequestBody Application application) {
        Application saved = repository.save(application);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public Application get(@PathVariable UUID id) {
        return findOrThrow(id);
    }

    @PatchMapping("/{id}")
    public Application update(@PathVariable UUID id, @RequestBody UpdateApplicationRequest request) {
        Application application = findOrThrow(id);
        applyUpdates(application, request);
        return repository.save(application);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Application application = findOrThrow(id);
        repository.delete(application);
        return ResponseEntity.noContent().build();
    }

    private Application findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: " + id));
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
        if (request.getCurrentStage() != null) {
            application.setCurrentStage(request.getCurrentStage());
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
