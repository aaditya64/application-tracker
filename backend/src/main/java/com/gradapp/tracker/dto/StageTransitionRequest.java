package com.gradapp.tracker.dto;

import com.gradapp.tracker.model.StageType;

public class StageTransitionRequest {

    private StageType newStage;

    public StageType getNewStage() {
        return newStage;
    }

    public void setNewStage(StageType newStage) {
        this.newStage = newStage;
    }
}
