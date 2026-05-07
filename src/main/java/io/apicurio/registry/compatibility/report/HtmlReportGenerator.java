package io.apicurio.registry.compatibility.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.apicurio.registry.compatibility.collector.TestResultCollector;
import io.apicurio.registry.compatibility.model.TestOutcome;

public class HtmlReportGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CONFLUENT_VERSION =
            System.getProperty("confluent.registry.version", "7.8.0");

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

    // ---------------------------------------------------------------
    // HTML generation
    // ---------------------------------------------------------------

    private String buildHtml(List<TestOutcome> outcomes) {
        StringBuilder sb = new StringBuilder(outcomes.size() * 4096);

        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("    <meta charset=\"UTF-8\">\n");
        sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("    <title>Apicurio Registry - Confluent v8 API Compatibility Report</title>\n");
        appendCss(sb);
        sb.append("</head>\n<body>\n");

        // Header
        sb.append("    <h1>Apicurio Registry - Confluent v8 API Compatibility Report</h1>\n");
        sb.append("    <p class=\"meta\">Generated: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .append(" &middot; Confluent Schema Registry: v")
                .append(escapeHtml(CONFLUENT_VERSION))
                .append("</p>\n");

        // Summary cards
        appendSummarySection(sb);

        // Toolbar
        appendToolbar(sb);

        // Results table
        appendTable(sb, outcomes);

        // TEST_DATA array
        appendTestData(sb, outcomes);

        // Drawer markup
        appendDrawerMarkup(sb);

        // JavaScript
        appendJavaScript(sb);

        sb.append("</body>\n</html>");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // CSS
    // ---------------------------------------------------------------

    private void appendCss(StringBuilder sb) {
        sb.append("    <style>\n");
        // Reset & body
        sb.append("        * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        sb.append("        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n");
        sb.append("               background: #f5f5f5; color: #333; padding: 2rem; }\n");
        sb.append("        h1 { font-size: 1.5rem; margin-bottom: 0.5rem; }\n");
        sb.append("        .meta { color: #666; font-size: 0.85rem; margin-bottom: 1rem; }\n");

        // Toolbar
        sb.append("        .toolbar { display: flex; gap: 0.75rem; align-items: center; margin-bottom: 1rem; }\n");
        sb.append("        .toolbar button { padding: 0.4rem 0.8rem; border: 1px solid #ccc; border-radius: 4px;\n");
        sb.append("                          background: white; cursor: pointer; font-size: 0.85rem; }\n");
        sb.append("        .toolbar button:hover { background: #e8e8e8; }\n");
        sb.append("        .toolbar button.active { background: #37474f; color: white; border-color: #37474f; }\n");

        // Summary cards
        sb.append("        .summary { display: grid; grid-template-columns: repeat(6, 1fr); gap: 1rem;\n");
        sb.append("                   margin-bottom: 2rem; }\n");
        sb.append("        .summary-card { background: white; border-radius: 8px; padding: 1rem;\n");
        sb.append("                        text-align: center; box-shadow: 0 1px 3px rgba(0,0,0,0.1);\n");
        sb.append("                        cursor: pointer; transition: box-shadow 0.2s; }\n");
        sb.append("        .summary-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.15); }\n");
        sb.append("        .summary-card .number { font-size: 2rem; font-weight: 700; }\n");
        sb.append("        .summary-card .label { font-size: 0.8rem; color: #666; text-transform: uppercase; }\n");
        sb.append("        .pass { color: #2e7d32; }\n");
        sb.append("        .fail { color: #c62828; }\n");
        sb.append("        .skipped { color: #f57f17; }\n");
        sb.append("        .known { color: #1565c0; }\n");
        sb.append("        .error { color: #6a1b9a; }\n");

        // Table
        sb.append("        table { width: 100%; border-collapse: collapse; background: white;\n");
        sb.append("                border-radius: 8px; overflow: hidden;\n");
        sb.append("                box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n");
        sb.append("        th { background: #37474f; color: white; text-align: left;\n");
        sb.append("             padding: 0.75rem 1rem; font-size: 0.85rem; text-transform: uppercase;\n");
        sb.append("             position: sticky; top: 0; z-index: 1; cursor: pointer;\n");
        sb.append("             user-select: none; }\n");
        sb.append("        th:hover { background: #455a64; }\n");
        sb.append("        th .sort-arrow { font-size: 0.7rem; margin-left: 0.3rem; opacity: 0.4; }\n");
        sb.append("        th.sorted-asc .sort-arrow, th.sorted-desc .sort-arrow { opacity: 1; }\n");
        sb.append("        td { padding: 0.6rem 1rem; border-bottom: 1px solid #eee; font-size: 0.9rem; }\n");
        sb.append("        tr.main-row { cursor: pointer; transition: background 0.15s; }\n");
        sb.append("        tr.main-row:hover { background: #e3f2fd; }\n");
        sb.append("        .badge { display: inline-block; padding: 0.15rem 0.5rem; border-radius: 4px;\n");
        sb.append("                 font-size: 0.75rem; font-weight: 600; color: white; }\n");
        sb.append("        .badge-PASS { background: #4caf50; }\n");
        sb.append("        .badge-FAIL { background: #f44336; }\n");
        sb.append("        .badge-SKIPPED_GAP { background: #ff9800; }\n");
        sb.append("        .badge-KNOWN_INCOMPATIBILITY { background: #2196f3; }\n");
        sb.append("        .badge-ERROR { background: #9c27b0; }\n");
        sb.append("        .details-summary { max-width: 400px; overflow: hidden; text-overflow: ellipsis;\n");
        sb.append("                   white-space: nowrap; font-family: monospace; font-size: 0.8rem;\n");
        sb.append("                   color: #666; }\n");

        // Triage dot
        sb.append("        .triage-dot { display: inline-block; width: 8px; height: 8px;\n");
        sb.append("                      border-radius: 50%; vertical-align: middle;\n");
        sb.append("                      background: #bdbdbd; margin-right: 4px; }\n");
        sb.append("        .triage-dot[data-status=\"Investigating\"] { background: #ff9800; }\n");
        sb.append("        .triage-dot[data-status=\"Confirmed Bug\"] { background: #f44336; }\n");
        sb.append("        .triage-dot[data-status=\"Known Gap\"] { background: #2196f3; }\n");
        sb.append("        .triage-dot[data-status=\"Won't Fix\"] { background: #9e9e9e; }\n");

        // Drawer overlay
        sb.append("        .drawer-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%;\n");
        sb.append("                          background: rgba(0,0,0,0.4); z-index: 1000;\n");
        sb.append("                          display: none; }\n");
        sb.append("        .drawer-overlay.visible { display: block; }\n");

        // Drawer
        sb.append("        .drawer { position: fixed; top: 0; right: 0; height: 100vh; width: 60%;\n");
        sb.append("                  z-index: 1001; background: white;\n");
        sb.append("                  box-shadow: -4px 0 20px rgba(0,0,0,0.15);\n");
        sb.append("                  display: flex; flex-direction: column;\n");
        sb.append("                  transform: translateX(100%); transition: transform 0.3s ease; }\n");
        sb.append("        .drawer.open { transform: translateX(0); }\n");

        // Drawer header
        sb.append("        .drawer-header { display: flex; justify-content: space-between;\n");
        sb.append("                         align-items: center; padding: 1rem 1.25rem;\n");
        sb.append("                         border-bottom: 1px solid #e0e0e0; }\n");
        sb.append("        .drawer-header h2 { font-size: 1rem; font-weight: 600;\n");
        sb.append("                            overflow: hidden; text-overflow: ellipsis;\n");
        sb.append("                            white-space: nowrap; flex: 1; margin-right: 1rem; }\n");
        sb.append("        .drawer-close { background: none; border: none; font-size: 1.5rem;\n");
        sb.append("                        cursor: pointer; color: #666; padding: 0 0.25rem;\n");
        sb.append("                        line-height: 1; }\n");
        sb.append("        .drawer-close:hover { color: #333; }\n");

        // Drawer details subtitle
        sb.append("        .drawer-details { padding: 0.5rem 1.25rem; background: #fafafa;\n");
        sb.append("                          font-size: 0.85rem; color: #555;\n");
        sb.append("                          border-bottom: 1px solid #e0e0e0;\n");
        sb.append("                          font-family: 'SF Mono', 'Consolas', monospace;\n");
        sb.append("                          word-break: break-word; }\n");
        sb.append("        .drawer-details.fail { color: #c62828; background: #fbe9e7; }\n");

        // Drawer tabs
        sb.append("        .drawer-tabs { display: flex; border-bottom: 1px solid #e0e0e0;\n");
        sb.append("                       padding: 0 1rem; }\n");
        sb.append("        .tab-btn { padding: 0.6rem 1rem; border: none; background: none;\n");
        sb.append("                   cursor: pointer; font-size: 0.85rem; color: #666;\n");
        sb.append("                   border-bottom: 2px solid transparent;\n");
        sb.append("                   transition: color 0.2s, border-color 0.2s; }\n");
        sb.append("        .tab-btn:hover { color: #333; }\n");
        sb.append("        .tab-btn.active { color: #1565c0; border-bottom-color: #1565c0;\n");
        sb.append("                          font-weight: 600; }\n");

        // Drawer body
        sb.append("        .drawer-body { flex: 1; overflow-y: auto; padding: 1.25rem;\n");
        sb.append("                       padding-bottom: 5rem; }\n");
        sb.append("        .tab-panel { display: none; }\n");
        sb.append("        .tab-panel.active { display: block; }\n");

        // Response panels (side by side)
        sb.append("        .response-panels { display: grid; grid-template-columns: 1fr 1fr;\n");
        sb.append("                           gap: 1rem; }\n");
        sb.append("        .response-panel { border: 1px solid #e0e0e0; border-radius: 6px;\n");
        sb.append("                          overflow: hidden; }\n");
        sb.append("        .response-panel .panel-header { background: #eceff1;\n");
        sb.append("                          padding: 0.4rem 0.75rem; font-size: 0.8rem;\n");
        sb.append("                          font-weight: 600; color: #455a64; }\n");
        sb.append("        .response-panel pre { padding: 0.75rem; font-size: 0.8rem;\n");
        sb.append("                          white-space: pre-wrap; word-break: break-all;\n");
        sb.append("                          max-height: 400px; overflow-y: auto; margin: 0;\n");
        sb.append("                          font-family: 'SF Mono', 'Consolas', monospace; }\n");

        // Code block (Test Code tab)
        sb.append("        .code-class-header { font-size: 0.85rem; color: #455a64;\n");
        sb.append("                              margin-bottom: 0.5rem; font-weight: 600; }\n");
        sb.append("        pre.code-block { padding: 1rem; background: #263238; color: #eeffff;\n");
        sb.append("                         font-family: 'SF Mono', 'Consolas', monospace;\n");
        sb.append("                         font-size: 0.8rem; white-space: pre-wrap;\n");
        sb.append("                         word-break: break-all; border-radius: 6px;\n");
        sb.append("                         max-height: 500px; overflow-y: auto; line-height: 1.5; }\n");

        // Syntax highlighting
        sb.append("        .hl-keyword { color: #7c4dff; }\n");
        sb.append("        .hl-string { color: #2e7d32; }\n");
        sb.append("        .hl-comment { color: #9e9e9e; font-style: italic; }\n");
        sb.append("        .hl-annotation { color: #c62828; }\n");

        // API Spec tab
        sb.append("        .api-spec-header { font-size: 1.1rem; font-weight: 600;\n");
        sb.append("                           margin-bottom: 0.75rem; }\n");

        // Doc sections (API Spec & Impl tabs)
        sb.append("        .doc-section { margin-bottom: 1rem; }\n");
        sb.append("        .doc-section h3 { font-size: 0.95rem; margin-bottom: 0.5rem;\n");
        sb.append("                           color: #37474f; }\n");
        sb.append("        .doc-section p { color: #555; font-size: 0.9rem; line-height: 1.5;\n");
        sb.append("                          margin-bottom: 0.5rem; }\n");
        sb.append("        .doc-section a { color: #1565c0; text-decoration: none;\n");
        sb.append("                          font-weight: 500; }\n");
        sb.append("        .doc-section a:hover { text-decoration: underline; }\n");
        sb.append("        .doc-section pre { background: #f5f5f5; padding: 0.75rem;\n");
        sb.append("                           border-radius: 4px; font-size: 0.8rem;\n");
        sb.append("                           white-space: pre-wrap; margin-top: 0.5rem; }\n");

        // Request tab
        sb.append("        .request-field { margin-bottom: 0.75rem; }\n");
        sb.append("        .request-label { font-weight: 600; color: #37474f; }\n");
        sb.append("        .request-payload { background: #f5f5f5; padding: 0.75rem; border-radius: 4px;\n");
        sb.append("                           font-size: 0.8rem; white-space: pre-wrap; margin-top: 0.5rem;\n");
        sb.append("                           font-family: 'SF Mono', 'Consolas', monospace; }\n");
        sb.append("        .request-payload.empty { color: #999; font-style: italic; }\n");

        // Triage bar
        sb.append("        .triage-bar { position: absolute; bottom: 0; left: 0; right: 0;\n");
        sb.append("                      display: flex; gap: 0.75rem; align-items: center;\n");
        sb.append("                      padding: 0.75rem 1.25rem; border-top: 1px solid #e0e0e0;\n");
        sb.append("                      background: white; }\n");
        sb.append("        .triage-bar label { font-size: 0.8rem; color: #666;\n");
        sb.append("                            font-weight: 600; }\n");
        sb.append("        .triage-bar select, .triage-bar input[type=\"text\"] {\n");
        sb.append("            padding: 0.3rem 0.5rem; border: 1px solid #ccc;\n");
        sb.append("            border-radius: 4px; font-size: 0.8rem; }\n");
        sb.append("        .triage-bar select { min-width: 130px; }\n");
        sb.append("        .triage-bar input[type=\"text\"] { flex: 1; min-width: 100px; }\n");

        sb.append("    </style>\n");
    }

    // ---------------------------------------------------------------
    // Summary section
    // ---------------------------------------------------------------

    private void appendSummarySection(StringBuilder sb) {
        sb.append("    <div class=\"summary\">\n");
        appendSummaryCard(sb, String.valueOf(collector.getTotalCount()), "Total", "", null);
        appendSummaryCard(sb, String.valueOf(collector.getPassCount()), "Pass", "pass", "PASS");
        appendSummaryCard(sb, String.valueOf(collector.getFailCount()), "Fail", "fail", "FAIL");
        appendSummaryCard(sb, String.valueOf(collector.getSkippedGapCount()), "Skipped", "skipped", "SKIPPED_GAP");
        appendSummaryCard(sb, String.valueOf(collector.getKnownIncompatibilityCount()), "Known Incompat.", "known", "KNOWN_INCOMPATIBILITY");
        appendSummaryCard(sb, String.valueOf(collector.getErrorCount()), "Error", "error", "ERROR");
        sb.append("    </div>\n");
    }

    private void appendSummaryCard(StringBuilder sb, String number, String label, String cssClass,
            String filterResult) {
        String onclick = filterResult != null
                ? " onclick=\"filterResults('" + filterResult + "')\""
                : " onclick=\"filterResults(null)\"";
        sb.append("        <div class=\"summary-card\"").append(onclick).append(">");
        sb.append("<div class=\"number ");
        if (!cssClass.isEmpty()) {
            sb.append(cssClass);
        }
        sb.append("\">").append(number).append("</div><div class=\"label\">")
                .append(label).append("</div></div>\n");
    }

    // ---------------------------------------------------------------
    // Toolbar
    // ---------------------------------------------------------------

    private void appendToolbar(StringBuilder sb) {
        sb.append("    <div class=\"toolbar\">\n");
        sb.append("        <button onclick=\"openAllDrawers()\">Open First</button>\n");
        sb.append("        <button onclick=\"closeDrawer()\">Close Drawer</button>\n");
        sb.append("        <button onclick=\"filterResults(null)\" class=\"active\" id=\"filter-all\">All</button>\n");
        sb.append("        <button onclick=\"filterResults('PASS')\" id=\"filter-PASS\">Pass</button>\n");
        sb.append("        <button onclick=\"filterResults('FAIL')\" id=\"filter-FAIL\">Fail</button>\n");
        sb.append("    </div>\n");
    }

    // ---------------------------------------------------------------
    // Results table
    // ---------------------------------------------------------------

    private void appendTable(StringBuilder sb, List<TestOutcome> outcomes) {
        sb.append("    <table id=\"results-table\">\n");
        sb.append("        <thead><tr>")
                .append("<th style=\"width:24px\"></th>")  // triage dot
                .append("<th onclick=\"sortTable(1)\">Test <span class=\"sort-arrow\">▲</span></th>")
                .append("<th onclick=\"sortTable(2)\">Method <span class=\"sort-arrow\">▲</span></th>")
                .append("<th onclick=\"sortTable(3)\">Endpoint <span class=\"sort-arrow\">▲</span></th>")
                .append("<th onclick=\"sortTable(4)\">Confluent <span class=\"sort-arrow\">▲</span></th>")
                .append("<th onclick=\"sortTable(5)\">Apicurio <span class=\"sort-arrow\">▲</span></th>")
                .append("<th onclick=\"sortTable(6)\">Result <span class=\"sort-arrow\">▲</span></th>")
                .append("<th onclick=\"sortTable(7)\">Details <span class=\"sort-arrow\">▲</span></th>")
                .append("</tr></thead>\n");
        sb.append("        <tbody>\n");

        int idx = 0;
        for (TestOutcome o : outcomes) {
            sb.append("        <tr class=\"main-row\" data-idx=\"").append(idx)
                    .append("\" data-result=\"").append(o.getResult().name())
                    .append("\" onclick=\"openDrawer(").append(idx).append(")\">");

            // Triage dot column
            sb.append("<td><span class=\"triage-dot\" id=\"dot-").append(idx)
                    .append("\" data-status=\"\"></span></td>");

            sb.append("<td>").append(escapeHtml(o.getTestName()))
                    .append("</td><td>").append(escapeHtml(o.getMethod()))
                    .append("</td><td>").append(escapeHtml(o.getEndpoint()))
                    .append("</td><td>").append(escapeHtml(o.getConfluentStatus()))
                    .append("</td><td>").append(escapeHtml(o.getApicurioStatus()))
                    .append("</td><td><span class=\"badge badge-").append(o.getResult().name())
                    .append("\">").append(o.getResult().name()).append("</span>")
                    .append("</td><td class=\"details-summary\">").append(escapeHtml(o.getDetails()))
                    .append("</td></tr>\n");
            idx++;
        }

        sb.append("        </tbody>\n    </table>\n");
    }

    // ---------------------------------------------------------------
    // TEST_DATA JavaScript array
    // ---------------------------------------------------------------

    private void appendTestData(StringBuilder sb, List<TestOutcome> outcomes) {
        sb.append("    <script>\n");
        sb.append("    var TEST_DATA = [\n");

        for (int i = 0; i < outcomes.size(); i++) {
            TestOutcome o = outcomes.get(i);
            if (i > 0) {
                sb.append(",\n");
            }
            sb.append("      {");
            sb.append("testName:\"").append(escapeJs(o.getTestName())).append("\", ");
            sb.append("endpoint:\"").append(escapeJs(o.getEndpoint())).append("\", ");
            sb.append("method:\"").append(escapeJs(o.getMethod())).append("\", ");
            sb.append("requestPayload:").append(toJsString(prettifyJson(o.getRequestPayload()))).append(", ");
            sb.append("result:\"").append(escapeJs(o.getResult().name())).append("\", ");
            sb.append("confluentStatus:\"").append(escapeJs(o.getConfluentStatus())).append("\", ");
            sb.append("apicurioStatus:\"").append(escapeJs(o.getApicurioStatus())).append("\", ");
            sb.append("details:\"").append(escapeJs(o.getDetails())).append("\", ");
            sb.append("confluentBody:").append(toJsString(prettifyJson(o.getConfluentBody()))).append(", ");
            sb.append("apicurioBody:").append(toJsString(prettifyJson(o.getApicurioBody()))).append(", ");
            sb.append("testClassName:\"").append(escapeJs(o.getTestClassName())).append("\", ");
            sb.append("testMethodName:\"").append(escapeJs(o.getTestMethodName())).append("\", ");
            sb.append("testSourceCode:").append(toJsString(o.getTestSourceCode())).append(", ");
            sb.append("openApiOperation:").append(toJsString(o.getOpenApiOperation())).append(", ");
            sb.append("confluentDocUrl:\"").append(escapeJs(o.getConfluentDocUrl())).append("\", ");
            sb.append("apicurioImplHint:\"").append(escapeJs(o.getApicurioImplHint())).append("\"");
            sb.append("}");
        }

        sb.append("\n    ];\n");
        sb.append("    </script>\n");
    }

    /**
     * Wraps a string value in double-quotes for use as a JS string literal,
     * escaping all special characters. Returns the quoted string (or "\"\"" for null).
     */
    private String toJsString(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + escapeJs(value) + "\"";
    }

    // ---------------------------------------------------------------
    // Drawer markup (static HTML shell - content populated by JS)
    // ---------------------------------------------------------------

    private void appendDrawerMarkup(StringBuilder sb) {
        // Overlay
        sb.append("    <div class=\"drawer-overlay\" id=\"drawerOverlay\" onclick=\"closeDrawer()\"></div>\n");

        // Drawer
        sb.append("    <div class=\"drawer\" id=\"drawer\">\n");

        // Header
        sb.append("        <div class=\"drawer-header\">\n");
        sb.append("            <h2 id=\"drawerTitle\">Test Details</h2>\n");
        sb.append("            <button class=\"drawer-close\" onclick=\"closeDrawer()\">&times;</button>\n");
        sb.append("        </div>\n");

        // Details subtitle (visible across all tabs)
        sb.append("        <div class=\"drawer-details\" id=\"drawerDetails\"></div>\n");

        // Tabs
        sb.append("        <div class=\"drawer-tabs\">\n");
        sb.append("            <button class=\"tab-btn active\" onclick=\"switchTab('responses')\">Responses</button>\n");
        sb.append("            <button class=\"tab-btn\" onclick=\"switchTab('request')\">Request</button>\n");
        sb.append("            <button class=\"tab-btn\" onclick=\"switchTab('testcode')\">Source</button>\n");
        sb.append("            <button class=\"tab-btn\" onclick=\"switchTab('apispec')\">API Spec</button>\n");
        sb.append("            <button class=\"tab-btn\" onclick=\"switchTab('impl')\">Impl</button>\n");
        sb.append("        </div>\n");

        // Body
        sb.append("        <div class=\"drawer-body\">\n");

        // Responses tab (default)
        sb.append("            <div class=\"tab-panel active\" id=\"panel-responses\">\n");
        sb.append("                <div class=\"response-panels\">\n");
        sb.append("                    <div class=\"response-panel\">\n");
        sb.append("                        <div class=\"panel-header\" id=\"confluentHeader\">Confluent Response</div>\n");
        sb.append("                        <pre id=\"confluentBody\"></pre>\n");
        sb.append("                    </div>\n");
        sb.append("                    <div class=\"response-panel\">\n");
        sb.append("                        <div class=\"panel-header\" id=\"apicurioHeader\">Apicurio Response</div>\n");
        sb.append("                        <pre id=\"apicurioBody\"></pre>\n");
        sb.append("                    </div>\n");
        sb.append("                </div>\n");
        sb.append("            </div>\n");

        // Request tab
        sb.append("            <div class=\"tab-panel\" id=\"panel-request\">\n");
        sb.append("                <div class=\"doc-section\">\n");
        sb.append("                    <div class=\"request-field\"><span class=\"request-label\">Method:</span> <span id=\"requestMethod\"></span></div>\n");
        sb.append("                    <div class=\"request-field\"><span class=\"request-label\">Endpoint:</span> <code id=\"requestEndpoint\"></code></div>\n");
        sb.append("                    <div class=\"request-field\">\n");
        sb.append("                        <span class=\"request-label\">Payload:</span>\n");
        sb.append("                        <pre class=\"request-payload\" id=\"requestPayload\"></pre>\n");
        sb.append("                    </div>\n");
        sb.append("                </div>\n");
        sb.append("            </div>\n");

        // Test Code tab
        sb.append("            <div class=\"tab-panel\" id=\"panel-testcode\">\n");
        sb.append("                <div class=\"code-class-header\" id=\"codeClassHeader\"></div>\n");
        sb.append("                <pre class=\"code-block\" id=\"codeBlock\"></pre>\n");
        sb.append("            </div>\n");

        // API Spec tab (combined spec + docs)
        sb.append("            <div class=\"tab-panel\" id=\"panel-apispec\">\n");
        sb.append("                <div class=\"api-spec-header\" id=\"apiSpecHeader\"></div>\n");
        sb.append("                <div class=\"doc-section\">\n");
        sb.append("                    <h3>Description</h3>\n");
        sb.append("                    <p id=\"apiSpecDescription\"></p>\n");
        sb.append("                </div>\n");
        sb.append("                <div class=\"doc-section\">\n");
        sb.append("                    <h3>Confluent Documentation</h3>\n");
        sb.append("                    <p><a id=\"docsLink\" href=\"#\" target=\"_blank\" rel=\"noopener\">View Documentation</a></p>\n");
        sb.append("                </div>\n");
        sb.append("            </div>\n");

        // Impl tab
        sb.append("            <div class=\"tab-panel\" id=\"panel-impl\">\n");
        sb.append("                <div class=\"doc-section\">\n");
        sb.append("                    <h3>Apicurio Implementation</h3>\n");
        sb.append("                    <p id=\"implHint\"></p>\n");
        sb.append("                    <p><a id=\"implLink\" href=\"#\" target=\"_blank\" rel=\"noopener\">View on GitHub</a></p>\n");
        sb.append("                </div>\n");
        sb.append("            </div>\n");

        sb.append("        </div>\n"); // end drawer-body

        // Triage bar (fixed at bottom)
        sb.append("        <div class=\"triage-bar\">\n");
        sb.append("            <label>Status</label>\n");
        sb.append("            <select id=\"triageStatus\" onchange=\"saveTriage()\">\n");
        sb.append("                <option value=\"Untriaged\">Untriaged</option>\n");
        sb.append("                <option value=\"Investigating\">Investigating</option>\n");
        sb.append("                <option value=\"Confirmed Bug\">Confirmed Bug</option>\n");
        sb.append("                <option value=\"Known Gap\">Known Gap</option>\n");
        sb.append("                <option value=\"Won't Fix\">Won't Fix</option>\n");
        sb.append("            </select>\n");
        sb.append("            <label>Severity</label>\n");
        sb.append("            <select id=\"triageSeverity\" onchange=\"saveTriage()\">\n");
        sb.append("                <option value=\"\">--</option>\n");
        sb.append("                <option value=\"Critical\">Critical</option>\n");
        sb.append("                <option value=\"High\">High</option>\n");
        sb.append("                <option value=\"Medium\">Medium</option>\n");
        sb.append("                <option value=\"Low\">Low</option>\n");
        sb.append("            </select>\n");
        sb.append("            <label>Notes</label>\n");
        sb.append("            <input type=\"text\" id=\"triageNotes\" placeholder=\"Add notes...\" oninput=\"saveTriage()\" />\n");
        sb.append("        </div>\n");

        sb.append("    </div>\n"); // end drawer
    }

    // ---------------------------------------------------------------
    // JavaScript
    // ---------------------------------------------------------------

    private void appendJavaScript(StringBuilder sb) {
        sb.append("    <script>\n");

        // Current drawer index
        sb.append("    var _currentIdx = -1;\n\n");

        // -- openDrawer --
        sb.append("    function openDrawer(idx) {\n");
        sb.append("        _currentIdx = idx;\n");
        sb.append("        var d = TEST_DATA[idx];\n");
        sb.append("        document.getElementById('drawerTitle').textContent = d.testName;\n");
        sb.append("        var detailsEl = document.getElementById('drawerDetails');\n");
        sb.append("        detailsEl.textContent = d.details;\n");
        sb.append("        detailsEl.className = d.result === 'FAIL' ? 'drawer-details fail' : 'drawer-details';\n");
        sb.append("        document.getElementById('confluentHeader').textContent = 'Confluent Response (' + d.confluentStatus + ')';\n");
        sb.append("        document.getElementById('apicurioHeader').textContent = 'Apicurio Response (' + d.apicurioStatus + ')';\n");
        sb.append("        document.getElementById('confluentBody').textContent = d.confluentBody;\n");
        sb.append("        document.getElementById('apicurioBody').textContent = d.apicurioBody;\n");

        // Request tab
        sb.append("        document.getElementById('requestMethod').textContent = d.method;\n");
        sb.append("        document.getElementById('requestEndpoint').textContent = d.endpoint;\n");
        sb.append("        var reqPayload = document.getElementById('requestPayload');\n");
        sb.append("        if (d.requestPayload && d.requestPayload.trim()) {\n");
        sb.append("            reqPayload.textContent = d.requestPayload;\n");
        sb.append("            reqPayload.className = 'request-payload';\n");
        sb.append("        } else {\n");
        sb.append("            reqPayload.textContent = '(no request body)';\n");
        sb.append("            reqPayload.className = 'request-payload empty';\n");
        sb.append("        }\n");

        // Test Code tab
        sb.append("        document.getElementById('codeClassHeader').textContent = d.testClassName + '.' + d.testMethodName;\n");
        sb.append("        document.getElementById('codeBlock').innerHTML = highlightJava(d.testSourceCode);\n");

        // API Spec tab
        sb.append("        var specHeader = document.getElementById('apiSpecHeader');\n");
        sb.append("        var specDesc = document.getElementById('apiSpecDescription');\n");
        sb.append("        if (d.openApiOperation && d.openApiOperation.trim()) {\n");
        sb.append("            try {\n");
        sb.append("                var spec = JSON.parse(d.openApiOperation);\n");
        sb.append("                specHeader.textContent = (spec.method || d.method) + ' ' + (spec.path || d.endpoint);\n");
        sb.append("                specDesc.textContent = spec.summary || d.endpoint;\n");
        sb.append("            } catch(e) {\n");
        sb.append("                specHeader.textContent = d.method + ' ' + d.endpoint;\n");
        sb.append("                specDesc.textContent = d.openApiOperation;\n");
        sb.append("            }\n");
        sb.append("        } else {\n");
        sb.append("            specHeader.textContent = d.method + ' ' + d.endpoint;\n");
        sb.append("            specDesc.textContent = 'No description available.';\n");
        sb.append("        }\n");
        sb.append("        var docsLink = document.getElementById('docsLink');\n");
        sb.append("        if (d.confluentDocUrl) {\n");
        sb.append("            docsLink.href = d.confluentDocUrl;\n");
        sb.append("            docsLink.style.display = '';\n");
        sb.append("        } else {\n");
        sb.append("            docsLink.style.display = 'none';\n");
        sb.append("        }\n");

        // Impl tab
        sb.append("        var implHint = document.getElementById('implHint');\n");
        sb.append("        var implLink = document.getElementById('implLink');\n");
        sb.append("        if (d.apicurioImplHint) {\n");
        sb.append("            implHint.textContent = d.apicurioImplHint;\n");
        sb.append("            if (d.apicurioImplHint.startsWith('http')) {\n");
        sb.append("                implLink.href = d.apicurioImplHint;\n");
        sb.append("                implLink.style.display = '';\n");
        sb.append("            } else {\n");
        sb.append("                implLink.href = 'https://github.com/Apicurio/apicurio-registry/blob/main/app/src/main/java/io/apicurio/registry/ccompat/rest/v8/' + d.apicurioImplHint;\n");
        sb.append("                implLink.style.display = '';\n");
        sb.append("            }\n");
        sb.append("        } else {\n");
        sb.append("            implHint.textContent = 'No implementation hint available.';\n");
        sb.append("            implLink.style.display = 'none';\n");
        sb.append("        }\n");

        // Load triage
        sb.append("        loadTriage(d.testName);\n");

        // Reset to Responses tab
        sb.append("        switchTab('responses');\n");

        // Show drawer
        sb.append("        document.getElementById('drawer').classList.add('open');\n");
        sb.append("        document.getElementById('drawerOverlay').classList.add('visible');\n");
        sb.append("        document.removeEventListener('keydown', _escHandler);\n");
        sb.append("        document.addEventListener('keydown', _escHandler);\n");
        sb.append("    }\n\n");

        // -- closeDrawer --
        sb.append("    function closeDrawer() {\n");
        sb.append("        document.getElementById('drawer').classList.remove('open');\n");
        sb.append("        document.getElementById('drawerOverlay').classList.remove('visible');\n");
        sb.append("        document.removeEventListener('keydown', _escHandler);\n");
        sb.append("        _currentIdx = -1;\n");
        sb.append("    }\n\n");

        // ESC key handler
        sb.append("    function _escHandler(e) {\n");
        sb.append("        if (e.key === 'Escape') closeDrawer();\n");
        sb.append("    }\n\n");

        // -- openAllDrawers (cycles through visible rows) --
        sb.append("    function openAllDrawers() {\n");
        sb.append("        var firstVisibleIdx = -1;\n");
        sb.append("        var rows = document.querySelectorAll('tr.main-row');\n");
        sb.append("        for (var i = 0; i < rows.length; i++) {\n");
        sb.append("            if (rows[i].style.display !== 'none') {\n");
        sb.append("                if (firstVisibleIdx === -1) firstVisibleIdx = parseInt(rows[i].dataset.idx);\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        if (firstVisibleIdx >= 0) openDrawer(firstVisibleIdx);\n");
        sb.append("    }\n\n");

        // -- switchTab --
        sb.append("    function switchTab(tabName) {\n");
        sb.append("        var panels = document.querySelectorAll('.tab-panel');\n");
        sb.append("        for (var i = 0; i < panels.length; i++) {\n");
        sb.append("            panels[i].classList.remove('active');\n");
        sb.append("        }\n");
        sb.append("        var btns = document.querySelectorAll('.tab-btn');\n");
        sb.append("        for (var i = 0; i < btns.length; i++) {\n");
        sb.append("            btns[i].classList.remove('active');\n");
        sb.append("        }\n");
        sb.append("        var panel = document.getElementById('panel-' + tabName);\n");
        sb.append("        if (panel) panel.classList.add('active');\n");
        // Find the button whose onclick contains the tab name
        sb.append("        var btns2 = document.querySelectorAll('.tab-btn');\n");
        sb.append("        for (var j = 0; j < btns2.length; j++) {\n");
        sb.append("            if (btns2[j].getAttribute('onclick').indexOf(\"'\" + tabName + \"'\") !== -1) {\n");
        sb.append("                btns2[j].classList.add('active');\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        // -- loadTriage --
        sb.append("    function loadTriage(testName) {\n");
        sb.append("        var raw = null;\n");
        sb.append("        try { raw = localStorage.getItem('apicurio-triage-' + testName); } catch(e) {}\n");
        sb.append("        var data = raw ? JSON.parse(raw) : {};\n");
        sb.append("        document.getElementById('triageStatus').value = data.status || 'Untriaged';\n");
        sb.append("        document.getElementById('triageSeverity').value = data.severity || '';\n");
        sb.append("        document.getElementById('triageNotes').value = data.notes || '';\n");
        // Update the triage dot for the current row
        sb.append("        if (_currentIdx >= 0) {\n");
        sb.append("            var dot = document.getElementById('dot-' + _currentIdx);\n");
        sb.append("            if (dot) dot.setAttribute('data-status', data.status || '');\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        // -- saveTriage --
        sb.append("    function saveTriage() {\n");
        sb.append("        if (_currentIdx < 0) return;\n");
        sb.append("        var d = TEST_DATA[_currentIdx];\n");
        sb.append("        var data = {\n");
        sb.append("            status: document.getElementById('triageStatus').value,\n");
        sb.append("            severity: document.getElementById('triageSeverity').value,\n");
        sb.append("            notes: document.getElementById('triageNotes').value\n");
        sb.append("        };\n");
        sb.append("        try { localStorage.setItem('apicurio-triage-' + d.testName, JSON.stringify(data)); } catch(e) {}\n");
        // Update dot
        sb.append("        var dot = document.getElementById('dot-' + _currentIdx);\n");
        sb.append("        if (dot) dot.setAttribute('data-status', data.status);\n");
        sb.append("    }\n\n");

        // -- filterResults --
        sb.append("    function filterResults(result) {\n");
        sb.append("        var rows = document.querySelectorAll('tr.main-row');\n");
        sb.append("        for (var i = 0; i < rows.length; i++) {\n");
        sb.append("            if (!result) { rows[i].style.display = ''; }\n");
        sb.append("            else { rows[i].style.display = rows[i].dataset.result === result ? '' : 'none'; }\n");
        sb.append("        }\n");
        sb.append("        var btns = document.querySelectorAll('.toolbar button');\n");
        sb.append("        for (var i = 0; i < btns.length; i++) { btns[i].classList.remove('active'); }\n");
        sb.append("        var btn = result ? document.getElementById('filter-' + result) : document.getElementById('filter-all');\n");
        sb.append("        if (btn) btn.classList.add('active');\n");
        sb.append("    }\n\n");

        // -- highlightJava --
        sb.append("    function highlightJava(code) {\n");
        sb.append("        if (!code) return '';\n");
        sb.append("        code = code.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');\n");
        // 1. Line comments
        sb.append("        code = code.replace(/(\\/\\/.*?)$/gm, '<span class=\"hl-comment\">$1</span>');\n");
        // 2. Strings (double-quoted, handle escaped quotes)
        sb.append("        code = code.replace(/(\"(?:[^\"\\\\]|\\\\.)*\")/g, '<span class=\"hl-string\">$1</span>');\n");
        // 3. Annotations
        sb.append("        code = code.replace(/(@\\w+)/g, '<span class=\"hl-annotation\">$1</span>');\n");
        // 4. Keywords
        sb.append("        code = code.replace(/\\b(void|return|new|public|private|protected|class|int|String|var|if|else|for|while|try|catch|throw|throws|import|package|static|final|extends|implements|boolean|long|byte|char|short|float|double|this|super|null|true|false)\\b/g,\n");
        sb.append("            '<span class=\"hl-keyword\">$1</span>');\n");
        sb.append("        return code;\n");
        sb.append("    }\n\n");

        // -- init: restore all triage dots on page load --
        sb.append("    (function initDots() {\n");
        sb.append("        for (var i = 0; i < TEST_DATA.length; i++) {\n");
        sb.append("            var dot = document.getElementById('dot-' + i);\n");
        sb.append("            if (!dot) continue;\n");
        sb.append("            var raw = null;\n");
        sb.append("            try { raw = localStorage.getItem('apicurio-triage-' + TEST_DATA[i].testName); } catch(e) {}\n");
        sb.append("            if (raw) {\n");
        sb.append("                try {\n");
        sb.append("                    var d = JSON.parse(raw);\n");
        sb.append("                    if (d.status) dot.setAttribute('data-status', d.status);\n");
        sb.append("                } catch(e) {}\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("    })();\n");

        // Table sorting
        sb.append("    var sortCol = -1, sortAsc = true;\n");
        sb.append("    function sortTable(col) {\n");
        sb.append("        if (sortCol === col) { sortAsc = !sortAsc; } else { sortCol = col; sortAsc = true; }\n");
        sb.append("        var table = document.getElementById('results-table');\n");
        sb.append("        var rows = Array.from(table.querySelectorAll('tbody tr'));\n");
        sb.append("        rows.sort(function(a, b) {\n");
        sb.append("            var va = a.cells[col].textContent.trim();\n");
        sb.append("            var vb = b.cells[col].textContent.trim();\n");
        sb.append("            var cmp = va < vb ? -1 : va > vb ? 1 : 0;\n");
        sb.append("            return sortAsc ? cmp : -cmp;\n");
        sb.append("        });\n");
        sb.append("        rows.forEach(function(r) { table.querySelector('tbody').appendChild(r); });\n");
        sb.append("        var ths = table.querySelectorAll('thead th');\n");
        sb.append("        ths.forEach(function(th) { th.className = ''; });\n");
        sb.append("        if (col >= 0 && ths[col]) ths[col].className = sortAsc ? 'sorted-asc' : 'sorted-desc';\n");
        sb.append("    }\n");

        sb.append("    </script>\n");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String prettifyJson(String json) {
        if (json == null || json.isEmpty()) return "";
        try {
            Object obj = MAPPER.readValue(json, Object.class);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return json;
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String escapeJs(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("</", "<\\/");
    }
}
