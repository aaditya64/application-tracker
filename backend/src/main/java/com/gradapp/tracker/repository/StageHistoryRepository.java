package com.gradapp.tracker.repository;

import com.gradapp.tracker.model.StageHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StageHistoryRepository extends JpaRepository<StageHistory, UUID> {
    List<StageHistory> findByApplicationIdOrderByEnteredAtAsc(UUID applicationId);

    Optional<StageHistory> findTopByApplicationIdOrderByEnteredAtDesc(UUID applicationId);
}
