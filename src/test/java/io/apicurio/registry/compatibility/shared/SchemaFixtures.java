package io.apicurio.registry.compatibility.shared;

/**
 * Avro schema test fixtures for compatibility testing.
 * These schemas cover various patterns needed to test the full Confluent API surface.
 */
public final class SchemaFixtures {

    private SchemaFixtures() {
    }

    // --- Simple schemas ---

    public static final String SIMPLE_STRING = """
            {"type": "string"}
            """;

    public static final String SIMPLE_INT = """
            {"type": "int"}
            """;

    // --- Record schemas ---

    public static final String BASIC_RECORD = """
            {
              "type": "record",
              "name": "User",
              "namespace": "com.example",
              "fields": [
                {"name": "id", "type": "long"},
                {"name": "name", "type": "string"},
                {"name": "email", "type": "string"}
              ]
            }
            """;

    public static final String RECORD_WITH_DEFAULTS = """
            {
              "type": "record",
              "name": "UserWithDefaults",
              "namespace": "com.example",
              "fields": [
                {"name": "id", "type": "long"},
                {"name": "name", "type": "string"},
                {"name": "active", "type": "boolean", "default": true},
                {"name": "role", "type": "string", "default": "user"}
              ]
            }
            """;

    public static final String RECORD_WITH_UNION = """
            {
              "type": "record",
              "name": "Event",
              "namespace": "com.example",
              "fields": [
                {"name": "id", "type": "long"},
                {"name": "payload", "type": ["null", "string"]},
                {"name": "timestamp", "type": "long"}
              ]
            }
            """;

    public static final String NESTED_RECORD = """
            {
              "type": "record",
              "name": "Address",
              "namespace": "com.example",
              "fields": [
                {"name": "street", "type": "string"},
                {"name": "city", "type": "string"},
                {"name": "zip", "type": "string"}
              ]
            }
            """;

    public static final String RECORD_WITH_NESTED = """
            {
              "type": "record",
              "name": "Customer",
              "namespace": "com.example",
              "fields": [
                {"name": "id", "type": "long"},
                {"name": "name", "type": "string"},
                {
                  "name": "address",
                  "type": {
                    "type": "record",
                    "name": "Address",
                    "fields": [
                      {"name": "street", "type": "string"},
                      {"name": "city", "type": "string"},
                      {"name": "zip", "type": "string"}
                    ]
                  }
                }
              ]
            }
            """;

    // --- Evolution schemas (backward-compatible) ---

    public static final String USER_V1 = """
            {
              "type": "record",
              "name": "User",
              "namespace": "com.example.evolution",
              "fields": [
                {"name": "id", "type": "long"},
                {"name": "name", "type": "string"}
              ]
            }
            """;

    public static final String USER_V2_ADD_FIELD = """
            {
              "type": "record",
              "name": "User",
              "namespace": "com.example.evolution",
              "fields": [
                {"name": "id", "type": "long"},
                {"name": "name", "type": "string"},
                {"name": "email", "type": "string", "default": "unknown@example.com"}
              ]
            }
            """;

    public static final String USER_V2_ADD_FIELD_NO_DEFAULT = """
            {
              "type": "record",
              "name": "User",
              "namespace": "com.example.evolution",
              "fields": [
                {"name": "id", "type": "long"},
                {"name": "name", "type": "string"},
                {"name": "email", "type": "string"}
              ]
            }
            """;

    public static final String USER_V3_ADD_ANOTHER_FIELD = """
            {
              "type": "record",
              "name": "User",
              "namespace": "com.example.evolution",
              "fields": [
                {"name": "id", "type": "long"},
                {"name": "name", "type": "string"},
                {"name": "email", "type": "string", "default": "unknown@example.com"},
                {"name": "age", "type": "int", "default": 0}
              ]
            }
            """;

    // --- Invalid schemas for error testing ---

    public static final String INVALID_SCHEMA = """
            {this is not valid json}
            """;

    public static final String MALFORMED_AVRO = """
            {
              "type": "record",
              "name": "BadSchema",
              "fields": "not_an_array"
            }
            """;
}
