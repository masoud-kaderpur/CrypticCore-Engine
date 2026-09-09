# CrypticCore Engine

![Build](https://github.com/masoud-kaderpur/CrypticCore-Engine/actions/workflows/ci.yml/badge.svg)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![License: MIT](https://img.shields.io/badge/License-MIT-green)

**CrypticCore Engine** is a Java 21 streaming engine built to demonstrate **OpenTelemetry** tracing.

> [!WARNING]
> **Educational Showcase Only**  
> The stream transformation uses a repeating-key XOR cipher. It provides **no cryptographic confidentiality or integrity**.

---

## Demo
https://github.com/user-attachments/assets/94072560-118d-43fd-8377-e40e8d15c895

---

## Features

* Streams data via an 8 KB buffer to handle gigabyte files.
* Exports spans and events over OTLP directly to Jaeger, Dynatrace, or any collector.
* Reaches ~740 MB/s sustained throughput (validated via JMH).
* Writes to temporary file (`.tmp`) before moving to destination paths.

---

## Architecture

The project decouples stream processing, I/O handling, and telemetry setup so components remain testable in isolation.

```text
src/main/java/
├── core/
│   ├── domain/         # Core domain models, interfaces and records.
│   └── engine/         # Stream processing and tracer orchestration
├── infrastructure/
│   ├── io/             # File checks, magic byte parsing, and staged writing
│   └── telemetry/      # OpenTelemetry SDK and OTLP exporters
├── App.java            # CLI entry point
└── Executor.java       # Pipeline coordinator
```

---

## Transformation Logic

The engine uses a repeating-key XOR stream cipher. Because XOR is an involution, encryption and decryption share the same logic.

* **Operation:** `input_byte ^ key_byte`
* **Byte handling:** Masked with `0xFF` during bitwise operations to avoid Java's signed byte expansion during `int` promotion.

---

## Benchmarks

Stream throughput measured with JMH on Java 21.

| Buffer Size  | Throughput | Approx. Speed |
| :--- | :--- | :--- |
| 1 KB | ~728k ops/s | ~745 MB/s |
| **8 KB (Default)** | **~90k ops/s** | **~739 MB/s** |
| 64 KB  | ~12k ops/s | ~787 MB/s |

---

## File Format (`.cce`)

Encrypted files use a 4-byte header:

| Offset | Length | Description | Value |
| :--- | :--- | :--- | :--- |
| `0x00` | 3 Bytes | Magic Bytes | `CCE` (`0x43 0x43 0x45`) |
| `0x03` | 1 Byte | Version | `1` (`0x01`) |

---

## Build
```bash
mvn clean package
```

---

## Tracing Setup

The engine uses native OpenTelemetry (OTLP) to export spans, events, and file attributes directly to backends like Dynatrace or Jaeger.

### Dynatrace Tracing

Traces and execution events are sent over `http/protobuf`.

#### 1. Configuration

```bash
export OTEL_SERVICE_NAME=cryptic-core-engine
export OTEL_EXPORTER_OTLP_ENDPOINT=https://<TENANT_ID>.live.dynatrace.com/api/v2/otlp
export OTEL_EXPORTER_OTLP_HEADERS="Authorization=Api-Token <API_TOKEN>"
export OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
```

#### 2. Execution

```bash
java -jar target/CrypticCore-jar-with-dependencies.jar ENCRYPTION input.txt output.cce PASSWORD
```


#### Example 

![Dynatrace Tracing](/images/dynatrace_trace.jpg)

### Local Tracing

#### 1. Start Jaeger
```bash
docker compose up -d jaeger
```

#### 2. Configuration
```bash
export OTEL_SERVICE_NAME=cryptic-core
export OTEL_TRACES_EXPORTER=otlp
export OTEL_METRICS_EXPORTER=none
export OTEL_LOGS_EXPORTER=none
```

#### 3. Execution
```bash
java -jar target/CrypticCore-jar-with-dependencies.jar ENCRYPTION input.txt output.cce PASSWORD
```

#### 4. View Traces
Navigate to http://localhost:16686 to view traces.

---

## Testing and CI

* GitHub Actions runs builds, tests, and style checks on every push and PR.
* JaCoCo enforces >85% instruction coverage.
* Checked against Google Java Style (`maven-checkstyle-plugin`).
* JUnit 5 and AssertJ covering unit behavior, stream edge cases, and corrupted headers.
