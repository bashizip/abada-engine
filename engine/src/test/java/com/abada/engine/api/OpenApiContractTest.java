package com.abada.engine.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void generatedOpenApiContainsTheFrozenV1SurfaceAndTypedErrors() throws Exception {
        String content = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode actual = objectMapper.readTree(content);
        JsonNode contract = readContract();

        contract.path("paths").fields().forEachRemaining(path -> {
            assertThat(actual.path("paths").has(path.getKey())).as("OpenAPI path %s", path.getKey()).isTrue();
            path.getValue().forEach(method -> {
                JsonNode operation = actual.path("paths").path(path.getKey()).path(method.asText());
                assertThat(operation.isMissingNode()).as("OpenAPI operation %s %s", method, path.getKey()).isFalse();
                for (String status : List.of("400", "401", "403", "404", "409", "500")) {
                    assertThat(operation.path("responses").has(status))
                            .as("typed %s response on %s %s", status, method, path.getKey()).isTrue();
                }
            });
        });

        JsonNode schemas = actual.path("components").path("schemas");
        contract.path("schemas").forEach(schema ->
                assertThat(schemas.has(schema.asText())).as("OpenAPI schema %s", schema.asText()).isTrue());
        contract.path("schemaProperties").fields().forEachRemaining(schema -> {
            Set<String> expectedProperties = objectMapper.convertValue(schema.getValue(),
                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
            assertThat(schemas.path(schema.getKey()).path("properties").fieldNames())
                    .as("stable properties for %s", schema.getKey())
                    .toIterable().containsAll(expectedProperties);
        });
        assertThat(schemas.has("ProcessDefinitionEntity")).isFalse();
        assertThat(schemas.has("ActivityHistoryEntity")).isFalse();
    }

    @Test
    void unmappedApiRoutesAlsoReturnTheTypedErrorEnvelope() throws Exception {
        mvc.perform(get("/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code")
                        .value("RESOURCE_NOT_FOUND"));
    }

    private JsonNode readContract() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/contracts/api-v1-contract.json")) {
            if (stream == null) throw new IllegalStateException("Missing API v1 contract manifest");
            return objectMapper.readTree(stream);
        }
    }
}
