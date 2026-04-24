<<<<<<< HEAD
# Smart Campus Sensor & Room Management API

A RESTful API built with **JAX-RS (Jersey 2.41)** deployed on **Apache Tomcat 10** for the University of Westminster "Smart Campus" initiative.

---

## API Design Overview

The API follows a resource-oriented RESTful architecture with two primary entities — **Rooms** and **Sensors** — and a nested sub-resource for **SensorReadings**.

```
GET  /api/v1                              → Discovery / metadata
GET  /api/v1/rooms                        → List all rooms
POST /api/v1/rooms                        → Create a room
GET  /api/v1/rooms/{roomId}               → Get a specific room
DEL  /api/v1/rooms/{roomId}               → Delete a room (blocked if sensors present)

GET  /api/v1/sensors                      → List all sensors (supports ?type= filter)
POST /api/v1/sensors                      → Register a sensor (validates roomId)
GET  /api/v1/sensors/{sensorId}           → Get a specific sensor
DEL  /api/v1/sensors/{sensorId}           → Remove a sensor

GET  /api/v1/sensors/{sensorId}/readings  → Get reading history
POST /api/v1/sensors/{sensorId}/readings  → Append a new reading
```

All data is stored in-memory using `ConcurrentHashMap` and `ArrayList`. No database is used.

**Technology Stack:** Java 11 · JAX-RS 2.1 · Jersey 2.41 · Apache Tomcat 10 · Jackson JSON · Maven (WAR packaging)

---

## Build & Run Instructions

### Prerequisites
- Java 11 or higher (`java -version`)
- Apache Maven 3.6+ (`mvn -version`)
- Apache Tomcat 10.x ([Download](https://tomcat.apache.org/download-10.cgi))

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/<your-username>/smart-campus-api.git
cd smart-campus-api

# 2. Build the WAR file
mvn clean package

# The WAR is created at: target/smart-campus-api.war
```

### Deploy to Tomcat

**Option A — Copy to webapps (recommended):**
```bash
cp target/smart-campus-api.war /path/to/tomcat/webapps/
# Start Tomcat
/path/to/tomcat/bin/startup.sh        # macOS / Linux
/path/to/tomcat/bin/startup.bat       # Windows
```

**Option B — Tomcat Manager UI:**
1. Open `http://localhost:8080/manager/html`
2. Scroll to "Deploy" → "WAR file to deploy"
3. Choose `target/smart-campus-api.war` → click Deploy

The API is now live at:
```
http://localhost:8080/smart-campus-api/api/v1/
```

### Stop Tomcat
```bash
/path/to/tomcat/bin/shutdown.sh    # macOS / Linux
/path/to/tomcat/bin/shutdown.bat   # Windows
```

---

## Sample curl Commands

> Replace `http://localhost:8080/smart-campus-api` with your deployed URL if different.

### 1. Discovery Endpoint
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/
```

### 2. Get All Rooms
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms
```

### 3. Create a New Room
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LAB-205","name":"AI Research Lab","capacity":25}'
```

### 4. Get All CO2 Sensors (filtered)
```bash
curl -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=CO2"
```

### 5. Create a New Sensor
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-002","type":"CO2","status":"ACTIVE","currentValue":0.0,"roomId":"LAB-205"}'
```

### 6. Post a Sensor Reading
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/CO2-002/readings \
  -H "Content-Type: application/json" \
  -d '{"value":450.5}'
```

### 7. Get Sensor Reading History
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/sensors/CO2-002/readings
```

### 8. Attempt to Delete a Room With Sensors → 409 Conflict
```bash
curl -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LIB-301
```

### 9. Post Reading to MAINTENANCE Sensor → 403 Forbidden
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":12.0}'
```

### 10. Create Sensor With Invalid roomId → 422 Unprocessable Entity
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"FAKE-001","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"DOESNOTEXIST"}'
```

---

## Project Structure

```
smart-campus-api/
├── pom.xml                                         # Maven WAR build config
└── src/main/
    ├── webapp/WEB-INF/web.xml                      # Tomcat / Jersey servlet config
    └── java/com/smartcampus/
        ├── SmartCampusApplication.java             # @ApplicationPath("/api/v1")
        ├── DataStore.java                          # Thread-safe in-memory storage
        ├── model/
        │   ├── Room.java
        │   ├── Sensor.java
        │   └── SensorReading.java
        ├── resource/
        │   ├── DiscoveryResource.java              # GET /api/v1
        │   ├── RoomResource.java                   # /api/v1/rooms
        │   ├── SensorResource.java                 # /api/v1/sensors
        │   └── SensorReadingResource.java          # sub-resource: /readings
        ├── exception/
        │   ├── ErrorResponse.java
        │   ├── RoomNotEmptyException.java          + Mapper → 409
        │   ├── LinkedResourceNotFoundException.java + Mapper → 422
        │   ├── SensorUnavailableException.java     + Mapper → 403
        │   └── GlobalExceptionMapper.java          → 500
        └── filter/
            └── ApiLoggingFilter.java               # Request + Response logging
```

---

## Conceptual Report — Question Answers

### Part 1.1 — JAX-RS Resource Class Lifecycle

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (per-request lifecycle). This is mandated by the JAX-RS specification and is the default behaviour in Jersey unless a class is explicitly annotated with `@Singleton`.

**Impact on in-memory data management:**

Because a fresh resource instance is constructed per request, any instance variables declared inside the resource class are discarded at the end of that request. Storing a `HashMap<String, Room>` as an instance variable would mean every request starts with an empty map — all data lost.

To prevent this, all mutable state is externalised into a dedicated `DataStore` class using **`static` fields backed by `ConcurrentHashMap`**. The reasons for this choice:

- `ConcurrentHashMap` supports **fine-grained segment-level locking**, allowing concurrent reads/writes without blocking the entire map — critical when Tomcat handles multiple simultaneous requests on separate threads.
- Operations like `put`, `get`, `remove`, and `computeIfAbsent` are **atomic**, preventing race conditions where two threads could read-modify-write the same entry simultaneously.
- A regular `HashMap` is **not thread-safe**: concurrent writes can corrupt its internal bucket structure, causing data loss, infinite loops, or `ConcurrentModificationException`.

An alternative would be annotating the resource class with `@Singleton`, but this requires explicit `synchronized` blocks or `ReentrantReadWriteLock` on every data access. The static `ConcurrentHashMap` approach is cleaner, more idiomatic, and leverages Java's built-in concurrent primitives.

---

### Part 1.2 — Why HATEOAS is a Hallmark of Advanced REST Design

**HATEOAS** (Hypermedia As The Engine Of Application State) means the API includes navigable links in its responses, making it self-describing at runtime rather than relying on external documentation.

Example — our discovery response includes:
```json
{ "links": { "rooms": "/api/v1/rooms", "sensors": "/api/v1/sensors" } }
```

**Benefits over static documentation:**

1. **Discoverability** — A client can start at `/api/v1` and navigate the entire API by following links, without reading a single documentation page.
2. **Resilience to URL changes** — If a URL structure changes server-side, clients following embedded links adapt automatically. Clients with hard-coded URLs break.
3. **Reduced coupling** — Clients depend only on link relation names ("rooms", "delete"), not on specific URL patterns.
4. **Workflow guardrails** — The server can omit links that are not currently valid (e.g., no "delete" link when deletion is blocked), guiding clients away from invalid operations before they attempt them.
5. **Self-documentation** — The API surface is discoverable programmatically, enabling tools like API browsers to navigate it without pre-configuration.

---

### Part 2.1 — Full Objects vs. IDs in List Responses

| Approach | Pros | Cons |
|---|---|---|
| Return full objects | All data available immediately; no follow-up requests | Larger payload; slower for very large collections |
| Return only IDs | Minimal payload; faster initial response | Client must make N additional GET requests (N+1 problem) |

**Returning full room objects** is the correct default for this API because rooms are small POJOs and clients almost always need the full data (name, capacity, sensorIds) to render a useful UI. Returning only IDs forces the client into the **N+1 anti-pattern**: receive 100 IDs, make 100 individual GET requests, each with its own HTTP overhead (TCP round-trip, headers, latency), which is dramatically slower than one larger payload.

Returning only IDs is appropriate when: (a) the collection is extremely large (millions of records), (b) each object contains large binary fields, or (c) the API implements cursor-based pagination where clients only need the next page.

---

### Part 2.2 — Is DELETE Idempotent?

**Yes, DELETE is idempotent** in this implementation and by HTTP specification (RFC 9110).

Idempotency means **multiple identical requests leave the server in the same state** as a single request. It does NOT require that every response be identical.

In our implementation:
- **First DELETE** on an existing room → removes it → returns `200 OK`.
- **Second DELETE** on the same room → room already gone → returns `404 Not Found`.

The server state is **identical after both calls**: the room does not exist. The response code differs (200 vs 404), but the *resource state effect* is the same — this satisfies idempotency.

This property is important for network reliability: if a client sends a DELETE and the connection drops before receiving a response, it can safely retry. The second call confirms deletion without accidentally affecting other resources or creating duplicate side effects.

---

### Part 3.1 — Effect of `@Consumes(APPLICATION_JSON)` Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that a method only accepts request bodies with `Content-Type: application/json`.

If a client sends a different content type:

- **`Content-Type: text/plain`** → JAX-RS returns **HTTP 415 Unsupported Media Type** before the method body is ever executed. No Java code inside the method runs.
- **`Content-Type: application/xml`** → Same: **HTTP 415**. JAX-RS performs content negotiation at the framework level.
- **Missing `Content-Type` header** → Jersey typically returns 415, as it cannot identify the format.

This is enforced entirely by the JAX-RS runtime as part of its request-matching algorithm. A resource method is only invoked if the request's `Content-Type` matches a declared `@Consumes` value. This acts as a first line of defence, ensuring the Jackson JSON deserialiser never receives data it cannot parse (preventing obscure `JsonParseException` stack traces leaking to the client).

---

### Part 3.2 — `@QueryParam` vs. Path Segment for Filtering

**`@QueryParam` approach** (`GET /api/v1/sensors?type=CO2`) is superior because:

1. **Semantic correctness** — URL path segments identify a resource by its *identity*. `/sensors/CO2-001` means "the sensor whose ID is CO2-001". `/sensors/type/CO2` incorrectly implies "CO2" is a resource identifier, not a filter criterion.
2. **Optionality** — Query parameters are inherently optional. `GET /sensors` (no filter) returns all sensors naturally. With a path-based filter you need a separate route or awkward design like `/sensors/type/all`.
3. **Composability** — Multiple filters combine naturally: `?type=CO2&status=ACTIVE&roomId=LIB-301`. Path-based filters cannot compose without creating a combinatorial explosion of routes.
4. **HTTP caching** — Caches and CDNs understand that query parameters represent filtered views of the same underlying collection (`/sensors`), enabling more effective cache key strategies.
5. **REST convention** — The REST community standard is: path = resource identity, query string = filtering, sorting, pagination.

---

### Part 4.1 — Benefits of the Sub-Resource Locator Pattern

The Sub-Resource Locator uses a method **without an HTTP verb annotation** that returns a plain Java object. JAX-RS then dispatches the actual HTTP method (GET, POST, etc.) to that returned object, effectively delegating the sub-path to a separate class.

**Architectural benefits:**

1. **Single Responsibility Principle** — `SensorResource` manages sensor CRUD. `SensorReadingResource` manages reading history. Each class has one clearly defined job. Combining both in one controller would produce a class with dozens of methods, making it difficult to navigate and maintain.
2. **Scalability** — A production campus API might add: `/sensors/{id}/alerts`, `/sensors/{id}/calibrations`, `/sensors/{id}/firmware`. With sub-resource locators, each gets its own focused class. In a monolithic controller, every new sub-path adds more methods to an already large file.
3. **Independent testability** — `SensorReadingResource` can be unit-tested by constructing it with a test `sensorId`, without involving `SensorResource` or the full JAX-RS routing stack.
4. **Context injection** — The locator method receives `@PathParam("sensorId")` and passes it into the constructor, giving the sub-resource clean, scoped context without relying on global state or thread-locals.
5. **Separation of concerns** — Each class is independently deployable in theory, and changes to reading logic cannot accidentally break sensor CRUD logic.

---

### Part 5.2 — Why HTTP 422 Instead of 404?

When a client POSTs a new sensor with a `roomId` that does not exist:

- **404 Not Found** semantically means: "the *URL you requested* does not exist on the server." It refers to the *endpoint URL itself* (`POST /api/v1/sensors`) — but that URL is perfectly valid and does exist.
- **422 Unprocessable Entity** means: "the server understood the request's format and successfully parsed the JSON body, but the *semantic content* of the payload is invalid — a referenced entity within the payload does not exist."

The key distinction:
- **404** → problem with the *request URL*
- **400** → problem with the *request body format* (malformed JSON, missing required field)
- **422** → problem with the *request body semantics* (well-formed JSON, but content refers to a non-existent resource)

In our case, `POST /api/v1/sensors` is a valid endpoint (no 404), and the JSON is syntactically valid (no 400). The problem is specifically that `"roomId": "NONEXISTENT"` references a room that does not exist — a semantic validation failure. HTTP 422 communicates this precisely, giving client developers unambiguous signal that they need to check their payload's referenced IDs, not their URL structure.

---

### Part 5.4 — Security Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers is a serious security vulnerability:

1. **Technology fingerprinting** — Stack traces reveal the exact framework (`jersey`, `grizzly`), Java version, and library versions with their full package paths. Attackers cross-reference these against public CVE databases to identify known, unpatched vulnerabilities in those exact versions.
2. **Internal architecture exposure** — Package and class names (e.g., `com.smartcampus.resource.SensorResource.createSensor`) reveal the application's internal structure, making it trivial for an attacker to reason about the codebase and identify potential attack surfaces or injection points.
3. **File system path disclosure** — Some exceptions include absolute server paths (e.g., `/home/app/smart-campus-api/src/...`), revealing the OS, directory layout, deployment user, and potentially sensitive configuration file locations.
4. **Business logic leakage** — Method call chains reveal exactly how the application processes data, which attackers exploit to craft targeted payloads that trigger specific code paths.
5. **Error-based probing** — If an attacker can reliably trigger a `NullPointerException` by supplying specific inputs, the stack trace tells them which field caused it, enabling systematic blind injection attacks.

The `GlobalExceptionMapper` mitigates all of these by returning only a generic `"An unexpected error occurred"` message to the client, while logging the full stack trace **server-side only** — visible only to authorised system administrators via Tomcat's logs.

---

### Part 5.5 — JAX-RS Filters vs. Manual Logging Calls

Using a JAX-RS filter (`ContainerRequestFilter` / `ContainerResponseFilter`) is superior to placing `Logger.info()` calls inside every resource method because:

1. **DRY Principle** — With manual logging, every resource method must remember to log. Forgetting one creates gaps in observability. A filter applies automatically to *all* registered endpoints with zero per-method code.
2. **Separation of concerns** — Resource methods should express business logic only. Mixing logging into business code reduces readability, testability, and clarity of intent.
3. **Centralised control** — Need to change the log format, add a correlation/request ID, or route logs to a different system? Change one class, not dozens of resource methods.
4. **Consistency** — Filters guarantee a uniform log structure across all endpoints. Manual calls rely on developer discipline, which degrades over time as new endpoints are added.
5. **Request lifecycle access** — Filters intercept requests *before* the method runs and responses *after* it completes. This enables latency measurement (response time − request time) — impossible to do cleanly from inside a method body.
6. **Cross-cutting concern design** — Logging, authentication, CORS, and rate-limiting are all cross-cutting concerns. JAX-RS filters are the idiomatic mechanism for these, keeping business code clean and focused.
=======
# SmartCampus-API
SmartCampus - Sensor &amp; Room Management JAX-RS API
>>>>>>> 30b43a8458a533e5c0374b6d4b90d84aa5aea480
