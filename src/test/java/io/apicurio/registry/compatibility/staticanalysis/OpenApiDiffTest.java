package io.apicurio.registry.compatibility.staticanalysis;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import io.apicurio.registry.compatibility.collector.TestResultCollector;
import io.apicurio.registry.compatibility.model.CompatibilityTestResult;
import io.apicurio.registry.compatibility.model.TestOutcome;
import io.apicurio.registry.compatibility.shared.CompatibilityReportExtension;

@DisplayName("Static OpenAPI Spec Diff")
@ExtendWith(CompatibilityReportExtension.class)
class OpenApiDiffTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Compares Confluent v8 spec against Apicurio ccompat v8 spec")
    void openApiSpecDiff() throws IOException {
        OpenApiSpecProvider provider = new OpenApiSpecProvider();
        Path confluentSpec = provider.writeConfluentSpecToTemp();

        // Resolve Apicurio spec: system property URL > classpath resource > live fetch
        Path apicurioSpec = resolveApicurioSpec();

        if (apicurioSpec == null) {
            recordSkippedOutcome();
            return;
        }

        OpenApiDiffAnalyzer analyzer = new OpenApiDiffAnalyzer(
                confluentSpec.toString(), apicurioSpec.toString());
        List<TestOutcome> outcomes = analyzer.analyze();

        TestResultCollector collector = TestResultCollector.getInstance();
        for (TestOutcome outcome : outcomes) {
            collector.record(outcome);
        }

        System.out.println("[static-diff] Recorded " + outcomes.size()
                + " spec differences into test results.");
    }

    private Path resolveApicurioSpec() {
        // 1. System property override
        String url = System.getProperty("static.spec.apicurio.url");
        if (url != null && !url.isBlank()) {
            return fetchSpecToTemp(url, "apicurio-override");
        }

        // 2. Classpath resource (bundled fallback)
        try {
            return new OpenApiSpecProvider().writeApicurioSpecToTemp();
        } catch (RuntimeException e) {
            // No bundled spec — try live
        }

        // 3. Live fetch from running Apicurio container
        return fetchSpecToTemp("http://localhost:8082/apis/ccompat/v8/openapi", "apicurio-live");
    }

    private Path fetchSpecToTemp(String url, String label) {
        try {
            var response = given().get(url);
            if (response.statusCode() != 200) {
                return null;
            }
            Path file = tempDir.resolve(label + ".json");
            Files.writeString(file, response.body().asString());
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    private void recordSkippedOutcome() {
        TestResultCollector.getInstance().record(TestOutcome.builder()
                .testName("static-diff: OpenAPI spec comparison")
                .method("DIFF")
                .endpoint("openapi-spec")
                .confluentStatus("LOADED")
                .apicurioStatus("UNAVAILABLE")
                .result(CompatibilityTestResult.SKIPPED_GAP)
                .details("Apicurio OpenAPI spec not available. "
                        + "Run with containers up or provide -Dstatic.spec.apicurio.url=<url>.")
                .build());
        System.out.println("[static-diff] Skipped — no Apicurio spec available.");
    }
}
