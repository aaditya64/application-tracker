package com.gradapp.tracker.dto;

import com.gradapp.tracker.model.Application;
import com.gradapp.tracker.model.StageType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        String company,
        String role,
        String jobUrl,
        String jobDescription,
        String location,
        LocalDate dateApplied,
        StageType currentStage,
        Instant currentStageEnteredAt,
        boolean cvSubmitted,
        boolean coverLetterSubmitted,
        boolean applicationAnswersSubmitted,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApplicationResponse from(Application app, Instant currentStageEnteredAt) {
        return new ApplicationResponse(
                app.getId(),
                app.getCompany(),
                app.getRole(),
                app.getJobUrl(),
                app.getJobDescription(),
                app.getLocation(),
                app.getDateApplied(),
                app.getCurrentStage(),
                currentStageEnteredAt,
                app.isCvSubmitted(),
                app.isCoverLetterSubmitted(),
                app.isApplicationAnswersSubmitted(),
                app.getNotes(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
