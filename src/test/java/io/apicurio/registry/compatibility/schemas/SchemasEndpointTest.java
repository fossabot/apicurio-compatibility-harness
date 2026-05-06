package io.apicurio.registry.compatibility.schemas;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.apicurio.registry.compatibility.shared.AbstractCompatibilityTest;
import io.apicurio.registry.compatibility.shared.SchemaFixtures;
import io.restassured.response.Response;

@DisplayName("Schemas Endpoint Compatibility Tests")
class SchemasEndpointTest extends AbstractCompatibilityTest {

    private String subject() {
        return subjectName("schemas");
    }

    private String subject2() {
        return subjectName("schemas-2");
    }

    @AfterEach
    void cleanup() {
        deleteSubjectPermanently(subject());
        deleteSubjectPermanently(subject2());
    }

    @Nested
    @DisplayName("GET /schemas/ids/{id}")
    class GetSchemaById {

        @Test
        @DisplayName("Returns schema metadata by global ID")
        void getSchemaById() {
            DualResponse registered = registerSchema(subject(), SchemaFixtures.BASIC_RECORD);
            assertEquals(200, registered.confluent().statusCode());
            assertEquals(200, registered.apicurio().statusCode());

            int confluentId = registered.confluent().jsonPath().getInt("id");
            int apicurioId = registered.apicurio().jsonPath().getInt("id");

            Response confluent = given()
                    .get(confluentUrl() + "/schemas/ids/{id}", confluentId);
            Response apicurio = given()
                    .get(apicurioUrl() + "/schemas/ids/{id}", apicurioId);

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("getSchemaById", "GET", "/schemas/ids/{id}",
                    confluent, apicurio);
        }

        @Test
        @DisplayName("Returns 404 with error_code 40403 for nonexistent ID")
        void getSchemaById_nonexistent() {
            int fakeId = 999999999;

            Response confluent = given()
                    .get(confluentUrl() + "/schemas/ids/{id}", fakeId);
            Response apicurio = given()
                    .get(apicurioUrl() + "/schemas/ids/{id}", fakeId);

            assertEquals(404, confluent.statusCode());
            assertEquals(404, apicurio.statusCode());

            assertCompatibility("getSchemaById_nonexistent", "GET", "/schemas/ids/{id}",
                    confluent, apicurio);
        }

        @Test
        @DisplayName("Returns schema for primitive type")
        void getSchemaById_simpleString() {
            DualResponse registered = registerSchema(subject(), SchemaFixtures.SIMPLE_STRING);
            assertEquals(200, registered.confluent().statusCode());
            assertEquals(200, registered.apicurio().statusCode());

            int confluentId = registered.confluent().jsonPath().getInt("id");
            int apicurioId = registered.apicurio().jsonPath().getInt("id");

            Response confluent = given()
                    .get(confluentUrl() + "/schemas/ids/{id}", confluentId);
            Response apicurio = given()
                    .get(apicurioUrl() + "/schemas/ids/{id}", apicurioId);

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("getSchemaById_simpleString", "GET", "/schemas/ids/{id}",
                    confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("GET /schemas/ids/{id}/versions")
    class GetSchemaVersions {

        @Test
        @DisplayName("Returns all versions for a schema ID")
        void getSchemaVersions() {
            DualResponse registered = registerSchema(subject(), SchemaFixtures.BASIC_RECORD);
            assertEquals(200, registered.confluent().statusCode());
            assertEquals(200, registered.apicurio().statusCode());

            int confluentId = registered.confluent().jsonPath().getInt("id");
            int apicurioId = registered.apicurio().jsonPath().getInt("id");

            Response confluent = given()
                    .get(confluentUrl() + "/schemas/ids/{id}/versions", confluentId);
            Response apicurio = given()
                    .get(apicurioUrl() + "/schemas/ids/{id}/versions", apicurioId);

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("getSchemaVersions", "GET", "/schemas/ids/{id}/versions",
                    confluent, apicurio);
        }

        @Test
        @DisplayName("Returns versions from multiple subjects for same schema")
        void getSchemaVersions_multipleSubjects() {
            DualResponse first = registerSchema(subject(), SchemaFixtures.BASIC_RECORD);
            assertEquals(200, first.confluent().statusCode());
            assertEquals(200, first.apicurio().statusCode());

            DualResponse second = registerSchema(subject2(), SchemaFixtures.BASIC_RECORD);
            assertEquals(200, second.confluent().statusCode());
            assertEquals(200, second.apicurio().statusCode());

            int confluentId = first.confluent().jsonPath().getInt("id");
            int apicurioId = first.apicurio().jsonPath().getInt("id");

            assertEquals(confluentId, second.confluent().jsonPath().getInt("id"),
                    "Confluent should return same ID for identical schema");
            assertEquals(apicurioId, second.apicurio().jsonPath().getInt("id"),
                    "Apicurio should return same ID for identical schema");

            Response confluent = given()
                    .get(confluentUrl() + "/schemas/ids/{id}/versions", confluentId);
            Response apicurio = given()
                    .get(apicurioUrl() + "/schemas/ids/{id}/versions", apicurioId);

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("getSchemaVersions_multipleSubjects", "GET",
                    "/schemas/ids/{id}/versions", confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("GET /schemas/ids/{id}/subjects")
    class GetSchemaSubjects {

        @Test
        @DisplayName("Returns all subjects referencing a schema ID")
        void getSchemaSubjects() {
            DualResponse registered = registerSchema(subject(), SchemaFixtures.BASIC_RECORD);
            assertEquals(200, registered.confluent().statusCode());
            assertEquals(200, registered.apicurio().statusCode());

            int confluentId = registered.confluent().jsonPath().getInt("id");
            int apicurioId = registered.apicurio().jsonPath().getInt("id");

            Response confluent = given()
                    .get(confluentUrl() + "/schemas/ids/{id}/subjects", confluentId);
            Response apicurio = given()
                    .get(apicurioUrl() + "/schemas/ids/{id}/subjects", apicurioId);

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("getSchemaSubjects", "GET", "/schemas/ids/{id}/subjects",
                    confluent, apicurio);
        }

        @Test
        @DisplayName("Returns multiple subjects when same schema registered under different subjects")
        void getSchemaSubjects_multipleSubjects() {
            registerSchema(subject(), SchemaFixtures.SIMPLE_STRING);
            registerSchema(subject2(), SchemaFixtures.SIMPLE_STRING);

            Response confluentLookup = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body("{\"schema\": " + escapeJson(SchemaFixtures.SIMPLE_STRING) + "}")
                    .post(confluentUrl() + "/subjects/{subject}/versions/1", subject());
            Response apicurioLookup = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body("{\"schema\": " + escapeJson(SchemaFixtures.SIMPLE_STRING) + "}")
                    .post(apicurioUrl() + "/subjects/{subject}/versions/1", subject());

            int confluentId = confluentLookup.jsonPath().getInt("id");
            int apicurioId = apicurioLookup.jsonPath().getInt("id");

            Response confluent = given()
                    .get(confluentUrl() + "/schemas/ids/{id}/subjects", confluentId);
            Response apicurio = given()
                    .get(apicurioUrl() + "/schemas/ids/{id}/subjects", apicurioId);

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("getSchemaSubjects_multipleSubjects", "GET",
                    "/schemas/ids/{id}/subjects", confluent, apicurio);
        }
    }
}
