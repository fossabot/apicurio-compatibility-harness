package io.apicurio.registry.compatibility.mode;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.apicurio.registry.compatibility.shared.AbstractCompatibilityTest;
import io.restassured.response.Response;

@DisplayName("Mode Endpoint Compatibility Tests")
class ModeEndpointTest extends AbstractCompatibilityTest {

    @AfterEach
    void resetMode() {
        String resetBody = "{\"mode\": \"READWRITE\"}";
        try {
            given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(resetBody)
                    .put(confluentUrl() + "/mode");
        } catch (Exception ignored) {
        }
        try {
            given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(resetBody)
                    .put(apicurioUrl() + "/mode");
        } catch (Exception ignored) {
        }
    }

    @Nested
    @DisplayName("GET /mode")
    class GetMode {

        @Test
        @DisplayName("Returns global mode as READWRITE by default")
        void getMode_default() {
            Response confluent = given().get(confluentUrl() + "/mode");
            Response apicurio = given().get(apicurioUrl() + "/mode");

            assertCompatibility("getMode_default", "GET", "/mode",
                    confluent, apicurio, false);
        }
    }

    @Nested
    @DisplayName("PUT /mode")
    class SetMode {

        @Test
        @DisplayName("Sets global mode to READONLY")
        void setMode_readonly() {
            String body = "{\"mode\": \"READONLY\"}";

            Response confluent = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(confluentUrl() + "/mode");
            Response apicurio = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(apicurioUrl() + "/mode");

            assertCompatibility("setMode_readonly", "PUT", "/mode",
                    confluent, apicurio, false);
        }

        @Test
        @DisplayName("Verifies mode change is reflected on subsequent GET")
        void setMode_andVerify() {
            String body = "{\"mode\": \"READONLY\"}";
            given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(confluentUrl() + "/mode");
            given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(apicurioUrl() + "/mode");

            Response confluent = given().get(confluentUrl() + "/mode");
            Response apicurio = given().get(apicurioUrl() + "/mode");

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("setMode_andVerify", "GET", "/mode",
                    confluent, apicurio, false);
        }

        @Test
        @DisplayName("Rejects invalid mode value with 422")
        void setMode_invalidMode() {
            String body = "{\"mode\": \"INVALID_MODE\"}";

            Response confluent = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(confluentUrl() + "/mode");
            Response apicurio = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .put(apicurioUrl() + "/mode");

            assertEquals(422, confluent.statusCode());
            assertEquals(422, apicurio.statusCode());

            assertCompatibility("setMode_invalidMode", "PUT", "/mode",
                    confluent, apicurio);
        }
    }
}
