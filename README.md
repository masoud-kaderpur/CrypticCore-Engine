# CrypticCore Engine

![Build Status](https://github.com/masoud-kaderpur/CrypticCore-Engine/actions/workflows/ci.yml/badge.svg)
![Java Version](https://img.shields.io/badge/Java-21-blue)
![License](https://img.shields.io/badge/License-MIT-green)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED)

CrypticCore is a high-performance Java-based encryption engine designed for memory-efficient file
transformation. It implements a decoupled architecture that separates the cryptographic logic
from the data streaming process, enriched with production-grade, vendor-neutral telemetry.

---

## 1. Theoretical Foundation

### 1.1 The Transformation (XOR Logic)

The engine utilizes the bitwise **Exclusive OR (XOR)** operation. Given that XOR is an involution,
the transformation is self-inverse, allowing for identical encryption and decryption logic.

The operation is defined as:

$$P \oplus K = C$$
$$C \oplus K = P$$

### 1.2 Key Streaming (Modular Arithmetic)

To handle data streams where the length of the plaintext exceeds the key length, a cyclic key
schedule is implemented:

$$i_{key} = i_{file} \pmod{L_{key}}$$

---

## 2. Implementation Details

### 2.1 Java Type Handling (Sign Extension Mitigation)

To prevent unintended sign extension during the implicit promotion from `byte` to 32-bit `int`, a
bitmask of $0xFF$ is
applied to maintain 8-bit integrity:

$$Result = (P \land 0xFF) \oplus (K \land 0xFF)$$

### 2.2 Memory Efficiency & Performance

* **O(1) Space Complexity**: Processes data in discrete **8 KB buffers**, allowing files of
  arbitrary size (tested up to
  5 GB) to be processed with minimal RAM footprint.
* **Throughput**: Optimized for high-speed I/O, achieving over **400 MB/s** on standard hardware.
* **Real-time Telemetry**: Integrated progress bar, vendor-neutral Micrometer instrumentation, and performance statistics (throughput, processing latency).

### 2.3 Robustness & Safety

* **Atomic Writes**: Utilizes a `.tmp` file staging strategy. The final output is only created via
  an atomic `move`
  operation upon successful completion, preventing data corruption during crashes or power failures.
* **Memory Sanitation**: The encryption key is explicitly overwritten in the JVM heap using
  `Arrays.fill()` immediately
  after use to mitigate memory dump exploits.
* **Header Validation**: Strict magic number and version checking prevents the processing of
  incompatible or corrupted
  files.

### 2.4 SOLID Architecture & Decoupling

The engine is built on SOLID principles to ensure extensibility and testability:

* **Single Responsibility (SRP):** I/O handling, header validation, metric recording, and cryptographic logic are
  strictly separated into specialized components (`HeaderHandler`, `FileValidator`, `EncryptionEngine`).
* **Dependency Inversion (DIP):** The engine does not depend on a specific UI or monitoring platform. It communicates progress through a `ProgressObserver` interface and accepts a vendor-neutral Micrometer `MeterRegistry` via constructor injection, rendering it compatible with Prometheus, Dynatrace, or Datadog.
* **Interface Segregation:** Cryptographic strategies are injected via the `CipherAlgorithm`
  interface, making the engine open for future algorithms (e.g., AES) without modifying the core streaming
  logic.

---

## 3. Cloud-Native & Containerization

The engine is fully containerized to ensure environment parity and security.

* **Multi-Stage Docker Build:** Uses a builder stage (Maven) and a hardened runtime stage (JRE
  Alpine) to minimize image size (~160MB) and attack surface.
* **Security Hardening:** The container execution is restricted to a **Non-Root User**.
* **Orchestration Stack:** Includes a unified `docker-compose.yml` that provisions the engine along with a containerized time-series database (Prometheus) and visualization node (Grafana) in a shared network topology.

---

## 4. File Format Specification

Every encrypted file starts with a 4-byte metadata header.

| Offset | Length  | Description        | Value (Hex / ASCII) |
|:-------|:--------|:-------------------|:--------------------|
| 0x00   | 3 Bytes | Magic Number (CCE) | `0x43 0x43 0x45`    |
| 0x03   | 1 Byte  | Format Version     | `0x01`              |

---

## 5. Usage

### 5.1 Native Execution

```bash
java -jar target/CrypticCore-jar-with-dependencies.jar <mode> <input> <output> <key>
```

### 5.2 Docker Execution

```bash
docker compose run --rm engine <mode> /app/data/input.txt /app/data/output.enc <key>
```

**Parameters:**

* **mode:** `ENCRYPTION` or `DECRYPTION` (Case-insensitive).
* **input:** Path to the source file.
* **output:** Final destination path for the transformed file.
* **key:** Secret key for transformation.

## 6. Quality Assurance

The project follows a rigorous testing strategy to ensure data integrity and system stability:

### 6.1 Automated Quality Gate (CI/CD) & Observability

The project utilizes **GitHub Actions** for continuous integration. Every push and pull request is
automatically validated against:

* **Compilation & Test Suite:** Ensures 100% build stability on Java 21.
* **Checkstyle (Google Java Style):** Strict enforcement of
  the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
* **Test Coverage (JaCoCo):** A quality gate is set to ensure a minimum of **85% code coverage**.
* **Hybrid Structured Logging:** Implements environment-aware logging. The engine automatically
  detects its environment and switches to Structured JSON Logging when running in Docker.
* **Telemetric Exporters:** Exposes real-time runtime counters and timing metrics formatted natively for Prometheus scraping via an embedded low-overhead HTTP daemon.

### 6.2 Testing Strategy

* **Unit Testing:** Verified the involution property and edge cases (byte boundaries).
* **Integration Testing:** End-to-end validation of the custom `.cce` format and header integrity.
* **Resilience:** Validation of atomic write operations and prevention of data truncation.
* **End-to-End Cycle:** Successful encryption and decryption of real file streams.
* **Atomic Integrity:** Verification of the `.tmp` staging and atomic move strategy.
* **Error Resilience:** * Detection of truncated files (Expected vs. Actual size check).
    * Prevention of in-place corruption (Same-file validation).
    * Robust header and version validation.

## 7. Project Architecture

The engine is structured into specialized packages to ensure high maintainability and separation of
concerns:

* **`at.tuwien.crypticcore.api`**: Functional interfaces and core contracts for algorithm
  strategies.
* **`at.tuwien.crypticcore.engine`**: The orchestration layer. Stateless execution of streaming
  cryptography.
* **`at.tuwien.crypticcore.io`**: Infrastructure layer handling format-specific headers (
  `HeaderHandler`), fail-fast validation (`FileValidator`), and the `ProgressObserver` pattern.

## 8. Telemetry & Grafana Pipeline Step-by-Step

Follow these steps to spin up the metrics pipeline, process a file, and visualize the data within Grafana.

### 8.1 Verify the Configuration Files

Ensure your project root contains the following configuration structures:

prometheus.yml
```yaml
global:
  scrape_interval: 2s

scrape_configs:
  - job_name: 'crypticcore-engine'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

docker-compose.yml
```yaml
services:
  engine:
    build: .
    image: cryptic-core-engine:latest
    volumes:
      - .:/app/data
    extra_hosts:
      - "host.docker.internal:host-gateway"

  prometheus:
    image: prom/prometheus:v2.53.0
    container_name: cc-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    extra_hosts:
      - "host.docker.internal:host-gateway"

  grafana:
    image: grafana/grafana:11.0.0
    container_name: cc-grafana
    ports:
      - "3001:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
```

### 8.2 Spin up the Infrastructure

Boot the integrated monitoring infrastructure using Docker Compose:

```bash
docker compose down
docker compose up -d
```

### 8.3 Generate Local Data & Run the Engine

Generate a large test file (e.g., 100 MB or 1 GB) to benchmark throughput and latency.

Linux/macOS:
```bash
dd if=/dev/urandom of=input.txt bs=10m count=100
```

Windows(PowerShell):
```bash
$fs = New-Object System.IO.FileStream("large_input.txt", [System.IO.FileMode]::Create); $fs.SetLength(100MB); $fs.Close()
```

Compile and run the binary application:

```bash
mvn clean package
java -jar target/CrypticCore-jar-with-dependencies.jar ENCRYPTION input.txt output.enc secretkey
```

Note: The application will log that the telemetry server is live on port 8080 and hold execution until manually terminated via Ctrl+C.

### 8.4 Wire and Build the Dashboard

1. Open Grafana at http://localhost:3001 (Credentials: admin / admin).
2. Navigate to Connections → Data sources → Add data source, and select Prometheus.
3. Set the connection URL to: http://prometheus:9090 and click Save & test.
4. Create a new dashboard panel, set the data source to your Prometheus entry, and configure the visualization with these PromQL formulas:

Accumulated Volume Panel:

```bash
crypticcore_bytes_processed_total / 1024 / 1024
```

(Unit configuration: Data → Megabytes (MB), style as a plateau or counter step graph)

Restart-Aware Throughput Spike Panel:

```bash
increase(crypticcore_bytes_processed_total[1m]) / 1024 / 1024
```



