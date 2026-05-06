package io.apicurio.registry.compatibility.config;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.apicurio.registry.compatibility.shared.AbstractCompatibilityTest;
import io.restassured.response.Response;

@DisplayName("Config Endpoint Compatibility Tests")
class ConfigEndpointTest extends AbstractCompatibilityTest {

    private String subject() {
        return subjectName("config");
    }

    @AfterEach
    void resetGlobalConfig() {
        String resetBody = "{\"compatibility\": \"BACKWARD\"}";
        try {
            given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(resetBody)
                    .put(confluentUrl() + "/config");
        } catch (Exception ignored) {
        }
        try {
            given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(resetBody)
                    .put(apicurioUrl() + "/config");
        } catch (Exception ignored) {
        }
        deleteSubjectPermanently(subject());
    }

    @Nested
    @DisplayName("GET /config")
    class GetGlobalConfig {

        @Test
        @DisplayName("Returns global compatibility level")
        void getGlobalConfig() {
            Response confluent = given().get(confluentUrl() + "/config");
            Response apicurio = given().get(apicurioUrl() + "/config");

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("getGlobalConfig", "GET", "/config", confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("PUT /config")
    class SetGlobalConfig {

        @Test
        @DisplayName("Sets global compatibility level to FULL")
        void setGlobalConfig() {
            String body = "{\"compatibility\": \"FULL\"}";

            Response confluent = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(confluentUrl() + "/config");
            Response apicurio = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(apicurioUrl() + "/config");

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("setGlobalConfig", "PUT", "/config", confluent, apicurio);
        }

        @Test
        @DisplayName("Rejects invalid compatibility level with 422")
        void setGlobalConfig_invalidLevel() {
            String body = "{\"compatibility\": \"INVALID_LEVEL\"}";

            Response confluent = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(confluentUrl() + "/config");
            Response apicurio = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(apicurioUrl() + "/config");

            assertEquals(422, confluent.statusCode());
            assertEquals(422, apicurio.statusCode());

            assertCompatibility("setGlobalConfig_invalidLevel", "PUT", "/config",
                    confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("GET /config/{subject}")
    class GetSubjectConfig {

        @Test
        @DisplayName("Returns 404 for subject with no config override")
        void getSubjectConfig_notFound() {
            Response confluent = given()
                    .get(confluentUrl() + "/config/{subject}", subject());
            Response apicurio = given()
                    .get(apicurioUrl() + "/config/{subject}", subject());

            assertEquals(404, confluent.statusCode());
            assertEquals(404, apicurio.statusCode());

            assertCompatibility("getSubjectConfig_notFound", "GET", "/config/{subject}",
                    confluent, apicurio);
        }

        @Test
        @DisplayName("Returns subject-level compatibility after setting it")
        void getSubjectConfig_afterSet() {
            String body = "{\"compatibility\": \"NONE\"}";
            given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(confluentUrl() + "/config/{subject}", subject());
            given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(apicurioUrl() + "/config/{subject}", subject());

            Response confluent = given()
                    .get(confluentUrl() + "/config/{subject}", subject());
            Response apicurio = given()
                    .get(apicurioUrl() + "/config/{subject}", subject());

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("getSubjectConfig_afterSet", "GET", "/config/{subject}",
                    confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("PUT /config/{subject}")
    class SetSubjectConfig {

        @Test
        @DisplayName("Sets subject-level compatibility to NONE")
        void setSubjectConfig() {
            String body = "{\"compatibility\": \"NONE\"}";

            Response confluent = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(confluentUrl() + "/config/{subject}", subject());
            Response apicurio = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(apicurioUrl() + "/config/{subject}", subject());

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("setSubjectConfig", "PUT", "/config/{subject}",
                    confluent, apicurio);
        }

        @Test
        @DisplayName("Rejects invalid compatibility level for subject with 422")
        void setSubjectConfig_invalidLevel() {
            String body = "{\"compatibility\": \"NOT_A_LEVEL\"}";

            Response confluent = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(confluentUrl() + "/config/{subject}", subject());
            Response apicurio = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(apicurioUrl() + "/config/{subject}", subject());

            assertEquals(422, confluent.statusCode());
            assertEquals(422, apicurio.statusCode());

            assertCompatibility("setSubjectConfig_invalidLevel", "PUT", "/config/{subject}",
                    confluent, apicurio);
        }
    }
}
