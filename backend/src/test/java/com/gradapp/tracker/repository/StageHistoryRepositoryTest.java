package com.gradapp.tracker.repository;

import com.gradapp.tracker.model.Application;
import com.gradapp.tracker.model.StageHistory;
import com.gradapp.tracker.model.StageType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StageHistoryRepositoryTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private StageHistoryRepository stageHistoryRepository;

    @Test
    void ordersHistoryByEnteredAtAscending() throws InterruptedException {
        Application application = new Application();
        application.setCompany("Acme Corp");
        application.setRole("Backend Engineer");
        application = applicationRepository.save(application);

        stageHistoryRepository.save(new StageHistory(application, StageType.NOT_YET_APPLIED));
        Thread.sleep(5);
        stageHistoryRepository.save(new StageHistory(application, StageType.APPLIED));
        Thread.sleep(5);
        stageHistoryRepository.save(new StageHistory(application, StageType.INTERVIEW));

        List<StageHistory> history = stageHistoryRepository.findByApplicationIdOrderByEnteredAtAsc(application.getId());

        assertThat(history).extracting(StageHistory::getStage)
                .containsExactly(StageType.NOT_YET_APPLIED, StageType.APPLIED, StageType.INTERVIEW);
    }

    @Test
    void findsMostRecentStageEntry() throws InterruptedException {
        Application application = new Application();
        application.setCompany("Acme Corp");
        application.setRole("Backend Engineer");
        application = applicationRepository.save(application);

        stageHistoryRepository.save(new StageHistory(application, StageType.NOT_YET_APPLIED));
        Thread.sleep(5);
        stageHistoryRepository.save(new StageHistory(application, StageType.APPLIED));

        Optional<StageHistory> latest = stageHistoryRepository.findTopByApplicationIdOrderByEnteredAtDesc(application.getId());

        assertThat(latest).isPresent();
        assertThat(latest.get().getStage()).isEqualTo(StageType.APPLIED);
    }
}
