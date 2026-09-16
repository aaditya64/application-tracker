package com.gradapp.tracker.dto;

import com.gradapp.tracker.model.OutcomeStatus;

import java.time.LocalDate;

/**
 * Partial update payload for PATCH /api/applications/{id}.
 * Any field left null is left unchanged on the target application.
 * Stage changes go through POST /api/applications/{id}/stage instead, so they're timestamped in StageHistory.
 */
public class UpdateApplicationRequest {

    private String company;
    private String role;
    private String jobUrl;
    private String location;
    private LocalDate dateApplied;
    private Boolean cvSubmitted;
    private Boolean coverLetterSubmitted;
    private Boolean applicationAnswersSubmitted;
    private String notes;
    private OutcomeStatus outcomeStatus;
    private LocalDate actionDueDate;
    private String actionNotes;
    private String actionEmailLink;

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

    public OutcomeStatus getOutcomeStatus() {
        return outcomeStatus;
    }

    public void setOutcomeStatus(OutcomeStatus outcomeStatus) {
        this.outcomeStatus = outcomeStatus;
    }

    public LocalDate getActionDueDate() {
        return actionDueDate;
    }

    public void setActionDueDate(LocalDate actionDueDate) {
        this.actionDueDate = actionDueDate;
    }

    public String getActionNotes() {
        return actionNotes;
    }

    public void setActionNotes(String actionNotes) {
        this.actionNotes = actionNotes;
    }

    public String getActionEmailLink() {
        return actionEmailLink;
    }

    public void setActionEmailLink(String actionEmailLink) {
        this.actionEmailLink = actionEmailLink;
    }
}
