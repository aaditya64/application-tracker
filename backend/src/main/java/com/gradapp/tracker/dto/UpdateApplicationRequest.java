package com.gradapp.tracker.dto;

import com.gradapp.tracker.model.StageType;

import java.time.LocalDate;

/**
 * Partial update payload for PATCH /api/applications/{id}.
 * Any field left null is left unchanged on the target application.
 */
public class UpdateApplicationRequest {

    private String company;
    private String role;
    private String jobUrl;
    private String jobDescription;
    private String location;
    private LocalDate dateApplied;
    private StageType currentStage;
    private Boolean cvSubmitted;
    private Boolean coverLetterSubmitted;
    private Boolean applicationAnswersSubmitted;
    private String notes;

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getDateApplied() {
        return dateApplied;
    }

    public void setDateApplied(LocalDate dateApplied) {
        this.dateApplied = dateApplied;
    }

    public StageType getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(StageType currentStage) {
        this.currentStage = currentStage;
    }

    public Boolean getCvSubmitted() {
        return cvSubmitted;
    }

    public void setCvSubmitted(Boolean cvSubmitted) {
        this.cvSubmitted = cvSubmitted;
    }

    public Boolean getCoverLetterSubmitted() {
        return coverLetterSubmitted;
    }

    public void setCoverLetterSubmitted(Boolean coverLetterSubmitted) {
        this.coverLetterSubmitted = coverLetterSubmitted;
    }

    public Boolean getApplicationAnswersSubmitted() {
        return applicationAnswersSubmitted;
    }

    public void setApplicationAnswersSubmitted(Boolean applicationAnswersSubmitted) {
        this.applicationAnswersSubmitted = applicationAnswersSubmitted;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
