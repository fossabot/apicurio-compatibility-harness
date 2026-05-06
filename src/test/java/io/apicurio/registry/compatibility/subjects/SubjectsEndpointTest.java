package io.apicurio.registry.compatibility.subjects;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.apicurio.registry.compatibility.shared.AbstractCompatibilityTest;
import io.apicurio.registry.compatibility.shared.SchemaFixtures;
import io.restassured.response.Response;

@DisplayName("Subjects Endpoint Compatibility Tests")
class SubjectsEndpointTest extends AbstractCompatibilityTest {

    private String subject() {
        return subjectName("subjects");
    }

    private String subject2() {
        return subjectName("subjects-2");
    }

    @AfterEach
    void cleanup() {
        deleteSubjectPermanently(subject());
        deleteSubjectPermanently(subject2());
    }

    @Nested
    @DisplayName("GET /subjects")
    class ListSubjects {

        @Test
        @DisplayName("Lists subjects after registration")
        void listSubjects_afterRegistration() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            Response confluent = given().get(confluentUrl() + "/subjects");
            Response apicurio = given().get(apicurioUrl() + "/subjects");

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("listSubjects_afterRegistration", "GET", "/subjects",
                    confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("GET /subjects/{subject}/versions")
    class ListVersions {

        @Test
        @DisplayName("Lists versions under a subject")
        void listVersions() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            Response confluent = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions", subject());
            Response apicurio = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions", subject());

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("listVersions", "GET",
                    "/subjects/{subject}/versions", confluent, apicurio);
        }

        @Test
        @DisplayName("Returns 404 for nonexistent subject")
        void listVersions_nonexistentSubject() {
            String ghost = subjectName("ghost");
            Response confluent = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions", ghost);
            Response apicurio = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions", ghost);

            assertCompatibility("listVersions_nonexistentSubject", "GET",
                    "/subjects/{subject}/versions", confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("GET /subjects/{subject}/versions/{version}")
    class GetVersion {

        @Test
        @DisplayName("Gets a specific version of a schema")
        void getVersion() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            Response confluent = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions/1", subject());
            Response apicurio = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions/1", subject());

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("getVersion", "GET",
                    "/subjects/{subject}/versions/{version}", confluent, apicurio);
        }

        @Test
        @DisplayName("Returns 404 for nonexistent version")
        void getVersion_nonexistent() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            Response confluent = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions/999", subject());
            Response apicurio = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions/999", subject());

            assertCompatibility("getVersion_nonexistent", "GET",
                    "/subjects/{subject}/versions/{version}", confluent, apicurio);
        }

        @Test
        @DisplayName("Gets latest version with 'latest'")
        void getVersion_latest() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            Response confluent = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions/latest", subject());
            Response apicurio = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions/latest", subject());

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("getVersion_latest", "GET",
                    "/subjects/{subject}/versions/latest", confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("GET /subjects/{subject}/versions/{version}/schema")
    class GetSchemaOnly {

        @Test
        @DisplayName("Returns raw schema string")
        void getSchema() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            Response confluent = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions/1/schema", subject());
            Response apicurio = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions/1/schema", subject());

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("getSchema", "GET",
                    "/subjects/{subject}/versions/{version}/schema", confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("POST /subjects/{subject}/versions")
    class RegisterSchema {

        @Test
        @DisplayName("Registers a new schema")
        void registerNewSchema() {
            DualResponse dual = registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            assertEquals(200, dual.confluent().statusCode());
            assertEquals(200, dual.apicurio().statusCode());

            assertCompatibility("registerNewSchema", "POST",
                    "/subjects/{subject}/versions", dual.confluent(), dual.apicurio());
        }

        @Test
        @DisplayName("Registers same schema under different subject returns same ID")
        void registerSameSchema() {
            DualResponse first = registerSchema(subject(), SchemaFixtures.BASIC_RECORD);
            int firstConfluentId = first.confluent().jsonPath().getInt("id");
            int firstApicurioId = first.apicurio().jsonPath().getInt("id");

            DualResponse second = registerSchema(subject2(), SchemaFixtures.BASIC_RECORD);

            assertEquals(firstConfluentId, second.confluent().jsonPath().getInt("id"),
                    "Confluent should return same global ID for same schema");
            assertEquals(firstApicurioId, second.apicurio().jsonPath().getInt("id"),
                    "Apicurio should return same global ID for same schema");

            assertCompatibility("registerSameSchema", "POST",
                    "/subjects/{subject}/versions", second.confluent(), second.apicurio());
        }

        @Test
        @DisplayName("Rejects invalid schema with 422")
        void registerInvalidSchema() {
            DualResponse dual = registerSchema(subject(), SchemaFixtures.INVALID_SCHEMA);

            assertEquals(422, dual.confluent().statusCode());
            assertEquals(422, dual.apicurio().statusCode());

            assertCompatibility("registerInvalidSchema", "POST",
                    "/subjects/{subject}/versions", dual.confluent(), dual.apicurio());
        }
    }

    @Nested
    @DisplayName("DELETE /subjects/{subject}")
    class DeleteSubject {

        @Test
        @DisplayName("Deletes a subject and all versions")
        void deleteSubject() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            Response confluent = given()
                    .delete(confluentUrl() + "/subjects/{subject}", subject());
            Response apicurio = given()
                    .delete(apicurioUrl() + "/subjects/{subject}", subject());

            assertCompatibility("deleteSubject", "DELETE", "/subjects/{subject}",
                    confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("DELETE /subjects/{subject}/versions/{version}")
    class DeleteVersion {

        @Test
        @DisplayName("Soft-deletes a version")
        void softDeleteVersion() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            Response confluent = given()
                    .delete(confluentUrl() + "/subjects/{subject}/versions/1", subject());
            Response apicurio = given()
                    .delete(apicurioUrl() + "/subjects/{subject}/versions/1", subject());

            assertCompatibility("softDeleteVersion", "DELETE",
                    "/subjects/{subject}/versions/{version}", confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("POST /subjects/{subject}/versions/{version}")
    class LookupVersion {

        @Test
        @DisplayName("Looks up schema version by schema content")
        void lookupVersion() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            String body = "{\"schema\": " + escapeJson(SchemaFixtures.BASIC_RECORD) + "}";

            Response confluent = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .post(confluentUrl() + "/subjects/{subject}/versions/1", subject());
            Response apicurio = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .post(apicurioUrl() + "/subjects/{subject}/versions/1", subject());

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("lookupVersion", "POST",
                    "/subjects/{subject}/versions/{version}", confluent, apicurio);
        }

        @Test
        @DisplayName("Returns 404 when looking up unregistered schema")
        void lookupVersion_unregisteredSchema() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            String body = "{\"schema\": " + escapeJson(SchemaFixtures.SIMPLE_STRING) + "}";

            Response confluent = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .post(confluentUrl() + "/subjects/{subject}/versions/1", subject());
            Response apicurio = given()
                    .contentType(config.SCHEMA_REGISTRY_CONTENT_TYPE)
                    .body(body)
                    .post(apicurioUrl() + "/subjects/{subject}/versions/1", subject());

            assertCompatibility("lookupVersion_unregisteredSchema", "POST",
                    "/subjects/{subject}/versions/{version}", confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("GET /subjects/{subject}/versions/{version}/referencedby")
    class ReferencedBy {

        @Test
        @DisplayName("Returns schemas that reference this version")
        void referencedBy_afterRegistration() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            Response confluent = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions/1/referencedby",
                            subject());
            Response apicurio = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions/1/referencedby",
                            subject());

            assertCompatibility("referencedBy_afterRegistration", "GET",
                    "/subjects/{subject}/versions/{version}/referencedby",
                    confluent, apicurio);
        }

        @Test
        @DisplayName("Returns 404 for nonexistent subject")
        void referencedBy_nonexistentSubject() {
            String ghost = subjectName("ghost-ref");
            Response confluent = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions/1/referencedby",
                            ghost);
            Response apicurio = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions/1/referencedby",
                            ghost);

            assertCompatibility("referencedBy_nonexistentSubject", "GET",
                    "/subjects/{subject}/versions/{version}/referencedby",
                    confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("DELETE /subjects/{subject}/versions/{version}?permanent=true")
    class PermanentDeleteVersion {

        @Test
        @DisplayName("Permanently deletes a soft-deleted version")
        void permanentDeleteVersion() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            given().delete(confluentUrl() + "/subjects/{subject}/versions/1", subject());
            given().delete(apicurioUrl() + "/subjects/{subject}/versions/1", subject());

            Response confluent = given()
                    .delete(confluentUrl()
                            + "/subjects/{subject}/versions/1?permanent=true", subject());
            Response apicurio = given()
                    .delete(apicurioUrl()
                            + "/subjects/{subject}/versions/1?permanent=true", subject());

            assertCompatibility("permanentDeleteVersion", "DELETE",
                    "/subjects/{subject}/versions/{version}?permanent=true",
                    confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("DELETE /subjects/{subject}?permanent=true")
    class PermanentDeleteSubject {

        @Test
        @DisplayName("Permanently deletes a subject and all its versions")
        void permanentDeleteSubject() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);

            given().delete(confluentUrl() + "/subjects/{subject}", subject());
            given().delete(apicurioUrl() + "/subjects/{subject}", subject());

            Response confluent = given()
                    .delete(confluentUrl() + "/subjects/{subject}?permanent=true",
                            subject());
            Response apicurio = given()
                    .delete(apicurioUrl() + "/subjects/{subject}?permanent=true",
                            subject());

            assertCompatibility("permanentDeleteSubject", "DELETE",
                    "/subjects/{subject}?permanent=true", confluent, apicurio);
        }
    }

    @Nested
    @DisplayName("Multi-version registration")
    class MultiVersion {

        @Test
        @DisplayName("Registers multiple versions and verifies version numbering")
        void multiVersion_numbering() {
            registerSchema(subject(), SchemaFixtures.USER_V1);
            registerSchema(subject(), SchemaFixtures.USER_V2_ADD_FIELD);
            DualResponse v3 = registerSchema(subject(),
                    SchemaFixtures.USER_V3_ADD_ANOTHER_FIELD);

            assertEquals(200, v3.confluent().statusCode());
            assertEquals(200, v3.apicurio().statusCode());

            assertEquals(3, v3.confluent().jsonPath().getInt("version"));
            assertEquals(3, v3.apicurio().jsonPath().getInt("version"));

            assertCompatibility("multiVersion_numbering", "POST",
                    "/subjects/{subject}/versions", v3.confluent(), v3.apicurio());
        }

        @Test
        @DisplayName("latest resolves to the most recent version")
        void multiVersion_latest() {
            registerSchema(subject(), SchemaFixtures.USER_V1);
            registerSchema(subject(), SchemaFixtures.USER_V2_ADD_FIELD);

            Response confluent = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions/latest",
                            subject());
            Response apicurio = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions/latest",
                            subject());

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertEquals(2, confluent.jsonPath().getInt("version"));
            assertEquals(2, apicurio.jsonPath().getInt("version"));

            assertCompatibility("multiVersion_latest", "GET",
                    "/subjects/{subject}/versions/latest", confluent, apicurio);
        }

        @Test
        @DisplayName("Lists all versions in order")
        void multiVersion_listVersions() {
            registerSchema(subject(), SchemaFixtures.USER_V1);
            registerSchema(subject(), SchemaFixtures.USER_V2_ADD_FIELD);

            Response confluent = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions", subject());
            Response apicurio = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions", subject());

            assertEquals(200, confluent.statusCode());
            assertEquals(200, apicurio.statusCode());

            assertCompatibility("multiVersion_listVersions", "GET",
                    "/subjects/{subject}/versions", confluent, apicurio);
        }

        @Test
        @DisplayName("Gets each version by explicit version number")
        void multiVersion_getExplicit() {
            registerSchema(subject(), SchemaFixtures.USER_V1);
            registerSchema(subject(), SchemaFixtures.USER_V2_ADD_FIELD);

            Response confluentV1 = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions/1", subject());
            Response apicurioV1 = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions/1", subject());

            assertEquals(200, confluentV1.statusCode());
            assertEquals(200, apicurioV1.statusCode());

            assertCompatibility("multiVersion_getV1", "GET",
                    "/subjects/{subject}/versions/{version}", confluentV1, apicurioV1);

            Response confluentV2 = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions/2", subject());
            Response apicurioV2 = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions/2", subject());

            assertEquals(200, confluentV2.statusCode());
            assertEquals(200, apicurioV2.statusCode());

            assertCompatibility("multiVersion_getV2", "GET",
                    "/subjects/{subject}/versions/{version}", confluentV2, apicurioV2);
        }

        @Test
        @DisplayName("Registers same schema under multiple subjects and cross-lookups work")
        void multiSubject_crossLookup() {
            registerSchema(subject(), SchemaFixtures.BASIC_RECORD);
            registerSchema(subject2(), SchemaFixtures.BASIC_RECORD);

            Response confluentV1 = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions/1", subject());
            Response apicurioV1 = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions/1", subject());

            assertEquals(200, confluentV1.statusCode());
            assertEquals(200, apicurioV1.statusCode());

            assertCompatibility("multiSubject_crossLookup_subject1", "GET",
                    "/subjects/{subject}/versions/{version}", confluentV1, apicurioV1);

            Response confluentV2 = given()
                    .get(confluentUrl() + "/subjects/{subject}/versions/1", subject2());
            Response apicurioV2 = given()
                    .get(apicurioUrl() + "/subjects/{subject}/versions/1", subject2());

            assertEquals(200, confluentV2.statusCode());
            assertEquals(200, apicurioV2.statusCode());

            assertCompatibility("multiSubject_crossLookup_subject2", "GET",
                    "/subjects/{subject}/versions/{version}", confluentV2, apicurioV2);
        }
    }
}
