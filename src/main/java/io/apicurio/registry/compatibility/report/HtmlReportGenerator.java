package io.apicurio.registry.compatibility.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import io.apicurio.registry.compatibility.collector.TestResultCollector;
import io.apicurio.registry.compatibility.model.TestOutcome;

public class HtmlReportGenerator {

    private final TestResultCollector collector;

    public HtmlReportGenerator(TestResultCollector collector) {
        this.collector = collector;
    }

    public void generate(Path outputPath) throws IOException {
        List<TestOutcome> outcomes = collector.getOutcomes();
        String html = buildHtml(outcomes);
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, html);
    }

    private String buildHtml(List<TestOutcome> outcomes) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("    <meta charset=\"UTF-8\">\n");
        sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("    <title>Apicurio Registry - Confluent v8 API Compatibility Report</title>\n");
        sb.append("    <style>\n");
        sb.append("        * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        sb.append("        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n");
        sb.append("               background: #f5f5f5; color: #333; padding: 2rem; }\n");
        sb.append("        h1 { font-size: 1.5rem; margin-bottom: 0.5rem; }\n");
        sb.append("        h2 { font-size: 1.2rem; margin: 1.5rem 0 0.75rem; border-bottom: 2px solid #ddd;\n");
        sb.append("             padding-bottom: 0.25rem; }\n");
        sb.append("        .meta { color: #666; font-size: 0.85rem; margin-bottom: 1.5rem; }\n");
        sb.append("        .summary { display: grid; grid-template-columns: repeat(5, 1fr); gap: 1rem;\n");
        sb.append("                   margin-bottom: 2rem; }\n");
        sb.append("        .summary-card { background: white; border-radius: 8px; padding: 1rem;\n");
        sb.append("                        text-align: center; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n");
        sb.append("        .summary-card .number { font-size: 2rem; font-weight: 700; }\n");
        sb.append("        .summary-card .label { font-size: 0.8rem; color: #666; text-transform: uppercase; }\n");
        sb.append("        .pass { color: #2e7d32; }\n");
        sb.append("        .fail { color: #c62828; }\n");
        sb.append("        .skipped { color: #f57f17; }\n");
        sb.append("        .known { color: #1565c0; }\n");
        sb.append("        .error { color: #6a1b9a; }\n");
        sb.append("        table { width: 100%; border-collapse: collapse; background: white;\n");
        sb.append("                border-radius: 8px; overflow: hidden;\n");
        sb.append("                box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n");
        sb.append("        th { background: #37474f; color: white; text-align: left;\n");
        sb.append("             padding: 0.75rem 1rem; font-size: 0.85rem; text-transform: uppercase; }\n");
        sb.append("        td { padding: 0.6rem 1rem; border-bottom: 1px solid #eee; font-size: 0.9rem; }\n");
        sb.append("        tr:hover { background: #fafafa; }\n");
        sb.append("        .badge { display: inline-block; padding: 0.15rem 0.5rem; border-radius: 4px;\n");
        sb.append("                 font-size: 0.75rem; font-weight: 600; color: white; }\n");
        sb.append("        .badge-PASS { background: #4caf50; }\n");
        sb.append("        .badge-FAIL { background: #f44336; }\n");
        sb.append("        .badge-SKIPPED_GAP { background: #ff9800; }\n");
        sb.append("        .badge-KNOWN_INCOMPATIBILITY { background: #2196f3; }\n");
        sb.append("        .badge-ERROR { background: #9c27b0; }\n");
        sb.append("        .details { max-width: 400px; overflow: hidden; text-overflow: ellipsis;\n");
        sb.append("                   white-space: nowrap; font-family: monospace; font-size: 0.8rem;\n");
        sb.append("                   color: #666; }\n");
        sb.append("    </style>\n</head>\n<body>\n");

        sb.append("    <h1>Apicurio Registry - Confluent v8 API Compatibility Report</h1>\n");
        sb.append("    <p class=\"meta\">Generated: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .append("</p>\n");

        sb.append("    <div class=\"summary\">\n");
        appendSummaryCard(sb, String.valueOf(collector.getTotalCount()), "Total", "");
        appendSummaryCard(sb, String.valueOf(collector.getPassCount()), "Pass", "pass");
        appendSummaryCard(sb, String.valueOf(collector.getFailCount()), "Fail", "fail");
        appendSummaryCard(sb, String.valueOf(collector.getSkippedGapCount()), "Skipped (Gap)", "skipped");
        appendSummaryCard(sb, String.valueOf(collector.getKnownIncompatibilityCount()), "Known Incompat.", "known");
        sb.append("    </div>\n");

        sb.append("    <h2>Detailed Results</h2>\n");
        sb.append("    <table>\n");
        sb.append("        <tr><th>Test</th><th>Method</th><th>Endpoint</th><th>Confluent</th>")
                .append("<th>Apicurio</th><th>Result</th><th>Details</th></tr>\n");

        for (TestOutcome outcome : outcomes) {
            sb.append("        <tr><td>").append(escapeHtml(outcome.getTestName()))
                    .append("</td><td>").append(outcome.getMethod())
                    .append("</td><td>").append(escapeHtml(outcome.getEndpoint()))
                    .append("</td><td>").append(outcome.getConfluentStatus())
                    .append("</td><td>").append(outcome.getApicurioStatus())
                    .append("</td><td><span class=\"badge badge-").append(outcome.getResult().name())
                    .append("\">").append(outcome.getResult().name()).append("</span>")
                    .append("</td><td class=\"details\">").append(escapeHtml(outcome.getDetails()))
                    .append("</td></tr>\n");
        }

        sb.append("    </table>\n</body>\n</html>");

        return sb.toString();
    }

    private void appendSummaryCard(StringBuilder sb, String number, String label, String cssClass) {
        sb.append("        <div class=\"summary-card\"><div class=\"number ");
        if (!cssClass.isEmpty()) {
            sb.append(cssClass);
        }
        sb.append("\">").append(number).append("</div><div class=\"label\">")
                .append(label).append("</div></div>\n");
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
