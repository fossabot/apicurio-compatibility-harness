# Report Detail Drawer Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Transform the HTML compatibility report into a triage workbench with a slide-out detail drawer containing tabbed context panels and localStorage-based triage state.

**Architecture:** Extend `TestOutcome` with 6 new fields. Add a `ReportContextEnricher` that maps `(method, endpoint)` to static context data and reads test source files. Rewrite `HtmlReportGenerator` to emit drawer HTML, tab JS, and triage persistence. Pass test class/method names through `AbstractCompatibilityTest.recordOutcome()`.

**Tech Stack:** Java 17, Jackson, vanilla HTML/CSS/JS, localStorage

---

### Task 1: Extend TestOutcome with new fields

**Files:**
- Modify: `src/main/java/io/apicurio/registry/compatibility/model/TestOutcome.java`

**Step 1: Add new fields to the class**

Add after the existing fields (line 16):

```java
    private final String testClassName;
    private final String testMethodName;
    private final String testSourceCode;
    private final String openApiOperation;
    private final String confluentDocUrl;
    private final String apicurioImplHint;
```

Add to the constructor (after line 27):

```java
        this.testClassName = builder.testClassName;
        this.testMethodName = builder.testMethodName;
        this.testSourceCode = builder.testSourceCode;
        this.openApiOperation = builder.openApiOperation;
        this.confluentDocUrl = builder.confluentDocUrl;
        this.apicurioImplHint = builder.apicurioImplHint;
```

Add getters (after line 38):

```java
    public String getTestClassName() { return testClassName; }
    public String getTestMethodName() { return testMethodName; }
    public String getTestSourceCode() { return testSourceCode; }
    public String getOpenApiOperation() { return openApiOperation; }
    public String getConfluentDocUrl() { return confluentDocUrl; }
    public String getApicurioImplHint() { return apicurioImplHint; }
```

Add builder fields (after line 53):

```java
        private String testClassName;
        private String testMethodName;
        private String testSourceCode;
        private String openApiOperation;
        private String confluentDocUrl;
        private String apicurioImplHint;
```

Add builder setters (after line 63):

```java
        public Builder testClassName(String testClassName) { this.testClassName = testClassName; return this; }
        public Builder testMethodName(String testMethodName) { this.testMethodName = testMethodName; return this; }
        public Builder testSourceCode(String testSourceCode) { this.testSourceCode = testSourceCode; return this; }
        public Builder openApiOperation(String openApiOperation) { this.openApiOperation = openApiOperation; return this; }
        public Builder confluentDocUrl(String confluentDocUrl) { this.confluentDocUrl = confluentDocUrl; return this; }
        public Builder apicurioImplHint(String apicurioImplHint) { this.apicurioImplHint = apicurioImplHint; return this; }
```

**Step 2: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/io/apicurio/registry/compatibility/model/TestOutcome.java
git commit -m "feat: extend TestOutcome with context fields for report enrichment"
```

---

### Task 2: Create ReportContextEnricher

**Files:**
- Create: `src/main/java/io/apicurio/registry/compatibility/report/ReportContextEnricher.java`

**Step 1: Create the enricher class**

This class:
1. Has a static map of `(method, endpoint)` -> `EndpointContext` (summary, doc URL, impl hint)
2. Reads test `.java` source files from `src/test/` using the test class name
3. Extracts individual method bodies using regex
4. Has an `enrich(List<TestOutcome>)` method that sets the new fields on each outcome

```java
package io.apicurio.registry.compatibility.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.apicurio.registry.compatibility.model.TestOutcome;

public class ReportContextEnricher {

    private static final String CONFLUENT_DOCS_BASE = "https://docs.confluent.io/platform/current/schema-registry/develop/api.html";
    private static final String APICURIO_GITHUB_BASE = "https://github.com/Apicurio/apicurio-registry/blob/main/app/src/main/java/io/apicurio/registry/ccompat/rest/v8";

    private record EndpointContext(String summary, String docAnchor, String implFile) {}

    private static final Map<String, EndpointContext> CONTEXT_MAP = Map.ofEntries(
        // Subjects
        entry("GET:/subjects", new EndpointContext(
            "List all registered subjects",
            "get--subjects",
            "SubjectsResourceImpl.java")),
        entry("GET:/subjects/{subject}/versions", new EndpointContext(
            "List versions registered under a subject",
            "get--subjects-subject-versions",
            "SubjectsResourceImpl.java")),
        entry("GET:/subjects/{subject}/versions/{version}", new EndpointContext(
            "Get a specific version of a schema under a subject",
            "get--subjects-subject-versions-version",
            "SubjectsResourceImpl.java")),
        entry("GET:/subjects/{subject}/versions/{version}/schema", new EndpointContext(
            "Get the raw schema string for a version",
            "get--subjects-subject-versions-version-schema",
            "SubjectsResourceImpl.java")),
        entry("POST:/subjects/{subject}/versions", new EndpointContext(
            "Register a new schema under a subject",
            "post--subjects-subject-versions",
            "SubjectsResourceImpl.java")),
        entry("POST:/subjects/{subject}/versions/{version}", new EndpointContext(
            "Look up schema version by content",
            "post--subjects-subject-versions-version",
            "SubjectsResourceImpl.java")),
        entry("DELETE:/subjects/{subject}", new EndpointContext(
            "Delete a subject and all its versions",
            "delete--subjects-subject",
            "SubjectsResourceImpl.java")),
        entry("DELETE:/subjects/{subject}/versions/{version}", new EndpointContext(
            "Delete a specific version of a subject",
            "delete--subjects-subject-versions-version",
            "SubjectsResourceImpl.java")),
        entry("GET:/subjects/{subject}/versions/{version}/referencedby", new EndpointContext(
            "Find schemas that reference the given version",
            "get--subjects-subject-versions-version-referencedby",
            "SubjectsResourceImpl.java")),
        // Schemas
        entry("GET:/schemas/ids/{id}", new EndpointContext(
            "Get schema by global ID",
            "get--schemas-ids-id",
            "SchemasResourceImpl.java")),
        entry("GET:/schemas/ids/{id}/versions", new EndpointContext(
            "Get all versions for a schema global ID",
            "get--schemas-ids-id-versions",
            "SchemasResourceImpl.java")),
        entry("GET:/schemas/ids/{id}/subjects", new EndpointContext(
            "Get all subjects referencing a schema global ID",
            "get--schemas-ids-id-subjects",
            "SchemasResourceImpl.java")),
        // Compatibility
        entry("POST:/compatibility/subjects/{subject}/versions/{version}", new EndpointContext(
            "Test schema compatibility against a specific version",
            "post--compatibility-subjects-subject-versions-version",
            "CompatibilityResourceImpl.java")),
        // Config
        entry("GET:/config", new EndpointContext(
            "Get global compatibility configuration",
            "get--config",
            "ConfigResourceImpl.java")),
        entry("PUT:/config", new EndpointContext(
            "Update global compatibility configuration",
            "put--config",
            "ConfigResourceImpl.java")),
        entry("GET:/config/{subject}", new EndpointContext(
            "Get compatibility configuration for a subject",
            "get--config-subject",
            "ConfigResourceImpl.java")),
        entry("PUT:/config/{subject}", new EndpointContext(
            "Update compatibility configuration for a subject",
            "put--config-subject",
            "ConfigResourceImpl.java")),
        // Mode
        entry("GET:/mode", new EndpointContext(
            "Get the registry mode",
            "get--mode",
            "ModeResourceImpl.java")),
        entry("PUT:/mode", new EndpointContext(
            "Update the registry mode",
            "put--mode",
            "ModeResourceImpl.java"))
    );

    private static Map.Entry<String, EndpointContext> entry(String key, EndpointContext val) {
        return Map.entry(key, val);
    }

    public void enrich(List<TestOutcome> outcomes) {
        Map<String, String> sourceCache = new java.util.HashMap<>();

        for (int i = 0; i < outcomes.size(); i++) {
            TestOutcome original = outcomes.get(i);
            EndpointContext ctx = CONTEXT_MAP.get(original.getMethod() + ":" + original.getEndpoint());

            String openApiOperation = ctx != null ? ctx.summary() : "";
            String confluentDocUrl = ctx != null ? CONFLUENT_DOCS_BASE + "#" + ctx.docAnchor() : CONFLUENT_DOCS_BASE;
            String apicurioImplHint = ctx != null ? APICURIO_GITHUB_BASE + "/" + ctx.implFile() : "";

            String testClassName = original.getTestClassName();
            String testMethodName = original.getTestMethodName();
            String testSourceCode = "";

            if (testClassName != null && testMethodName != null) {
                String classSource = sourceCache.computeIfAbsent(testClassName, this::readTestClassSource);
                if (classSource != null) {
                    testSourceCode = extractMethodBody(classSource, testMethodName);
                }
            }

            TestOutcome enriched = TestOutcome.builder()
                    .testName(original.getTestName())
                    .endpoint(original.getEndpoint())
                    .method(original.getMethod())
                    .result(original.getResult())
                    .confluentStatus(original.getConfluentStatus())
                    .apicurioStatus(original.getApicurioStatus())
                    .details(original.getDetails())
                    .confluentBody(original.getConfluentBody())
                    .apicurioBody(original.getApicurioBody())
                    .testClassName(testClassName)
                    .testMethodName(testMethodName)
                    .testSourceCode(testSourceCode)
                    .openApiOperation(openApiOperation)
                    .confluentDocUrl(confluentDocUrl)
                    .apicurioImplHint(apicurioImplHint)
                    .build();

            outcomes.set(i, enriched);
        }
    }

    String readTestClassSource(String className) {
        String relativePath = className.replace('.', '/') + ".java";
        Path sourcePath = Path.of("src/test/java", relativePath);
        if (Files.exists(sourcePath)) {
            try {
                return Files.readString(sourcePath);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    String extractMethodBody(String classSource, String methodName) {
        // Match the method from its annotation+signature through to the closing brace at the correct nesting level
        // Look for @Test/@DisplayName annotations preceding the method
        Pattern pattern = Pattern.compile(
            "((?:@\\w+(?:\\([^)]*\\))?\\s*)*)" +  // annotations
            "(?:public|private|protected)?\\s+" +
            "(?:void|\\w+)\\s+" +
            Pattern.quote(methodName) +
            "\\s*\\([^)]*\\)\\s*(?:throws[^{]*)?\\{",
            Pattern.MULTILINE
        );
        Matcher matcher = pattern.matcher(classSource);
        if (!matcher.find()) {
            return "";
        }

        int start = matcher.start();
        int braceStart = classSource.indexOf('{', matcher.start());
        if (braceStart == -1) return "";

        int depth = 1;
        int pos = braceStart + 1;
        while (pos < classSource.length() && depth > 0) {
            char c = classSource.charAt(pos);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            pos++;
        }

        return classSource.substring(start, pos).trim();
    }
}
```

**Step 2: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/io/apicurio/registry/compatibility/report/ReportContextEnricher.java
git commit -m "feat: add ReportContextEnricher for endpoint context and source embedding"
```

---

### Task 3: Pass test class/method names through AbstractCompatibilityTest

**Files:**
- Modify: `src/main/java/io/apicurio/registry/compatibility/shared/AbstractCompatibilityTest.java` (lines 73-151)

**Step 1: Add method to capture calling test class and method names**

Add this private method to `AbstractCompatibilityTest` (after line 248, before the closing brace):

```java
    private String[] inferTestClassAndMethod() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        // Walk up the stack past this class and JUnit infrastructure to find the test method
        for (StackTraceElement frame : stack) {
            String cls = frame.getClassName();
            if (cls.startsWith("io.apicurio.registry.compatibility.") && !cls.endsWith("AbstractCompatibilityTest")) {
                return new String[] { cls, frame.getMethodName() };
            }
        }
        return new String[] { null, null };
    }
```

**Step 2: Update recordOutcome to capture and pass test class/method**

Replace the `recordOutcome` method (lines 136-151) with:

```java
    private void recordOutcome(String testName, String method, String endpoint,
            String confluentStatus, String apicurioStatus,
            CompatibilityTestResult result, String details,
            String confluentBody, String apicurioBody) {
        String[] classAndMethod = inferTestClassAndMethod();
        TestResultCollector.getInstance().record(TestOutcome.builder()
                .testName(testName)
                .endpoint(endpoint)
                .method(method)
                .result(result)
                .confluentStatus(confluentStatus)
                .apicurioStatus(apicurioStatus)
                .details(details)
                .confluentBody(confluentBody)
                .apicurioBody(apicurioBody)
                .testClassName(classAndMethod[0])
                .testMethodName(classAndMethod[1])
                .build());
    }
```

**Step 3: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add src/main/java/io/apicurio/registry/compatibility/shared/AbstractCompatibilityTest.java
git commit -m "feat: capture test class and method names in TestOutcome"
```

---

### Task 4: Wire enricher into report generation

**Files:**
- Modify: `src/main/java/io/apicurio/registry/compatibility/shared/CompatibilityReportExtension.java` (lines 27-36)

**Step 1: Update shutdown hook to enrich outcomes before generating report**

Replace the shutdown hook body (lines 28-35) with:

```java
                try {
                    TestResultCollector collector = TestResultCollector.getInstance();
                    ReportContextEnricher enricher = new ReportContextEnricher();
                    enricher.enrich(collector.getOutcomesForEnrichment());
                    HtmlReportGenerator generator = new HtmlReportGenerator(collector);
                    generator.generate(Path.of(REPORT_PATH));
                    System.out.println("Compatibility report written to " + REPORT_PATH
                            + " (" + collector.getTotalCount() + " results)");
                } catch (IOException e) {
                    System.err.println("Failed to generate compatibility report: " + e.getMessage());
                }
```

Add the import:

```java
import io.apicurio.registry.compatibility.report.ReportContextEnricher;
```

**Step 2: Add mutable accessor to TestResultCollector**

In `TestResultCollector.java`, add this method (after `getOutcomes()` at line 30):

```java
    public List<TestOutcome> getOutcomesForEnrichment() {
        return outcomes;
    }
```

This returns the mutable list so the enricher can replace items in-place. The existing `getOutcomes()` continues returning an unmodifiable copy.

**Step 3: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add src/main/java/io/apicurio/registry/compatibility/shared/CompatibilityReportExtension.java \
        src/main/java/io/apicurio/registry/compatibility/collector/TestResultCollector.java
git commit -m "feat: wire ReportContextEnricher into report generation pipeline"
```

---

### Task 5: Rewrite HtmlReportGenerator with drawer UI

**Files:**
- Modify: `src/main/java/io/apicurio/registry/compatibility/report/HtmlReportGenerator.java` (full rewrite)

This is the largest task. The entire `buildHtml` method and its CSS/JS need to be rewritten.

**Step 1: Rewrite the HtmlReportGenerator**

The new generator must produce:
1. CSS for the drawer overlay (fixed position, right-side slide-in, 60% width)
2. CSS for tabs inside the drawer
3. CSS for triage bar at drawer bottom
4. CSS for syntax-highlighted code blocks
5. HTML for the drawer container (hidden by default)
6. JS to open/close the drawer, switch tabs, load content per test
7. JS for triage persistence with localStorage
8. JS for inline Java syntax highlighting (keywords, strings, comments, annotations)
9. Keep the existing summary cards and filter toolbar
10. Replace inline row expansion with drawer trigger

The key change: instead of rendering `detail-row` TRs inline, the generator renders all test context data into a JavaScript array (`TEST_DATA`). When a row is clicked, the drawer opens and populates its tabs from the matching entry in `TEST_DATA`.

**Important constraints for the engineer:**
- All CSS is inline in a `<style>` block (no external files)
- All JS is inline in a `<script>` block
- The `TEST_DATA` JS array holds one object per test with all fields needed for every tab
- Test source code must be HTML-escaped and stored as JS string literals
- The triage bar uses `localStorage.getItem('apicurio-triage-' + testName)` and `localStorage.setItem(...)`
- The syntax highlighter is a JS function `highlightJava(code)` that wraps keywords/strings/comments in `<span class="hl-xxx">` tags
- The drawer has a CSS transition for slide-in animation

Here's the structure of the JS data array per entry:

```javascript
{testName:"...", endpoint:"...", method:"...", result:"...",
 confluentStatus:"...", apicurioStatus:"...", details:"...",
 confluentBody:"...", apicurioBody:"...",
 testClassName:"...", testMethodName:"...",
 testSourceCode:"...", openApiOperation:"...",
 confluentDocUrl:"...", apicurioImplHint:"..."}
```

The drawer HTML structure:

```html
<div id="drawer-overlay" class="drawer-overlay" onclick="closeDrawer()"></div>
<div id="drawer" class="drawer">
  <div class="drawer-header">
    <span id="drawer-title"></span>
    <button class="drawer-close" onclick="closeDrawer()">&times;</button>
  </div>
  <div class="drawer-tabs">
    <button class="tab-btn active" onclick="switchTab('responses')">Responses</button>
    <button class="tab-btn" onclick="switchTab('testcode')">Test Code</button>
    <button class="tab-btn" onclick="switchTab('apispec')">API Spec</button>
    <button class="tab-btn" onclick="switchTab('docs')">Docs</button>
    <button class="tab-btn" onclick="switchTab('impl')">Impl</button>
  </div>
  <div class="drawer-body" id="drawer-body">
    <!-- Tab content injected by JS -->
  </div>
  <div class="triage-bar">
    <select id="triage-status" onchange="saveTriage()">...</select>
    <select id="triage-severity" onchange="saveTriage()">...</select>
    <input id="triage-notes" placeholder="Notes..." onblur="saveTriage()">
  </div>
</div>
```

**Step 2: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/io/apicurio/registry/compatibility/report/HtmlReportGenerator.java
git commit -m "feat: rewrite report generator with drawer UI, tabs, and triage"
```

---

### Task 6: Manual integration test

**Files:**
- All modified files

**Step 1: Start test infrastructure**

Run: `podman-compose up -d`

**Step 2: Run the full test suite**

Run: `mvn clean test`

Expected: All existing tests pass (same behavior as before, just with enriched outcomes).

**Step 3: Open the generated report**

Run: `xdg-open target/compatibility-report.html` (or open in browser)

Verify:
- Summary cards and filter toolbar work as before
- Clicking a test row opens the drawer sliding in from the right
- Drawer shows 5 tabs
- "Responses" tab shows side-by-side Confluent/Apicurio response bodies
- "Test Code" tab shows the Java source of the test method with syntax highlighting
- "API Spec" tab shows the OpenAPI operation summary for the endpoint
- "Docs" tab shows a description and a clickable link to Confluent docs
- "Impl" tab shows the Apicurio implementation class name and a clickable GitHub link
- Triage bar at the bottom has status, severity, and notes fields
- Setting triage values, closing and reopening the report restores triage state
- Closing the drawer returns to the table view

**Step 4: Commit any fixes**

If any issues found, fix and commit.

**Step 5: Tear down infrastructure**

Run: `podman-compose down`

---

### Task 7: Add triage indicator to table rows

**Files:**
- Modify: `src/main/java/io/apicurio/registry/compatibility/report/HtmlReportGenerator.java`

**Step 1: Add triage status icon to each table row**

In the table row rendering, add a small colored indicator before the test name that shows triage status loaded from localStorage. This requires adding a `loadTriageIndicator(testName)` JS function that runs on page load and sets a CSS class on each row.

Add a small colored dot (8x8 circle) in the first TD of each main-row, colored by triage status:
- gray = untriaged
- yellow = investigating
- red = confirmed-bug
- blue = known-gap
- green = won't-fix

**Step 2: Verify**

Open report in browser, set triage status on a few tests, refresh page. Dots should reflect saved status.

**Step 3: Commit**

```bash
git add src/main/java/io/apicurio/registry/compatibility/report/HtmlReportGenerator.java
git commit -m "feat: add triage status indicators to table rows"
```
