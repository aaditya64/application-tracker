package com.gradapp.tracker.dto;

import com.gradapp.tracker.model.StageHistory;
import com.gradapp.tracker.model.StageType;

import java.time.Instant;
import java.util.UUID;

public record StageHistoryEntry(UUID id, StageType stage, Instant enteredAt) {

    public static StageHistoryEntry from(StageHistory history) {
        return new StageHistoryEntry(history.getId(), history.getStage(), history.getEnteredAt());
    }
}
