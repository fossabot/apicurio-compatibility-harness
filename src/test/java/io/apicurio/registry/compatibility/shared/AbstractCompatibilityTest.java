package io.apicurio.registry.compatibility.shared;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.apache.avro.Schema;
import org.apache.avro.SchemaNormalization;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.apicurio.registry.compatibility.collector.TestResultCollector;
import io.apicurio.registry.compatibility.config.TestConfiguration;
import io.apicurio.registry.compatibility.model.CompatibilityTestResult;
import io.apicurio.registry.compatibility.model.TestOutcome;
import io.restassured.response.Response;

@ExtendWith(CompatibilityReportExtension.class)
public abstract class AbstractCompatibilityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Schema.Parser SCHEMA_PARSER = new Schema.Parser();
    private static final String ID_FIELD = "id";
    private static final String VERSION_FIELD = "version";
    private static final String SCHEMA_FIELD = "schema";

    protected static final TestConfiguration config = new TestConfiguration();

    private final String runId = UUID.randomUUID().toString().substring(0, 8);

    protected String confluentUrl() {
        return config.getConfluentRegistryUrl();
    }

    protected String apicurioUrl() {
        return config.getApicurioRegistryUrl();
    }

    protected String subjectName(String base) {
        return getClass().getSimpleName() + "-" + runId + "-" + base;
    }

    protected DualResponse registerSchema(String subject, String schemaJson) {
        String body = "{\"schema\": " + escapeJson(schemaJson) + "}";

        Response confluentResp = given()
                .contentType(TestConfiguration.SCHEMA_REGISTRY_CONTENT_TYPE)
                .body(body)
                .post(confluentUrl() + "/subjects/{subject}/versions", subject);

        Response apicurioResp = given()
                .contentType(TestConfiguration.SCHEMA_REGISTRY_CONTENT_TYPE)
                .body(body)
                .post(apicurioUrl() + "/subjects/{subject}/versions", subject);

        return new DualResponse(confluentResp, apicurioResp);
    }

    protected void deleteSubjectPermanently(String subject) {
        try {
            given().delete(confluentUrl() + "/subjects/{subject}?permanent=true", subject);
        } catch (Exception ignored) {
        }
        try {
            given().delete(apicurioUrl() + "/subjects/{subject}?permanent=true", subject);
        } catch (Exception ignored) {
        }
    }

    protected void assertCompatibility(String testName, String method, String endpoint,
            Response confluentResp, Response apicurioResp) {
        assertCompatibility(testName, method, endpoint, confluentResp, apicurioResp, true);
    }

    protected void assertCompatibility(String testName, String method, String endpoint,
            Response confluentResp, Response apicurioResp, boolean assertStatus) {

        String confluentStatus = String.valueOf(confluentResp.statusCode());
        String apicurioStatus = String.valueOf(apicurioResp.statusCode());

        if (assertStatus) {
            assertEquals(confluentResp.statusCode(), apicurioResp.statusCode(),
                    "Status code mismatch for " + testName + ": Confluent=" + confluentStatus
                            + ", Apicurio=" + apicurioStatus);
        }

        CompatibilityTestResult result;
        String details;

        if (confluentResp.statusCode() != apicurioResp.statusCode()) {
            result = CompatibilityTestResult.FAIL;
            details = "Status mismatch - Confluent: " + confluentStatus
                    + ", Apicurio: " + apicurioStatus;
        } else if (responsesSemanticallyMatch(confluentResp, apicurioResp)) {
            result = CompatibilityTestResult.PASS;
            details = "Responses match";
        } else {
            result = CompatibilityTestResult.FAIL;
            details = "Body mismatch - Confluent: " + truncate(confluentResp.asString(), 200)
                    + ", Apicurio: " + truncate(apicurioResp.asString(), 200);
        }

        recordOutcome(testName, method, endpoint, confluentStatus, apicurioStatus, result, details);
    }

    protected void assertErrorParity(String testName, Response confluent, Response apicurio,
            int expectedStatus, int expectedErrorCode) {
        assertEquals(expectedStatus, confluent.statusCode(),
                "Confluent should return " + expectedStatus + " for " + testName);
        assertEquals(expectedStatus, apicurio.statusCode(),
                "Apicurio should return " + expectedStatus + " for " + testName);

        int confluentErrorCode = confluent.jsonPath().getInt("error_code");
        int apicurioErrorCode = apicurio.jsonPath().getInt("error_code");

        CompatibilityTestResult result;
        String details;
        if (confluentErrorCode == apicurioErrorCode) {
            result = CompatibilityTestResult.PASS;
            details = "Error codes match: " + confluentErrorCode;
        } else {
            result = CompatibilityTestResult.FAIL;
            details = "Error code mismatch: Confluent=" + confluentErrorCode
                    + ", Apicurio=" + apicurioErrorCode + " (expected " + expectedErrorCode + ")";
        }

        recordOutcome(testName, "VARIES", "error-code-parity",
                String.valueOf(confluent.statusCode()), String.valueOf(apicurio.statusCode()),
                result, details);
    }

    private void recordOutcome(String testName, String method, String endpoint,
            String confluentStatus, String apicurioStatus,
            CompatibilityTestResult result, String details) {
        TestResultCollector.getInstance().record(TestOutcome.builder()
                .testName(testName)
                .endpoint(endpoint)
                .method(method)
                .result(result)
                .confluentStatus(confluentStatus)
                .apicurioStatus(apicurioStatus)
                .details(details)
                .build());
    }

    private boolean responsesSemanticallyMatch(Response confluentResp, Response apicurioResp) {
        String confluentBody = confluentResp.asString();
        String apicurioBody = apicurioResp.asString();

        if (confluentBody.equals(apicurioBody)) {
            return true;
        }

        try {
            JsonNode confluentNode = MAPPER.readTree(confluentBody);
            JsonNode apicurioNode = MAPPER.readTree(apicurioBody);
            return jsonNodesMatch(confluentNode, apicurioNode);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean jsonNodesMatch(JsonNode confluent, JsonNode apicurio) {
        if (confluent.isObject() && apicurio.isObject()) {
            if (confluent.size() != apicurio.size()) {
                return false;
            }
            var confluentFields = confluent.fieldNames();
            while (confluentFields.hasNext()) {
                String field = confluentFields.next();
                if (!apicurio.has(field)) {
                    return false;
                }
                JsonNode cVal = confluent.get(field);
                JsonNode aVal = apicurio.get(field);

                if (isIdField(field)) {
                    if (!cVal.isNumber() || !aVal.isNumber()) {
                        return false;
                    }
                    continue;
                }

                if (SCHEMA_FIELD.equals(field) && cVal.isTextual() && aVal.isTextual()) {
                    if (schemasMatch(cVal.asText(), aVal.asText())) {
                        continue;
                    }
                    return false;
                }

                if (!jsonNodesMatch(cVal, aVal)) {
                    return false;
                }
            }
            return true;
        }

        if (confluent.isArray() && apicurio.isArray()) {
            if (confluent.size() != apicurio.size()) {
                return false;
            }
            for (int i = 0; i < confluent.size(); i++) {
                if (!jsonNodesMatch(confluent.get(i), apicurio.get(i))) {
                    return false;
                }
            }
            return true;
        }

        return confluent.equals(apicurio);
    }

    private boolean isIdField(String fieldName) {
        return ID_FIELD.equals(fieldName) || VERSION_FIELD.equals(fieldName);
    }

    private boolean schemasMatch(String schema1, String schema2) {
        try {
            Schema s1 = SCHEMA_PARSER.parse(schema1);
            Schema s2 = SCHEMA_PARSER.parse(schema2);
            return SchemaNormalization.toParsingForm(s1)
                    .equals(SchemaNormalization.toParsingForm(s2));
        } catch (Exception e) {
            return schema1.equals(schema2);
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null || s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen) + "...";
    }

    protected String escapeJson(String raw) {
        try {
            return MAPPER.writeValueAsString(raw);
        } catch (Exception e) {
            return "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }

    public record DualResponse(Response confluent, Response apicurio) {
    }
}
