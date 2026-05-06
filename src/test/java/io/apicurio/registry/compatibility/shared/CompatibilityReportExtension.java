package io.apicurio.registry.compatibility.shared;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.apicurio.registry.compatibility.collector.TestResultCollector;
import io.apicurio.registry.compatibility.report.HtmlReportGenerator;

/**
 * JUnit 5 extension that generates the HTML compatibility report after all tests complete.
 * Uses a shared {@link ExtensionContext.Store} to ensure the report is written only once
 * even though every test class registers this extension.
 */
public class CompatibilityReportExtension implements AfterAllCallback {

    private static final String REPORT_PATH = "target/compatibility-report.html";

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        ExtensionContext.Store rootStore = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        rootStore.getOrComputeIfAbsent("report-generated", k -> {
            generateReport();
            return "done";
        });
    }

    private void generateReport() {
        try {
            TestResultCollector collector = TestResultCollector.getInstance();
            HtmlReportGenerator generator = new HtmlReportGenerator(collector);
            generator.generate(Path.of(REPORT_PATH));
            System.out.println("Compatibility report written to " + REPORT_PATH);
        } catch (IOException e) {
            System.err.println("Failed to generate compatibility report: " + e.getMessage());
        }
    }
}
