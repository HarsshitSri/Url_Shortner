package com.urlshortener.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class UrlApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("urlshortener")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.base-url", () -> "http://localhost:8080");
        registry.add("spring.ai.model.chat", () -> "none");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createRedirectAndMetadataFlow() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl":"https://example.com/integration"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shortCode").isNotEmpty())
                .andExpect(jsonPath("$.data.safetyStatus").value("SAFE"))
                .andReturn();

        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String shortCode = body.get("data").get("shortCode").asText();

        mockMvc.perform(get("/api/v1/urls/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalUrl").value("https://example.com/integration"));

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/integration"));
    }

    @Test
    void createWithHttpAddsWarning() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl":"http://example.com/insecure"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings[0]").exists());
    }

    @Test
    void listSupportsSearchAndPagination() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl":"https://example.com/listable-alpha"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/urls")
                        .param("q", "listable-alpha")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }

    @Test
    void patchDisableBlocksRedirectAndDeleteRemoves() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl":"https://example.com/to-disable"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String shortCode = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("data")
                .get("shortCode")
                .asText();

        mockMvc.perform(patch("/api/v1/urls/" + shortCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(get("/" + shortCode)).andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/urls/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/urls/" + shortCode)).andExpect(status().isNotFound());
    }

    @Test
    void tourEndpointReturnsSteps() throws Exception {
        mockMvc.perform(get("/api/v1/tour"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps").isArray())
                .andExpect(jsonPath("$.data.title").value("Welcome to ShortLink"));
    }

    @Test
    void validationRejectsInvalidUrl() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl":"ftp://example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(400));
    }
}
