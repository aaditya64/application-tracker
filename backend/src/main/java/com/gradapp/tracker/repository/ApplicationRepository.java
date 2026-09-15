package com.gradapp.tracker.repository;

import com.gradapp.tracker.model.Application;
import com.gradapp.tracker.model.StageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    @Query("""
            SELECT a FROM Application a
            WHERE (:status IS NULL OR a.currentStage = :status)
            AND (:company IS NULL OR LOWER(a.company) LIKE LOWER(CONCAT('%', :company, '%')))
            AND (:search IS NULL
                 OR LOWER(a.company) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(a.role) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(a.location) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    List<Application> search(@Param("status") StageType status, @Param("company") String company, @Param("search") String search);
}
