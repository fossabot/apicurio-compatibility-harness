package io.apicurio.registry.compatibility.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.apicurio.registry.compatibility.model.TestOutcome;

/**
 * Enriches test outcomes with endpoint context information and embedded test source code.
 * Maps each (method, endpoint) pair to static metadata about the Confluent API operation,
 * and reads test class source files to extract individual test method bodies.
 */
public class ReportContextEnricher {

    private static final String CONFLUENT_DOCS_BASE =
            "https://docs.confluent.io/platform/current/schema-registry/develop/api.html";

    private static final String APICURIO_GITHUB_BASE =
            "https://github.com/Apicurio/apicurio-registry/blob/main/app/src/main/java/io/apicurio/registry/ccompat/rest/v8";

    /**
     * Static metadata for a single Confluent v8 API endpoint.
     */
    public record EndpointContext(
            String summary,
            String docAnchor,
            String implFile
    ) {}

    private static final Map<String, EndpointContext> ENDPOINT_CONTEXTS = new HashMap<>();

    static {
        // Subjects endpoints
        ENDPOINT_CONTEXTS.put("GET:/subjects", new EndpointContext(
                "List all registered subjects",
                "get--subjects",
                "SubjectsResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("GET:/subjects/{subject}/versions", new EndpointContext(
                "List versions registered under a subject",
                "get--subjects-subject-versions",
                "SubjectsResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("GET:/subjects/{subject}/versions/{version}", new EndpointContext(
                "Get a specific version of a schema under a subject",
                "get--subjects-subject-versions-version",
                "SubjectsResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("GET:/subjects/{subject}/versions/{version}/schema", new EndpointContext(
                "Get the raw schema string for a version",
                "get--subjects-subject-versions-version-schema",
                "SubjectsResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("POST:/subjects/{subject}/versions", new EndpointContext(
                "Register a new schema under a subject",
                "post--subjects-subject-versions",
                "SubjectsResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("POST:/subjects/{subject}/versions/{version}", new EndpointContext(
                "Look up schema version by content",
                "post--subjects-subject-versions-version",
                "SubjectsResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("DELETE:/subjects/{subject}", new EndpointContext(
                "Delete a subject and all its versions",
                "delete--subjects-subject",
                "SubjectsResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("DELETE:/subjects/{subject}/versions/{version}", new EndpointContext(
                "Delete a specific version of a subject",
                "delete--subjects-subject-versions-version",
                "SubjectsResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("DELETE:/subjects/{subject}/versions/{version}?permanent=true", new EndpointContext(
                "Delete a specific version of a subject",
                "delete--subjects-subject-versions-version",
                "SubjectsResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("GET:/subjects/{subject}/versions/{version}/referencedby", new EndpointContext(
                "Find schemas that reference the given version",
                "get--subjects-subject-versions-version-referencedby",
                "SubjectsResourceImpl.java"));

        // Schemas endpoints
        ENDPOINT_CONTEXTS.put("GET:/schemas/ids/{id}", new EndpointContext(
                "Get schema by global ID",
                "get--schemas-ids-id",
                "SchemasResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("GET:/schemas/ids/{id}/versions", new EndpointContext(
                "Get all versions for a schema global ID",
                "get--schemas-ids-id-versions",
                "SchemasResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("GET:/schemas/ids/{id}/subjects", new EndpointContext(
                "Get all subjects referencing a schema global ID",
                "get--schemas-ids-id-subjects",
                "SchemasResourceImpl.java"));

        // Compatibility endpoint
        ENDPOINT_CONTEXTS.put("POST:/compatibility/subjects/{subject}/versions/{version}", new EndpointContext(
                "Test schema compatibility against a specific version",
                "post--compatibility-subjects-subject-versions-version",
                "CompatibilityResourceImpl.java"));

        // Config endpoints
        ENDPOINT_CONTEXTS.put("GET:/config", new EndpointContext(
                "Get global compatibility configuration",
                "get--config",
                "ConfigResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("PUT:/config", new EndpointContext(
                "Update global compatibility configuration",
                "put--config",
                "ConfigResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("GET:/config/{subject}", new EndpointContext(
                "Get compatibility configuration for a subject",
                "get--config-subject",
                "ConfigResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("PUT:/config/{subject}", new EndpointContext(
                "Update compatibility configuration for a subject",
                "put--config-subject",
                "ConfigResourceImpl.java"));

        // Mode endpoints
        ENDPOINT_CONTEXTS.put("GET:/mode", new EndpointContext(
                "Get the registry mode",
                "get--mode",
                "ModeResourceImpl.java"));

        ENDPOINT_CONTEXTS.put("PUT:/mode", new EndpointContext(
                "Update the registry mode",
                "put--mode",
                "ModeResourceImpl.java"));
    }

    /**
     * Regex that matches a method definition preceded by optional annotations.
     * Group 1 captures the annotations + signature, group 2 captures the body content.
     */
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "((?:@(?:Test|DisplayName|ParameterizedTest|MethodSource|Tag|Disabled|Order|RepeatedTest|Nested)[^\n]*\\n\\s*)*)"
            + "(?:public|private|protected|package)?\\s+"
            + "(?:static\\s+)?"
            + "(?:\\w+(?:<[^>]+>)?\\s+)?"
            + "(\\w+)\\s*\\([^)]*\\)\\s*(?:throws\\s+[\\w\\s,.]+)?\\s*\\{",
            Pattern.MULTILINE);

    /** Cache of class name -> source file content. */
    private final Map<String, String> sourceCache = new HashMap<>();

    /**
     * Enriches a list of test outcomes in-place by setting context fields:
     * openApiOperation, confluentDocUrl, apicurioImplHint, and testSourceCode.
     *
     * @param outcomes the mutable list of test outcomes to enrich
     */
    public void enrich(List<TestOutcome> outcomes) {
        for (int i = 0; i < outcomes.size(); i++) {
            TestOutcome outcome = outcomes.get(i);

            String openApiOperation = "";
            String confluentDocUrl = "";
            String apicurioImplHint = "";

            String key = outcome.getMethod() + ":" + outcome.getEndpoint();
            EndpointContext ctx = ENDPOINT_CONTEXTS.get(key);
            if (ctx != null) {
                openApiOperation = ctx.summary();
                confluentDocUrl = CONFLUENT_DOCS_BASE + "#" + ctx.docAnchor();
                apicurioImplHint = APICURIO_GITHUB_BASE + "/" + ctx.implFile();
            }

            String testSourceCode = "";
            String className = outcome.getTestClassName();
            String methodName = outcome.getTestMethodName();
            if (className != null && !className.isEmpty()
                    && methodName != null && !methodName.isEmpty()) {
                String classSource = sourceCache.computeIfAbsent(className, this::readTestClassSource);
                if (classSource != null) {
                    testSourceCode = extractMethodBody(classSource, methodName);
                }
            }

            TestOutcome enriched = TestOutcome.builder()
                    .testName(outcome.getTestName())
                    .endpoint(outcome.getEndpoint())
                    .method(outcome.getMethod())
                    .result(outcome.getResult())
                    .confluentStatus(outcome.getConfluentStatus())
                    .apicurioStatus(outcome.getApicurioStatus())
                    .details(outcome.getDetails())
                    .confluentBody(outcome.getConfluentBody())
                    .apicurioBody(outcome.getApicurioBody())
                    .testClassName(outcome.getTestClassName())
                    .testMethodName(outcome.getTestMethodName())
                    .testSourceCode(testSourceCode)
                    .openApiOperation(openApiOperation)
                    .confluentDocUrl(confluentDocUrl)
                    .apicurioImplHint(apicurioImplHint)
                    .build();

            outcomes.set(i, enriched);
        }
    }

    /**
     * Reads the source file for a test class by converting the fully-qualified
     * class name to a file path under {@code src/test/java/}.
     *
     * @param className fully-qualified class name (e.g. {@code com.example.MyTest})
     * @return the file content, or {@code null} if the file cannot be read
     */
    String readTestClassSource(String className) {
        String relativePath = className.replace('.', '/') + ".java";
        Path sourcePath = Path.of("src/test/java", relativePath);
        try {
            return Files.readString(sourcePath);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Extracts the full text of a named method from a Java class source string.
     * Includes any preceding test-related annotations (e.g. @Test, @DisplayName).
     * Uses brace counting from the opening brace to find the method boundary.
     *
     * @param classSource the full source text of the class
     * @param methodName  the simple name of the method to extract
     * @return the method text (annotations + signature + body), or empty string if not found
     */
    String extractMethodBody(String classSource, String methodName) {
        Matcher matcher = METHOD_PATTERN.matcher(classSource);
        while (matcher.find()) {
            String fullMatch = matcher.group(0);
            // Check if this method declaration matches the target method name
            // The method name appears right before the opening parenthesis
            if (fullMatch.contains(methodName + "(") || fullMatch.contains(methodName + " (")) {
                // Find the opening brace position in the original source
                int matchEnd = matcher.end();
                int openBraceIndex = classSource.lastIndexOf('{', matchEnd);
                if (openBraceIndex < 0) {
                    continue;
                }

                // Find the start of annotations (if any)
                int methodStart = matcher.start(1);
                if (methodStart < 0) {
                    methodStart = matcher.start();
                }

                // Count braces to find the closing brace
                int depth = 1;
                int pos = openBraceIndex + 1;
                while (pos < classSource.length() && depth > 0) {
                    char c = classSource.charAt(pos);
                    if (c == '{') {
                        depth++;
                    } else if (c == '}') {
                        depth--;
                    }
                    // Skip string literals to avoid counting braces inside strings
                    if (c == '"') {
                        pos++;
                        while (pos < classSource.length()) {
                            char sc = classSource.charAt(pos);
                            if (sc == '\\') {
                                pos++; // skip escaped char
                            } else if (sc == '"') {
                                break;
                            }
                            pos++;
                        }
                    }
                    pos++;
                }

                if (depth == 0) {
                    return classSource.substring(methodStart, pos).trim();
                }
            }
        }
        return "";
    }
}
