package com.gradapp.tracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String role;

    private String jobUrl;

    @Lob
    private String jobDescription;

    private String location;

    private LocalDate dateApplied;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(30)")
    private StageType currentStage = StageType.NOT_YET_APPLIED;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(30)")
    private OutcomeStatus outcomeStatus;

    @Column(nullable = false)
    private boolean cvSubmitted = false;

    @Column(nullable = false)
    private boolean coverLetterSubmitted = false;

    @Column(nullable = false)
    private boolean applicationAnswersSubmitted = false;

    @Lob
    private String notes;

    /** Path (relative to the app's working directory) of a saved PDF snapshot of jobUrl, if captured. */
    private String snapshotFilePath;

    private Instant snapshotCapturedAt;

    /**
     * True if the current snapshot was uploaded by hand rather than auto-captured.
     * columnDefinition includes an explicit DEFAULT: SQLite refuses to add a NOT NULL column
     * without one to a table that already has rows, which is exactly the table this runs against.
     */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean snapshotManuallyUploaded = false;

    /**
     * True if an auto-captured snapshot looks like it failed silently (a robot-check page, or
     * suspiciously little content) - a hint to look at it and upload one manually if needed.
     * Always false for manually uploaded snapshots, which are trusted as-is.
     */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean snapshotLikelyFaulty = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

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

    public OutcomeStatus getOutcomeStatus() {
        return outcomeStatus;
    }

    public void setOutcomeStatus(OutcomeStatus outcomeStatus) {
        this.outcomeStatus = outcomeStatus;
    }

    public boolean isCvSubmitted() {
        return cvSubmitted;
    }

    public void setCvSubmitted(boolean cvSubmitted) {
        this.cvSubmitted = cvSubmitted;
    }

    public boolean isCoverLetterSubmitted() {
        return coverLetterSubmitted;
    }

    public void setCoverLetterSubmitted(boolean coverLetterSubmitted) {
        this.coverLetterSubmitted = coverLetterSubmitted;
    }

    public boolean isApplicationAnswersSubmitted() {
        return applicationAnswersSubmitted;
    }

    public void setApplicationAnswersSubmitted(boolean applicationAnswersSubmitted) {
        this.applicationAnswersSubmitted = applicationAnswersSubmitted;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSnapshotFilePath() {
        return snapshotFilePath;
    }

    public void setSnapshotFilePath(String snapshotFilePath) {
        this.snapshotFilePath = snapshotFilePath;
    }

    public Instant getSnapshotCapturedAt() {
        return snapshotCapturedAt;
    }

    public void setSnapshotCapturedAt(Instant snapshotCapturedAt) {
        this.snapshotCapturedAt = snapshotCapturedAt;
    }

    public boolean isSnapshotManuallyUploaded() {
        return snapshotManuallyUploaded;
    }

    public void setSnapshotManuallyUploaded(boolean snapshotManuallyUploaded) {
        this.snapshotManuallyUploaded = snapshotManuallyUploaded;
    }

    public boolean isSnapshotLikelyFaulty() {
        return snapshotLikelyFaulty;
    }

    public void setSnapshotLikelyFaulty(boolean snapshotLikelyFaulty) {
        this.snapshotLikelyFaulty = snapshotLikelyFaulty;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
