package com.gradapp.tracker.repository;

import com.gradapp.tracker.model.Application;
import com.gradapp.tracker.model.StageType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApplicationRepositoryTest {

    @Autowired
    private ApplicationRepository repository;

    private Application persistApplication(String company, String role, String location, StageType stage) {
        Application application = new Application();
        application.setCompany(company);
        application.setRole(role);
        application.setLocation(location);
        application.setCurrentStage(stage);
        return repository.save(application);
    }

    @Test
    void findsAllWhenNoFiltersGiven() {
        persistApplication("Acme Corp", "Backend Engineer", "Remote", StageType.NOT_YET_APPLIED);
        persistApplication("Globex", "Frontend Engineer", "Sydney", StageType.APPLIED);

        List<Application> results = repository.search(null, null, null);

        assertThat(results).hasSize(2);
    }

    @Test
    void filtersByStatus() {
        persistApplication("Acme Corp", "Backend Engineer", "Remote", StageType.NOT_YET_APPLIED);
        persistApplication("Globex", "Frontend Engineer", "Sydney", StageType.APPLIED);

        List<Application> results = repository.search(StageType.APPLIED, null, null);

        assertThat(results).extracting(Application::getCompany).containsExactly("Globex");
    }

    @Test
    void filtersByCompanyCaseInsensitive() {
        persistApplication("Acme Corp", "Backend Engineer", "Remote", StageType.NOT_YET_APPLIED);
        persistApplication("Globex", "Frontend Engineer", "Sydney", StageType.APPLIED);

        List<Application> results = repository.search(null, "acme", null);

        assertThat(results).extracting(Application::getCompany).containsExactly("Acme Corp");
    }

    @Test
    void searchMatchesAcrossCompanyRoleAndLocation() {
        persistApplication("Acme Corp", "Backend Engineer", "Remote", StageType.NOT_YET_APPLIED);
        persistApplication("Globex", "Frontend Engineer", "Sydney", StageType.APPLIED);

        assertThat(repository.search(null, null, "sydney"))
                .extracting(Application::getCompany).containsExactly("Globex");
        assertThat(repository.search(null, null, "backend"))
                .extracting(Application::getCompany).containsExactly("Acme Corp");
    }

    @Test
    void combinesFiltersWithAnd() {
        persistApplication("Acme Corp", "Backend Engineer", "Remote", StageType.NOT_YET_APPLIED);
        persistApplication("Globex", "Frontend Engineer", "Sydney", StageType.APPLIED);

        List<Application> results = repository.search(StageType.APPLIED, null, "remote");

        assertThat(results).isEmpty();
    }
}
