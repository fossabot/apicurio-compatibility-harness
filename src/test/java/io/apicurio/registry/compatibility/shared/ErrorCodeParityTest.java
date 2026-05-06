package io.apicurio.registry.compatibility.shared;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

@DisplayName("Error Code Parity Tests")
class ErrorCodeParityTest extends AbstractCompatibilityTest {

    private String subject() {
        return subjectName("errors");
    }

    @AfterEach
    void cleanup() {
        deleteSubjectPermanently(subject());
    }

    @Nested
    @DisplayName("40401 - Subject Not Found")
    class SubjectNotFound {

        @Test
        @DisplayName("GET versions for nonexistent subject returns 40401")
        void subjectNotFound_getVersions() {
            String ghost = subjectName("ghost-40401");
            Response confluent = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions", ghost);
            Response apicurio = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions", ghost);

            assertErrorParity("subjectNotFound_getVersions", confluent, apicurio, 404, 40401);
        }

        @Test
        @DisplayName("DELETE nonexistent subject returns 40401")
        void subjectNotFound_delete() {
            String ghost = subjectName("ghost-40401-del");
            Response confluent = given()
                    .delete(confluentUrl() + "/subjects/{subject}", ghost);
            Response apicurio = given()
                    .delete(apicurioUrl() + "/subjects/{subject}", ghost);

            assertErrorParity("subjectNotFound_delete", confluent, apicurio, 404, 40401);
        }
    }

    @Nested
    @DisplayName("40402 - Version Not Found")
    class VersionNotFound {

        @Test
        @DisplayName("GET nonexistent version returns 40402")
        void versionNotFound() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            Response confluent = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions/999", subject());
            Response apicurio = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions/999", subject());

            assertErrorParity("versionNotFound", confluent, apicurio, 404, 40402);
        }
    }

    @Nested
    @DisplayName("40403 - Schema Not Found")
    class SchemaNotFound {

        @Test
        @DisplayName("GET nonexistent schema ID returns 40403")
        void schemaNotFound() {
            Response confluent = given()
                    .get(confluentUrl() + "/schemas/ids/999999");
            Response apicurio = given()
                    .get(apicurioUrl() + "/schemas/ids/999999");

            assertErrorParity("schemaNotFound", confluent, apicurio, 404, 40403);
        }
    }

    @Nested
    @DisplayName("42201 - Invalid Schema")
    class InvalidSchema {

        @Test
        @DisplayName("POST invalid JSON returns 42201")
        void invalidSchema() {
            DualResponse dual = registerSchema(subject(), SchemaFixtures.INVALID_SCHEMA);

            assertErrorParity("invalidSchema", dual.confluent(), dual.apicurio(), 422, 42201);
        }

        @Test
        @DisplayName("POST malformed Avro returns 42201")
        void malformedAvro() {
            DualResponse dual = registerSchema(subject(), SchemaFixtures.MALFORMED_AVRO);

            assertErrorParity("malformedAvro", dual.confluent(), dual.apicurio(), 422, 42201);
        }
    }

    @Nested
    @DisplayName("40901 - Incompatible Schema")
    class IncompatibleSchema {

        @Test
        @DisplayName("POST incompatible schema under BACKWARD compatibility returns 40901")
        void incompatibleSchema() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body("{\"compatibility\": \"BACKWARD\"}")
                    .put(confluentUrl() + "/config/{subject}", subject());
            given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body("{\"compatibility\": \"BACKWARD\"}")
                    .put(apicurioUrl() + "/config/{subject}", subject());

            DualResponse dual = registerSchema(subject(),
                    SchemaFixtures.USER_V2_ADD_FIELD_NO_DEFAULT);

            assertErrorParity("incompatibleSchema", dual.confluent(), dual.apicurio(), 409, 40901);
        }
    }
}
