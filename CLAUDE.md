
<!-- BACKLOG.MD MCP GUIDELINES START -->

<CRITICAL_INSTRUCTION>

## BACKLOG WORKFLOW INSTRUCTIONS

This project uses Backlog.md MCP for all task and project management activities.

**CRITICAL GUIDANCE**

- If your client supports MCP resources, read `backlog://workflow/overview` to understand when and how to use Backlog for this project.
- If your client only supports tools or the above request fails, call `backlog.get_backlog_instructions()` to load the tool-oriented overview. Use the `instruction` selector when you need `task-creation`, `task-execution`, or `task-finalization`.

- **First time working here?** Read the overview resource IMMEDIATELY to learn the workflow
- **Already familiar?** You should have the overview cached ("## Backlog.md Overview (MCP)")
- **When to read it**: BEFORE creating tasks, or when you're unsure whether to track work

These guides cover:
- Decision framework for when to create tasks
- Search-first workflow to avoid duplicates
- Links to detailed guides for task creation, execution, and finalization
- MCP tools reference

You MUST read the overview resource to understand the complete workflow. The information is NOT summarized here.

</CRITICAL_INSTRUCTION>

<!-- BACKLOG.MD MCP GUIDELINES END -->

---

# Apicurio Registry - Confluent v8 API Compatibility Harness

## Project Goal

Build an automated, reproducible test harness that compares Apicurio Registry's Confluent-compatible API against the official Confluent Schema Registry API v8. The output is a standalone HTML compatibility report. This project will be contributed back to the Apicurio organization, so all code must match their conventions.

**PRD Location**: `PRD.md` (authoritative requirements source)

## Upstream Reference: Apicurio Registry

- **Repository**: https://github.com/Apicurio/apicurio-registry
- **Current version**: `3.3.0-SNAPSHOT`
- **Java version**: 17 (`maven.compiler.release=17`)
- **Framework**: Quarkus **3.33.1**
- **Build tool**: Maven
- **GroupId**: `io.apicurio`
- **Confluent client version**: `8.0.0` (already declared in root POM as `confluent.version`)
- **Avro version**: `1.12.1`

### Key Apicurio Conventions

- **POM structure**: Root POM defines `quarkus.version` and all dependency versions as properties. Sub-modules inherit from root.
- **BOM import**: `io.quarkus:quarkus-bom` imported in `<dependencyManagement>`
- **Module naming**: kebab-case directory names (e.g., `app`, `common`, `serdes`, `schema-util`, `integration-tests`)
- **Artifact naming**: `apicurio-registry-{module}` pattern
- **Package naming**: `io.apicurio.registry.*`
- **Test naming**: `*Test.java` for unit tests, `*IT.java` for integration tests
- **Test framework**: JUnit 5, REST Assured (static imports from `io.restassured.RestAssured.given`)
- **Container testing**: `@QuarkusTestResource` with `QuarkusTestResourceLifecycleManager`
- **CDI annotations**: `@ApplicationScoped`, `@Inject`, `@Interceptors` (no Lombok)
- **Logging**: SLF4J (`org.slf4j.Logger`) with `@Logged` interceptor
- **Java version**: 17, 4-space indent, ~120 char line length
- **Contribution**: DCO sign-off required, squash-and-merge PRs, tests + docs mandatory

### Apicurio Confluent-Compatible API Module

Apicurio exposes Confluent-compatible REST endpoints in the `app/` module at:
- **v7**: `/apis/ccompat/v7/` (fully supported)
- **v8**: `/apis/ccompat/v8/` (fully supported)

Source code at: `app/src/main/java/io/apicurio/registry/ccompat/rest/`
- `v7/` - v7 API interfaces and implementations
- `v8/` - v8 API interfaces (delegate to v7 impls), includes:
  - `SubjectsResourceImpl.java` - delegates to v7 `SubjectsResourceImpl`
  - `SchemasResourceImpl.java`
  - `CompatibilityResourceImpl.java`
  - `ConfigResourceImpl.java`
  - `ModeResourceImpl.java`
  - `ContextResourceImpl.java`
  - `ExporterResourceImpl.java`
- `error/` - Error handling (ConflictException, SchemaNotFoundException, etc.)

### Feature Support (from ccompat README)

**Fully Supported**: Schemas, Subjects, Compatibility, Config, Mode, Contexts
**Partially Supported**: Data Contracts (metadata/ruleSet passthrough, NOT enforced)
**Not Supported**: Schema Linking/Exporters, KEKs/DEKs, Cluster Metadata

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17+ |
| Framework | Quarkus (Test) |
| Build | Maven |
| API Testing | REST Assured |
| Containers | podman + podman-compose |
| Static Analysis | openapi-diff |
| Reporting | Custom HTML via `StringBuilder` (standalone, no framework) |
| CI | GitHub Actions (podman-based) |

## Confluent Schema Registry API v8 Reference

**Note**: "v8" refers to the Confluent Platform 7.x series (CP 7.x = Schema Registry API v8).

### Endpoint Groups

| Group | Endpoints | Test Class |
|-------|-----------|------------|
| Subjects | `/subjects`, `/subjects/{subject}/versions/*`, `/subjects/{subject}` | `SubjectsEndpointTest` |
| Schemas | `/schemas/ids/{id}/*` | `SchemasEndpointTest` |
| Compatibility | `/compatibility/subjects/{subject}/versions/{version}` | `CompatibilityEndpointTest` |
| Config | `/config`, `/config/{subject}` | `ConfigEndpointTest` |
| Mode | `/mode` | `ModeEndpointTest` |

Full endpoint map and error codes are in `PRD.md`.

### Error Code Format

Confluent returns JSON with `error_code` (int) and `message` (string). Key codes: 40401 (subject not found), 40402 (version not found), 40901 (incompatible), 42201-42203 (validation errors), 50001/50003 (server errors).

### Mode Values

READWRITE (default), READONLY, READONLY_OVERRIDE, IMPORT, MIGRATE

### Request Content-Type

`application/vnd.schemaregistry.v1+json` (standard) or `application/json`

### Avro Schema Canonicalization

Avro schemas are normalized using `SchemaNormalization.toParsingForm()` before comparison. Whitespace, doc fields, and aliases are ignored. Two logically identical schemas with different formatting get the same ID.

### Compatibility Levels

NONE, BACKWARD, BACKWARD_TRANSITIVE, FORWARD, FORWARD_TRANSITIVE, FULL, FULL_TRANSITIVE

### Scope Restriction (v1.0)

- **Avro only** - Protobuf and JSON Schema are out of scope
- All Confluent v8 API endpoints must be covered
- Missing features in Apicurio must be flagged as `SKIPPED_GAP` / `KNOWN_INCOMPATIBILITY`, not hard failures

## Architecture

### Module Structure

```
apicurio-compatibility-harness/
  pom.xml                          # Root POM, aligned with Apicurio conventions
  podman-compose.yml               # Confluent + Apicurio + Kafka containers
  wait-for-ready.sh                # Health check script (usage: ./wait-for-ready.sh 180)
  src/
    main/
      java/io/apicurio/registry/compatibility/
        config/                    # Test configuration, base URLs (TestConfig)
        model/                     # CompatibilityTestResult enum, TestOutcome DTO (16 fields)
        collector/                 # TestResultCollector (thread-safe, CopyOnWriteArrayList)
        report/                    # HtmlReportGenerator (standalone HTML), ReportContextEnricher
    test/
      java/io/apicurio/registry/compatibility/
        shared/                    # AbstractCompatibilityTest, SchemaFixtures, CompatibilityReportExtension
        subjects/                  # /subjects endpoint tests
        schemas/                   # /schemas endpoint tests
        compatibility/             # /compatibility endpoint tests
        config/                    # /config endpoint tests
        mode/                      # /mode endpoint tests
```

### Container Services (podman-compose.yml)

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| kafka | `apache/kafka:3.9.0` | 9092 | Kafka broker (KRaft mode, no Zookeeper) |
| confluent-sr | `confluentinc/cp-schema-registry:7.8.0` | 8081 | Confluent Schema Registry v8 |
| apicurio-sr | `quay.io/apicurio/apicurio-registry-kafkasql:latest-snapshot` | 8082 | Apicurio Registry (nightly, shares Kafka) |

**Key**: Kafka uses KRaft mode (no Zookeeper needed). Both registries share the same Kafka cluster but use separate topics.

### Test Lifecycle

1. `podman-compose up -d` starts all services
2. Health checks confirm both registries are ready
3. Static analysis phase: OpenAPI diff + example validation
4. Runtime phase: REST Assured tests against both registries
5. Report phase: Generate HTML compatibility report
6. `podman-compose down` tears down services

## Key Design Decisions

### Test Result States

| State | Meaning |
|-------|---------|
| `PASS` | Apicurio matches Confluent behavior exactly |
| `FAIL` | Apicurio response differs from Confluent |
| `SKIPPED_GAP` | Apicurio does not support this feature (known gap) |
| `KNOWN_INCOMPATIBILITY` | Intentional deviation documented by Apicurio |
| `ERROR` | Test infrastructure issue (not an API gap) |

### Dual-Target Testing Pattern

Each test sends the **same request** to both:
- `http://localhost:8081` (Confluent Schema Registry)
- `http://localhost:8082/apis/ccompat/v8` (Apicurio Registry - note the `/apis/` prefix)

Then compares status codes, response bodies, and error codes.

## Key Patterns & Gotchas

### assertStatus=false for Known Incompatibilities

Tests use `assertCompatibility(testName, method, endpoint, confluent, apicurio, false)` to record status code differences as `FAIL` outcomes without throwing assertion errors. This lets CI pass while still surfacing all behavioral differences in the report. When `assertStatus=false`, the `assertEquals` on status codes is skipped but the outcome is still recorded.

### Report Enrichment Pipeline

At JVM shutdown (via `CompatibilityReportExtension`), `ReportContextEnricher` enriches each `TestOutcome` with:
- `testClassName`/`testMethodName` (inferred from stack trace)
- `testSourceCode` (method body extracted from source file via regex + brace counting)
- `openApiOperation`/`confluentDocUrl`/`apicurioImplHint` (static endpoint-to-context mapping)

### Report UI Features

The standalone HTML report includes a slide-out Detail Drawer with 5 tabs (Responses, Test Code, API Spec, Docs, Impl), localStorage-based triage state (status/severity/notes per test), and inline Java syntax highlighting.

### Apicurio URL Prefix

Apicurio's Confluent-compatible API is at `/apis/ccompat/v8` (note the `/apis/` prefix). Confluent's is at the root. This is handled by `TestConfig`.

### Known Apicurio Incompatibilities (as of CP 7.8.0)

- Invalid schema syntax not validated in compatibility checks (200 vs 422)
- Invalid compatibility level returns 500 instead of 422
- Invalid mode returns 400 instead of 422
- POST version lookup returns 405 (endpoint not fully supported)
- referencedBy on nonexistent subject returns 200 (empty list) instead of 404
- Schema registration may not include `version` field in response

## CI/CD (GitHub Actions)

### Compatibility Tests (`.github/workflows/compatibility-tests.yml`)
- Runs on push/PR to main
- Starts podman containers, waits for health checks via `wait-for-ready.sh`
- Runs `mvn clean test -Dconfluent.registry.version=VERSION` (version extracted from `podman-compose.yml`)
- Uploads `target/compatibility-report.html` as artifact
- On push to main: publishes summary as comment on pinned GitHub Issue #1 ("Compatibility Test Report Dashboard")

### Confluent Version Scout (`.github/workflows/confluent-version-scout.yml`)
- Weekly cron (Monday 03:17 UTC) + manual trigger
- Checks Docker Hub for newer `cp-schema-registry` tags
- Opens a GitHub Issue if a newer version is found (dedup check prevents duplicates)

## Commands

```bash
# Start test infrastructure
podman-compose up -d

# Wait for both registries to be healthy (timeout in seconds)
./wait-for-ready.sh 180

# Run all tests and generate report
mvn clean test

# Run with explicit Confluent version (used by CI)
CONFLUENT_VERSION=$(grep -oP 'cp-schema-registry:\K[0-9]+\.[0-9]+\.[0-9]+' podman-compose.yml)
mvn clean test -Dconfluent.registry.version="$CONFLUENT_VERSION"

# Run specific test class
mvn test -Dtest=SubjectsEndpointTest

# View the generated report
xdg-open target/compatibility-report.html

# Tear down infrastructure
podman-compose down
```
