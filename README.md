# Apicurio Registry - Confluent v8 API Compatibility Harness

Automated test harness that compares [Apicurio Registry](https://github.com/Apicurio/apicurio-registry)'s Confluent-compatible API against the official [Confluent Schema Registry](https://docs.confluent.io/platform/current/schema-registry/) API v8 (CP 7.x).

The harness runs the same REST Assured requests against both registries side-by-side, compares responses, and produces a standalone HTML compatibility report with a built-in triage workbench.

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- [Podman](https://podman.io/) + [podman-compose](https://github.com/containers/podman-compose)

### Run

```bash
# 1. Start infrastructure (Kafka, Confluent SR, Apicurio SR)
podman-compose up -d

# 2. Wait for both registries to be healthy
./wait-for-ready.sh 180

# 3. Run tests and generate report
mvn clean test

# 4. Open the report
xdg-open target/compatibility-report.html

# 5. Tear down when done
podman-compose down
```

## How It Works

### Dual-Target Testing

Every test sends the **same request** to both registries and compares the responses:

| Registry | Base URL |
|----------|----------|
| Confluent Schema Registry | `http://localhost:8081` |
| Apicurio Registry (v8 compat) | `http://localhost:8082/apis/ccompat/v8` |

Status codes, response bodies, and error codes are compared automatically.

### Test Coverage

53 tests across 6 endpoint groups:

| Group | Endpoints | Tests |
|-------|-----------|-------|
| Subjects | `/subjects`, `/subjects/{subject}/versions/*`, `/subjects/{subject}` | 23 |
| Schemas | `/schemas/ids/{id}/*` | 7 |
| Compatibility | `/compatibility/subjects/{subject}/versions/{version}` | 3 |
| Config | `/config`, `/config/{subject}` | 7 |
| Mode | `/mode` | 4 |
| Error Codes | Cross-cutting error code parity | 7 |

### Test Result States

| State | Meaning |
|-------|---------|
| `PASS` | Apicurio matches Confluent behavior exactly |
| `FAIL` | Apicurio response differs from Confluent |
| `SKIPPED_GAP` | Apicurio does not support this feature |
| `KNOWN_INCOMPATIBILITY` | Intentional deviation documented by Apicurio |
| `ERROR` | Test infrastructure issue (not an API gap) |

Behavioral differences are recorded as `FAIL` in the report without failing the build, so CI stays green while all incompatibilities are documented.

### HTML Report

The generated report (`target/compatibility-report.html`) is a standalone file with:

- **Summary dashboard** -- total/pass/fail counts, filterable by result
- **Detail drawer** -- click any test row to inspect Confluent vs Apicurio responses side-by-side
- **5 tabbed panels** -- Responses, Test Code, API Spec, Confluent Docs, Apicurio Impl
- **Triage workflow** -- mark tests with status, severity, and notes (persisted via localStorage)
- **Syntax highlighting** -- inline Java highlighting for test source code

## Infrastructure

Three containers via `podman-compose.yml`:

| Service | Image | Port |
|---------|-------|------|
| Kafka (KRaft) | `apache/kafka:3.9.0` | 9092 |
| Confluent Schema Registry | `confluentinc/cp-schema-registry:7.8.0` | 8081 |
| Apicurio Registry | `quay.io/apicurio/apicurio-registry:latest-snapshot` | 8082 |

Both registries share the same Kafka cluster but use separate topics. Kafka runs in KRaft mode (no Zookeeper).

## Project Structure

```
src/
  main/java/io/apicurio/registry/compatibility/
    config/          TestConfig -- base URLs, content types
    model/           CompatibilityTestResult enum, TestOutcome DTO
    collector/       TestResultCollector (thread-safe)
    report/          HtmlReportGenerator, ReportContextEnricher
  test/java/io/apicurio/registry/compatibility/
    shared/          AbstractCompatibilityTest, SchemaFixtures, CompatibilityReportExtension
    subjects/        SubjectsEndpointTest
    schemas/         SchemasEndpointTest
    compatibility/   CompatibilityEndpointTest
    config/          ConfigEndpointTest
    mode/            ModeEndpointTest
```

### Key Source Files

| File | Purpose |
|------|---------|
| `AbstractCompatibilityTest` | Base class with `assertCompatibility()`, `registerSchema()`, `recordOutcome()` |
| `CompatibilityReportExtension` | JUnit 5 lifecycle extension; triggers enrichment + HTML generation at shutdown |
| `ReportContextEnricher` | Adds test source code, OpenAPI context, doc links to each outcome |
| `HtmlReportGenerator` | Builds the standalone HTML report with drawer UI |
| `TestOutcome` | 16-field DTO carrying request/response data, enrichment metadata |
| `SchemaFixtures` | Avro schema constants for tests |

## CI/CD

### Compatibility Tests

[`.github/workflows/compatibility-tests.yml`](.github/workflows/compatibility-tests.yml) runs on every push/PR to `main`:

1. Starts podman containers, waits for health checks
2. Extracts Confluent version from `podman-compose.yml`, passes to Maven
3. Runs `mvn clean test -Dconfluent.registry.version=VERSION`
4. Uploads the HTML report as a workflow artifact
5. On push to `main`: posts a summary table as a comment on [Issue #1](../../issues/1) (Compatibility Test Report Dashboard)

### Confluent Version Scout

[`.github/workflows/confluent-version-scout.yml`](.github/workflows/confluent-version-scout.yml) runs weekly (Monday 03:17 UTC):

1. Queries Docker Hub for the latest `cp-schema-registry` tag
2. Compares against the version pinned in `podman-compose.yml`
3. Opens a GitHub Issue if a newer version is found (with upgrade instructions)

## Known Incompatibilities (CP 7.8.0)

The following behavioral differences between Confluent and Apicurio are tracked by the harness:

- Invalid schema syntax not validated in compatibility checks (200 vs 422)
- Invalid compatibility level returns 500 instead of 422
- Invalid mode returns 400 instead of 422
- POST version lookup returns 405 (endpoint not fully supported)
- `referencedBy` on nonexistent subject returns 200 (empty list) instead of 404
- Schema registration response may not include `version` field

## Scope (v1.0)

- **In scope**: Avro schemas only, all Confluent v8 API endpoints, both registries
- **Out of scope**: Protobuf, JSON Schema, Schema Linking/Exporters, KEKs/DEKs, Cluster Metadata

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| Framework | Quarkus (test scope) |
| Build | Maven |
| API Testing | REST Assured |
| Containers | Podman + podman-compose |
| Reporting | Custom HTML (standalone, no framework) |
| CI | GitHub Actions |

## License

This project follows the Apicurio Registry contribution guidelines: DCO sign-off required, squash-and-merge PRs.
