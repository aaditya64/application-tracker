package com.gradapp.tracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;
import java.util.UUID;

@Entity
public class StageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(30)")
    private StageType stage;

    @Column(nullable = false)
    private Instant enteredAt;

    protected StageHistory() {
    }

    public StageHistory(Application application, StageType stage) {
        this.application = application;
        this.stage = stage;
        this.enteredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public StageType getStage() {
        return stage;
    }

    public Instant getEnteredAt() {
        return enteredAt;
    }
}
