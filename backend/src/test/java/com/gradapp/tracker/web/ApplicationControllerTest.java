package com.gradapp.tracker.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String createApplication(String company, String role, String location) throws Exception {
        String body = """
                {"company":"%s","role":"%s","location":"%s"}
                """.formatted(company, role, location);

        String response = mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentStage").value("NOT_YET_APPLIED"))
                .andReturn().getResponse().getContentAsString();

        return response.replaceAll(".*\"id\":\"([0-9a-fA-F-]+)\".*", "$1");
    }

    @Test
    void createsApplicationWithDefaultStage() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company\":\"Acme Corp\",\"role\":\"Backend Engineer\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.company").value("Acme Corp"))
                .andExpect(jsonPath("$.currentStage").value("NOT_YET_APPLIED"))
                .andExpect(jsonPath("$.cvSubmitted").value(false))
                .andExpect(jsonPath("$.currentStageEnteredAt").exists());
    }

    @Test
    void getReturns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/applications/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchUpdatesOnlyProvidedFields() throws Exception {
        String id = createApplication("Acme Corp", "Backend Engineer", "Remote");

        mockMvc.perform(patch("/api/applications/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Applied via referral\",\"cvSubmitted\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Applied via referral"))
                .andExpect(jsonPath("$.cvSubmitted").value(true))
                .andExpect(jsonPath("$.company").value("Acme Corp"));
    }

    @Test
    void patchIgnoresCurrentStageField() throws Exception {
        String id = createApplication("Acme Corp", "Backend Engineer", "Remote");

        mockMvc.perform(patch("/api/applications/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentStage\":\"OFFER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStage").value("NOT_YET_APPLIED"));
    }

    @Test
    void stageTransitionUpdatesStageAndAppendsHistory() throws Exception {
        String id = createApplication("Acme Corp", "Backend Engineer", "Remote");

        mockMvc.perform(post("/api/applications/" + id + "/stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStage\":\"APPLIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStage").value("APPLIED"));

        mockMvc.perform(get("/api/applications/" + id + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].stage").value("NOT_YET_APPLIED"))
                .andExpect(jsonPath("$[1].stage").value("APPLIED"));
    }

    @Test
    void deleteRemovesApplication() throws Exception {
        String id = createApplication("Acme Corp", "Backend Engineer", "Remote");

        mockMvc.perform(delete("/api/applications/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/applications/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void listFiltersByStatusCompanyAndSearch() throws Exception {
        String acmeId = createApplication("Acme Corp", "Backend Engineer", "Remote");
        createApplication("Globex", "Frontend Engineer", "Sydney");

        mockMvc.perform(post("/api/applications/" + acmeId + "/stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStage\":\"INTERVIEW\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/applications").param("status", "INTERVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].company").value("Acme Corp"));

        mockMvc.perform(get("/api/applications").param("company", "globex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].company").value("Globex"));

        mockMvc.perform(get("/api/applications").param("search", "sydney"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].company").value("Globex"));
    }

    @Test
    void listReturns400ForInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/applications").param("status", "NOT_A_STAGE"))
                .andExpect(status().isBadRequest());
    }
}
