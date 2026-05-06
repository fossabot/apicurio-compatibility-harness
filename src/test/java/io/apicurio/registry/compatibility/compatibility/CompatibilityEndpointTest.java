package io.apicurio.registry.compatibility.compatibility;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.apicurio.registry.compatibility.shared.AbstractCompatibilityTest;
import io.apicurio.registry.compatibility.shared.SchemaFixtures;
import io.restassured.response.Response;

@DisplayName("Compatibility Endpoint Compatibility Tests")
class CompatibilityEndpointTest extends AbstractCompatibilityTest {

    private String subject() {
        return subjectName("compatibility");
    }

    @AfterEach
    void cleanup() {
        deleteSubjectPermanently(subject());
    }

    private DualResponse checkCompatibility(String subject, int version, String schemaJson) {
        String body = "{\"schema\": " + escapeJson(schemaJson) + "}";

        Response confluentResp = given()
                .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                .body(body)
                .post(confluentUrl() + "/compatibility/subjects/{subject}/versions/{version}",
                        subject, version);

        Response apicurioResp = given()
                .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                .body(body)
                .post(apicurioUrl() + "/compatibility/subjects/{subject}/versions/{version}",
                        subject, version);

        return new DualResponse(confluentResp, apicurioResp);
    }

    @Nested
    @DisplayName("POST /compatibility/subjects/{subject}/versions/{version}")
    class CheckCompatibility {

        @Test
        @DisplayName("Backward-compatible schema returns isCompatible: true")
        void backwardCompatibleSchema() {
            DualResponse registration = registerSchema(subject(), SchemaFixtures.USER_V1);
            assertEquals(200, registration.confluent().statusCode());
            assertEquals(200, registration.apicurio().statusCode());

            DualResponse compat = checkCompatibility(subject(), 1,
                    SchemaFixtures.USER_V2_ADD_FIELD);

            assertEquals(200, compat.confluent().statusCode());
            assertEquals(200, compat.apicurio().statusCode());

            assertCompatibility("backwardCompatibleSchema", "POST",
                    "/compatibility/subjects/{subject}/versions/{version}",
                    compat.confluent(), compat.apicurio());
        }

        @Test
        @DisplayName("Adding field without default under BACKWARD returns isCompatible: false")
        void addFieldWithoutDefault() {
            DualResponse registration = registerSchema(subject(), SchemaFixtures.USER_V1);
            assertEquals(200, registration.confluent().statusCode());
            assertEquals(200, registration.apicurio().statusCode());

            DualResponse compat = checkCompatibility(subject(), 1,
                    SchemaFixtures.USER_V2_ADD_FIELD_NO_DEFAULT);

            assertEquals(200, compat.confluent().statusCode());
            assertEquals(200, compat.apicurio().statusCode());

            assertCompatibility("addFieldWithoutDefault", "POST",
                    "/compatibility/subjects/{subject}/versions/{version}",
                    compat.confluent(), compat.apicurio());
        }

        @Test
        @DisplayName("Invalid schema returns error response")
        void invalidSchema() {
            DualResponse registration = registerSchema(subject(), SchemaFixtures.USER_V1);
            assertEquals(200, registration.confluent().statusCode());
            assertEquals(200, registration.apicurio().statusCode());

            DualResponse compat = checkCompatibility(subject(), 1,
                    SchemaFixtures.INVALID_SCHEMA);

            assertEquals(compat.confluent().statusCode(), compat.apicurio().statusCode(),
                    "Error status codes should match for invalid schema");

            assertCompatibility("invalidSchema", "POST",
                    "/compatibility/subjects/{subject}/versions/{version}",
                    compat.confluent(), compat.apicurio());
        }
    }
}
