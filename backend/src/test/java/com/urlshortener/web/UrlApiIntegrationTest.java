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
import org.springframework.http.HttpHeaders;
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
        registry.add("app.jwt.secret", () -> "integration-test-jwt-secret-key-32bytes");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password1"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data")
                .get("token")
                .asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void unauthenticatedCreateIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl":"https://example.com"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRedirectAndMetadataFlow() throws Exception {
        String token = registerAndGetToken("owner1@example.com");

        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl":"https://example.com/integration"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shortCode").isNotEmpty())
                .andExpect(jsonPath("$.data.ownerId").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String shortCode = body.get("data").get("shortCode").asText();

        mockMvc.perform(get("/api/v1/urls/" + shortCode)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalUrl").value("https://example.com/integration"));

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/integration"));
    }

    @Test
    void listIsScopedToOwner() throws Exception {
        String ownerToken = registerAndGetToken("owner-list@example.com");
        String otherToken = registerAndGetToken("other-list@example.com");

        mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl":"https://example.com/owner-only"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("q", "owner-only"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.totalElements").value(1));

        mockMvc.perform(get("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
                        .param("q", "owner-only"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.totalElements").value(0));
    }

    @Test
    void otherUserCannotPatchOrDelete() throws Exception {
        String ownerToken = registerAndGetToken("owner-mut@example.com");
        String otherToken = registerAndGetToken("other-mut@example.com");

        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl":"https://example.com/protected"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String shortCode = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("data")
                .get("shortCode")
                .asText();

        mockMvc.perform(patch("/api/v1/urls/" + shortCode)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DISABLED"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/urls/" + shortCode)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound());
    }

    @Test
    void patchDisableBlocksRedirectAndDeleteRemoves() throws Exception {
        String token = registerAndGetToken("owner-del@example.com");

        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
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
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(get("/" + shortCode)).andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/urls/" + shortCode)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/urls/" + shortCode)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void tourEndpointReturnsStepsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/tour"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps").isArray())
                .andExpect(jsonPath("$.data.title").value("Welcome to ShortLink"));
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        registerAndGetToken("dup@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dup@example.com","password":"password1"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }
}
