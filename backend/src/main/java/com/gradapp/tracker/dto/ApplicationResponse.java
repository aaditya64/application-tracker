package com.gradapp.tracker.dto;

import com.gradapp.tracker.model.Application;
import com.gradapp.tracker.model.OutcomeStatus;
import com.gradapp.tracker.model.StageType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        String company,
        String role,
        String jobUrl,
        String location,
        LocalDate dateApplied,
        StageType currentStage,
        Instant currentStageEnteredAt,
        OutcomeStatus outcomeStatus,
        LocalDate actionDueDate,
        String actionNotes,
        String actionEmailLink,
        boolean cvSubmitted,
        boolean coverLetterSubmitted,
        boolean applicationAnswersSubmitted,
        String notes,
        boolean snapshotAvailable,
        Instant snapshotCapturedAt,
        boolean snapshotManuallyUploaded,
        boolean snapshotLikelyFaulty,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApplicationResponse from(Application app, Instant currentStageEnteredAt) {
        return new ApplicationResponse(
                app.getId(),
                app.getCompany(),
                app.getRole(),
                app.getJobUrl(),
                app.getLocation(),
                app.getDateApplied(),
                app.getCurrentStage(),
                currentStageEnteredAt,
                app.getOutcomeStatus(),
                app.getActionDueDate(),
                app.getActionNotes(),
                app.getActionEmailLink(),
                app.isCvSubmitted(),
                app.isCoverLetterSubmitted(),
                app.isApplicationAnswersSubmitted(),
                app.getNotes(),
                app.getSnapshotFilePath() != null,
                app.getSnapshotCapturedAt(),
                app.isSnapshotManuallyUploaded(),
                app.isSnapshotLikelyFaulty(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
