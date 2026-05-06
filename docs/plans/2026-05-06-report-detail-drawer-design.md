# Report Detail Drawer Design

**Date**: 2026-05-06
**Status**: Approved

## Problem

The current HTML compatibility report is presentational - it shows test results but provides no path from a failure to its root cause. Apicurio maintainers need to manually locate test code, API specs, implementation files, and Confluent docs to triage failures.

## Solution

Transform the report from a "results dashboard" into a "triage workbench" by adding a slide-out Detail Drawer with tabbed context panels and localStorage-based triage state.

## Audience

Apicurio maintainers (familiar with the codebase).

## Constraints

- Must remain a single standalone HTML file (no external dependencies, no server)
- All context is baked in at report generation time
- Report is generated via JVM shutdown hook after all tests complete

## Approach: Detail Drawer

When a test row is clicked, a right-side drawer slides in overlaying ~60% of the viewport. The inline row expansion (current behavior) is removed.

### Drawer Structure

```
+------------------------------------------------------+
| [X] testName                                   TRIAGE |
+------------------------------------------------------+
| [Responses] [Test Code] [API Spec] [Docs] [Impl]     |
+------------------------------------------------------+
|                                                       |
|  (Tab content area)                                   |
|                                                       |
+------------------------------------------------------+
| Triage bar:                                           |
| [Status v] [Severity v] [Notes: _________] [Save]    |
+------------------------------------------------------+
```

### Tabs

1. **Responses** (default) - Side-by-side Confluent/Apicurio response comparison (migrated from current inline expansion)
2. **Test Code** - Syntax-highlighted Java source of the test method
3. **API Spec** - OpenAPI operation details (method, path, parameters, request/response schemas)
4. **Docs** - Confluent docs excerpt + deep link
5. **Impl** - Apicurio ccompat implementation class reference + GitHub link

### Triage Bar

Persisted in `localStorage` keyed by `apicurio-triage-{testName}`:

```json
{
  "status": "untriaged|investigating|confirmed-bug|known-gap|wont-fix",
  "severity": "critical|high|medium|low",
  "notes": "free text"
}
```

### Main Table Changes

- Clicking a row opens the drawer (replaces inline expansion)
- Small icon/badge shows triage status per row
- Expand All / Collapse All buttons remain but operate the drawer

## Data Model Changes

### TestOutcome - New Fields

| Field | Type | Purpose |
|-------|------|---------|
| `testClassName` | String | Fully qualified test class name |
| `testMethodName` | String | Test method name |
| `testSourceCode` | String | Full Java source of the test method |
| `openApiOperation` | String | OpenAPI operation summary for the endpoint |
| `confluentDocUrl` | String | Deep link to Confluent docs |
| `apicurioImplHint` | String | Apicurio ccompat class/method reference |

## Technical Design

### New Class: ReportContextEnricher

Maps `(method, endpointPattern)` tuples to static context:

- **OpenAPI summaries**: Hand-curated map of ~20 entries covering all v8 endpoints
- **Confluent docs URLs**: Base URL + anchor for each operation
- **Apicurio impl references**: File paths in the apicurio-registry GitHub repo

For test source code: reads `.java` files from `src/test/` at generation time, extracts method bodies using regex-based parsing.

### Modified Classes

| Class | Change |
|-------|--------|
| `TestOutcome` | Add 6 new builder fields |
| `AbstractCompatibilityTest` | Pass test class/method names through `recordOutcome()` |
| `HtmlReportGenerator` | Major rewrite for drawer HTML, tabs, triage JS |
| `CompatibilityReportExtension` | Call enricher before generating report |

### Syntax Highlighting

Inline regex-based Java syntax coloring (~30 lines of JS). No external libraries.

### Context Map (Static Data)

The enricher maintains a static map:

```
("GET", "/subjects")                    -> SubjectsResourceImpl, "List all subjects"
("GET", "/subjects/{subject}/versions") -> SubjectsResourceImpl, "List versions under subject"
("POST", "/subjects/{subject}/versions") -> SubjectsResourceImpl, "Register schema"
...
```

Each entry includes:
- OpenAPI operation summary
- Confluent docs anchor URL
- Apicurio GitHub file reference

## Scope

- In scope: Drawer UI, 5 context tabs, triage persistence, data enrichment
- Out of scope: Live test replay, API explorer, server-side components, external dependencies
