# Java Thread Visualizer (PTV)

A real-time JVM thread visualization tool that attaches to **any running Java program** via the Java Debug Interface (JDI) — no code modification required. Captures live thread states, stack frames, local variables, and execution history at the source-line level, and streams everything to an interactive browser dashboard via WebSocket.

---

## Demo

> Connect PTV to your running Java program → open `localhost:8080` → watch every thread, every line, every variable update in real time.

---

## Features

- **Zero target modification** — attaches to any JVM via JDWP without touching the target program's source code
- **Real-time thread visualization** — circular process diagram showing all threads color-coded by state (Runnable, Blocked, Waiting, Timed Waiting, Terminated)
- **Line-level execution tracking** — captures exactly which source line each thread is executing using JDI step-event breakpoints
- **Local variable snapshots** — reads and displays local variable names, types, and values at each step
- **Execution history** — scrollable log of every state change and line transition with variable values at that moment
- **Execution speed control** — slider to slow down or speed up how fast the visualizer steps through your program
- **Pause / Resume / Step** — manually control target JVM execution from the browser
- **Resizable sidebar** — drag to resize, collapse/expand each panel independently
- **WebSocket live push** — data pushed to browser every step via STOMP over SockJS, falls back to REST polling if WebSocket fails
- **Docker support** — single container connects outward to any JVM on the host machine

---

## How It Works

```
Your Java Program                 PTV (Spring Boot)
─────────────────                 ─────────────────
java -agentlib:jdwp=...           Attaches via JDI
        │                                │
        │◄──── JDI Step Events ──────────┤
        │                                │
        │      (thread suspended)        │ reads stack frames
        │                                │ reads local variables
        │                                │ reads thread states
        │◄──── resume ───────────────────┤
                                         │
                                    WebSocket push
                                         │
                                    Browser Dashboard
```

PTV registers a **StepEvent listener** on every user-code thread. Each time a new source line is reached, the JVM fires an event, PTV reads the full thread state and variables while the thread is suspended, pushes the data to the browser, waits for the configured delay, then resumes execution.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 22, Spring Boot 4 |
| JVM Inspection | Java Debug Interface (JDI) via `tools.jar` / `jdk.jdi` |
| Real-time push | WebSocket, STOMP, SockJS |
| Frontend | Vanilla JS, HTML, CSS |
| Containerization | Docker |

---

## Getting Started

### Prerequisites

- Java 21+ installed on your machine
- Docker (optional, for containerized run)
- Your target Java program compiled with debug info (`-g` flag)

---

### Step 1 — Compile your target program with debug info

with Maven, add to `pom.xml`:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <compilerArgs><arg>-g</arg></compilerArgs>
  </configuration>
</plugin>
```

---

### Step 2 — Run your target program with JDWP enabled

```bash
-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005
```

The program runs normally — the only difference is the `-agentlib` flag which opens a debug port.

---

### Step 3 — Run PTV

**Option A — Docker (recommended)**

```bash
# Windows / Mac
docker run -p 8080:8080 -e JDI_HOST=host.docker.internal ptv

# Linux
docker run -p 8080:8080 --network=host ptv
```

**Option B — Run JAR directly**

```bash
java -jar PTV-0.0.1-SNAPSHOT.jar
```

---

### Step 4 — Open the dashboard

```
http://localhost:8080
```

You should see your threads appear on the circle diagram and execution history start filling in immediately.

---

## Configuration

Set these as environment variables or in `src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `jdi.host` | `host.docker.internal` | Host where target JVM is running |
| `jdi.port` | `5005` | JDWP debug port of target JVM |

**Examples:**

```bash
# Target on same machine (Docker on Windows/Mac)
docker run -p 8080:8080 -e JDI_HOST=host.docker.internal ptv

# Target on Linux host
docker run -p 8080:8080 --network=host -e JDI_HOST=localhost ptv

# Target on remote machine
docker run -p 8080:8080 -e JDI_HOST=192.168.1.50 -e JDI_PORT=5005 ptv
```

---

## Building from Source

```bash
# Clone the repo
git clone https://github.com/yourname/ptv.git
cd ptv

# Build the JAR
./mvnw clean package -DskipTests

# Build Docker image
docker build -t ptv .
```

---

## Project Structure

```
PTV/
├── src/main/java/processThreadVisualizer/PTV/
│   ├── PtvApplication.java          # Spring Boot entry point
│   ├── jdi/
│   │   └── JdiService.java          # JDI attach, step events, variable reading
│   ├── controller/
│   │   └── ThreadController.java    # REST + WebSocket endpoints
│   ├── model/
│   │   └── ThreadInfo.java          # Thread data model
│   └── config/
│       ├── WebSocketConfig.java     # STOMP WebSocket configuration
│       └── WebConfig.java           # CORS configuration
├── src/main/resources/static/
│   ├── index.html                   # Browser dashboard
│   └── style.css                    # Dashboard styles
├── Dockerfile
└── pom.xml
```

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/status` | Check if PTV is connected to target JVM |
| `GET` | `/api/threads` | One-time snapshot of all threads |
| `POST` | `/api/connect?host=&port=` | Manually connect to a target JVM |
| `POST` | `/api/suspend` | Pause target JVM execution |
| `POST` | `/api/resume` | Resume target JVM execution |
| `POST` | `/api/step` | Execute one step then pause |
| `POST` | `/api/speed?delayMs=` | Set delay between steps (0–2000ms) |

WebSocket subscription: `STOMP /topic/threads` — receives thread snapshots in real time.

---

## Thread States

| Color | State | Meaning |
|---|---|---|
| 🟢 Green | RUNNABLE | Actively executing |
| 🔴 Red | BLOCKED | Waiting to acquire a monitor lock |
| 🟣 Purple | WAITING | Waiting indefinitely (e.g. `Object.wait()`) |
| 🔵 Blue | TIMED_WAITING | Waiting with a timeout (e.g. `Thread.sleep()`) |
| ⚫ Grey | TERMINATED | Finished execution |

---

## Limitations

- Target program must be started with `-agentlib:jdwp` — this is the only requirement on the user's side
- Local variables are only visible if the class was compiled with debug info (`-g`)
- Variables that have been optimized away by the JVM may not appear
- Object field values are not currently shown (only stack-local variables)
- JDWP has no built-in authentication — do not expose port 5005 to the public internet

---

## Why I Built This

Most Java profiling tools are heavyweight IDEs or expensive commercial products. PTV is a lightweight, open dashboard you can point at any JVM and immediately understand what every thread is doing — useful for debugging concurrency issues, teaching threading concepts, or just exploring how a Java program executes line by line.

---

## License

MIT
