# Smart Campus Sensor & Room Management API

A JAX-RS RESTful API for managing rooms and IoT sensors across a university smart campus, built as part of the **5COSC022W Client-Server Architectures** coursework at the **University of Westminster**.

---

## Table of Contents

1. [API Overview](#api-overview)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [Build & Run Instructions](#build--run-instructions)
5. [API Endpoints](#api-endpoints)
6. [Sample cURL Commands](#sample-curl-commands)
7. [Report: Answers to Coursework Questions](#report-answers-to-coursework-questions)

---

## API Overview

The Smart Campus API provides a resource-oriented interface for managing:

- **Rooms** — Physical spaces on campus (lecture halls, labs, libraries).
- **Sensors** — IoT devices deployed within rooms (temperature, CO2, occupancy).
- **Sensor Readings** — Historical data points recorded by each sensor with timestamps.

The API follows RESTful principles including proper HTTP status codes, JSON representations, resource nesting via sub-resource locators, comprehensive error handling with custom exception mappers, and cross-cutting concerns via filters.

**Base URL:** `http://localhost:8080/smart-campus-api/api/v1`

---

## Technology Stack

| Component          | Technology                                   |
| ------------------ | -------------------------------------------- |
| Language           | Java 11                                      |
| API Framework      | JAX-RS 2.1 (Jersey 2.41)                     |
| JSON Serialisation | Jackson (via jersey-media-json-jackson)      |
| Build Tool         | Apache Maven                                 |
| Data Storage       | In-memory (`ConcurrentHashMap`, `ArrayList`) |
| Application Server | Apache Tomcat 10.x                           |
| Deployment         | WAR (Web Application Archive)                |

---

## Project Structure

```
smart-campus-api/
├── pom.xml                                    # Maven build configuration
├── README.md
└── src/
    └── main/
        ├── java/com/smartcampus/
        │   ├── SmartCampusApplication.java   # JAX-RS Application with @ApplicationPath
        │   ├── DataStore.java                # Thread-safe in-memory data storage
        │   ├── exception/
        │   │   ├── ErrorResponse.java           # Standardised error response DTO
        │   │   ├── LinkedResourceNotFoundException.java
        │   │   ├── RoomNotEmptyException.java
        │   │   ├── SensorUnavailableException.java
        │   │   ├── GlobalExceptionMapper.java      # Catch-all → 500
        │   │   ├── LinkedResourceNotFoundExceptionMapper.java  # → 422
        │   │   ├── RoomNotEmptyExceptionMapper.java        # → 409
        │   │   └── SensorUnavailableExceptionMapper.java   # → 403
        │   ├── filter/
        │   │   └── ApiLoggingFilter.java      # Request/Response logging filter
        │   ├── model/
        │   │   ├── Room.java
        │   │   ├── Sensor.java
        │   │   └── SensorReading.java
        │   └── resource/
        │       ├── DiscoveryResource.java     # GET /api/v1 (metadata)
        │       ├── RoomResource.java          # /api/v1/rooms (CRUD)
        │       ├── SensorResource.java        # /api/v1/sensors (CRUD + sub-resource locator)
        │       └── SensorReadingResource.java # Sub-resource for /readings
        └── webapp/
            └── WEB-INF/
                └── web.xml                      # Tomcat & Jersey servlet configuration
```

---

## Build & Run Instructions

### Prerequisites

- **Java JDK 11** or later (`java -version`)
- **Apache Maven 3.6+** (`mvn -version`)
- **Apache Tomcat 10.x** ([Download](https://tomcat.apache.org/download-10.cgi))

### Step 1: Navigate to Project Root

```bash
cd "C:\Users\kiths\Desktop\Year 2\Client Server Architechture\Coursework\smart-campus-api\smart-campus-api"
```

### Step 2: Build the WAR File

```bash
mvn clean package
```

This produces `target/smart-campus-api.war`.

### Step 3: Deploy to Tomcat

**Option A — Copy WAR to Tomcat:**

```bash
# Windows
copy target\smart-campus-api.war C:\path\to\Tomcat\webapps\

# Linux/macOS
cp target/smart-campus-api.war /path/to/tomcat/webapps/
```

**Option B — Use Maven Tomcat Plugin:**

```bash
mvn tomcat7:run
```

This starts an embedded Tomcat server on port 8080.

### Step 4: Start Tomcat (if using Option A)

```bash
# Windows
C:\path\to\Tomcat\bin\startup.bat

# Linux/macOS
/path/to/tomcat/bin/startup.sh
```

### Step 5: Verify the API

Open a browser or console and test the discovery endpoint:

```bash
curl http://localhost:8080/smart-campus-api/api/v1
```

You should receive a JSON response with API metadata.

---

## API Endpoints

| Method | Path                                  | Description                                        | Status Codes       |
| ------ | ------------------------------------- | -------------------------------------------------- | ------------------ |
| GET    | `/api/v1`                             | API discovery & metadata                           | 200                |
| GET    | `/api/v1/rooms`                       | List all rooms                                     | 200                |
| POST   | `/api/v1/rooms`                       | Create a new room                                  | 201, 400, 409      |
| GET    | `/api/v1/rooms/{roomId}`              | Get a specific room                                | 200, 404           |
| PUT    | `/api/v1/rooms/{roomId}`              | Update room metadata (name, capacity)              | 200, 404           |
| DELETE | `/api/v1/rooms/{roomId}`              | Delete a room (blocked if sensors present)         | 200, 404, 409      |
| GET    | `/api/v1/sensors`                     | List all sensors (optional `?type=` filter)        | 200                |
| POST   | `/api/v1/sensors`                     | Register a new sensor                              | 201, 400, 409, 422 |
| GET    | `/api/v1/sensors/{sensorId}`          | Get a specific sensor                              | 200, 404           |
| PUT    | `/api/v1/sensors/{sensorId}`          | Update sensor status/room (value update forbidden) | 200, 403, 404, 422 |
| DELETE | `/api/v1/sensors/{sensorId}`          | Delete a sensor and unlink from room               | 200, 404           |
| GET    | `/api/v1/sensors/{sensorId}/readings` | Get sensor reading history                         | 200, 404           |
| POST   | `/api/v1/sensors/{sensorId}/readings` | Post a new reading (updates parent sensor value)   | 201, 403, 404      |

---

## Sample cURL Commands

### 1. Discover the API

```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1
```

**Expected Response (200):**

```json
{
  "message": "Welcome to Smart Campus Sensor & Room Management API",
  "version": "1.0",
  "resources": {
    "rooms": "http://localhost:8080/smart-campus-api/api/v1/rooms",
    "sensors": "http://localhost:8080/smart-campus-api/api/v1/sensors"
  }
}
```

---

### 2. Get All Rooms

```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms
```

**Expected Response (200):**

```json
[
  {
    "id": "LIB-301",
    "name": "Library Quiet Study",
    "capacity": 50,
    "sensorIds": ["TEMP-001", "CO2-001"]
  },
  {
    "id": "LAB-101",
    "name": "Computer Science Lab",
    "capacity": 30,
    "sensorIds": ["OCC-001"]
  }
]
```

---

### 3. Create a Room

```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "LAB-205",
    "name": "AI Research Lab",
    "capacity": 25
  }'
```

**Expected Response (201 Created):**

```json
{
  "id": "LAB-205",
  "name": "AI Research Lab",
  "capacity": 25,
  "sensorIds": []
}
```

---

### 4. Update a Room

```bash
curl -X PUT http://localhost:8080/smart-campus-api/api/v1/rooms/LAB-205 \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 150
  }'
```

**Expected Response (200 OK):**

```json
{
  "id": "LAB-205",
  "name": "AI Research Lab",
  "capacity": 150,
  "sensorIds": []
}
```

---

### 5. Register a Sensor in That Room

```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEMP-002",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 0.0,
    "roomId": "LAB-205"
  }'
```

**Expected Response (201 Created):**

```json
{
  "id": "TEMP-002",
  "type": "Temperature",
  "status": "ACTIVE",
  "currentValue": 0.0,
  "roomId": "LAB-205"
}
```

---

### 6. Post a Reading to the Sensor

```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-002/readings \
  -H "Content-Type: application/json" \
  -d '{
    "value": 22.5
  }'
```

**Expected Response (201 Created):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1745098765000,
  "value": 22.5
}
```

---

### 7. Get All Sensors Filtered by Type

```bash
curl -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=Temperature"
```

**Expected Response (200):**

```json
[
  {
    "id": "TEMP-001",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 22.5,
    "roomId": "LIB-301"
  },
  {
    "id": "TEMP-002",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 22.5,
    "roomId": "LAB-205"
  }
]
```

---

### 8. Get Sensor Reading History

```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-002/readings
```

**Expected Response (200):**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1745098765000,
    "value": 22.5
  },
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "timestamp": 1745098850000,
    "value": 23.1
  }
]
```

---

### 9. Attempt to Delete a Room with Sensors (Triggers 409 Conflict)

```bash
curl -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LAB-205
```

**Expected Response (409 Conflict):**

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Room 'LAB-205' cannot be deleted because it still has sensors assigned.",
  "timestamp": 1745098765000
}
```

---

### 10. Post Reading to MAINTENANCE Sensor (Triggers 403 Forbidden)

```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 12.0}'
```

**Expected Response (403 Forbidden):**

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Sensor 'OCC-001' is currently in MAINTENANCE status and cannot accept readings.",
  "timestamp": 1745098765000
}
```

---

### 11. Create Sensor With Invalid roomId (Triggers 422 Unprocessable Entity)

```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "FAKE-001",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 0.0,
    "roomId": "DOESNOTEXIST"
  }'
```

**Expected Response (422 Unprocessable Entity):**

```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Room 'DOESNOTEXIST' does not exist. Cannot create sensor.",
  "timestamp": 1745098765000
}
```

---

## Report: Answers to Coursework Questions

### Part 1: Service Architecture & Setup

**Q1.1: Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? How does this impact managing in-memory data structures?**

By default, JAX-RS resource classes follow a **per-request lifecycle**. The JAX-RS runtime (Jersey, in this case) creates a brand-new instance of the resource class for every incoming HTTP request, processes the request, and then discards the instance once the response is sent. The resource class is **not** a singleton by default.

This architectural decision has profound implications for managing shared mutable state. If we stored our rooms, sensors, or readings as instance variables on the resource class, that state would be lost immediately after the response is sent and would not be visible to other concurrent requests from different clients. Each request would see an empty data structure.

To solve this problem, we use a **dedicated `DataStore` class with `static` fields** backed by `ConcurrentHashMap` and `ArrayList`. Because these structures are `static`, they exist at the class level and persist for the entire lifetime of the application, shared across all resource instances. Because they are thread-safe collections from `java.util.concurrent`, they provide atomic operations and built-in synchronisation, preventing race conditions when multiple requests attempt to read and write data simultaneously without requiring explicit `synchronized` blocks or locking mechanisms.

---

**Q1.2: Why is "Hypermedia" (HATEOAS) considered a hallmark of advanced RESTful design? How does it benefit client developers compared to static documentation?**

HATEOAS (Hypermedia As The Engine Of Application State) represents the highest maturity level of REST architecture (Level 3 in the Richardson Maturity Model). It means that API responses include navigational links that describe what actions are available next and where related resources can be found, enabling clients to discover the API dynamically.

Key benefits over static external documentation:

1. **Dynamic Discoverability:** Clients can navigate the API by following links in responses, much like browsing a website by clicking hyperlinks. No need to hardcode URL structures.
2. **Decoupling:** If the server changes its URL structure or endpoint paths, existing clients continue to work because they follow server-provided links rather than constructing URLs.
3. **Self-Documenting:** The API response itself describes available actions, reducing dependency on external documentation and lowering the barrier to entry for new developers.
4. **Reduced Errors:** Clients are less likely to construct invalid URLs because they follow links provided by the server.
5. **Evolution:** The server can add new related resources or actions without breaking client code that uses link-following strategies.

Our implementation includes a Discovery endpoint at `GET /api/v1` that returns a `resources` map, providing lightweight HATEOAS by directing clients to `/api/v1/rooms` and `/api/v1/sensors` endpoints.

---

### Part 2: Room Management

**Q2.1: When returning a list of rooms, what are the implications of returning only IDs versus full room objects?**

Returning **only IDs** minimises the response payload size, saving network bandwidth. However, it creates the **N+1 problem**: clients must make a separate HTTP request for each room to retrieve its full details. With 100 rooms, this requires 101 total requests (1 list request + 100 individual detail requests), resulting in significant cumulative latency and increased server load.

Returning **full room objects** increases the initial response payload size, but eliminates follow-up requests. Clients receive all needed information in a single round-trip. Given that modern networks handle reasonably-sized payloads efficiently, and considering the significant reduction in total network round-trips required, this approach is preferred for most use cases, especially dashboards and management interfaces where responsiveness is critical.

Our implementation returns **full room objects** to minimise client-side complexity and network latency.

---

**Q2.2: Is the DELETE operation idempotent in your implementation?**

Yes, DELETE is idempotent in the sense that the server-side state is identical after one or many identical calls:

- **First call:** The room exists, has no sensors attached, and is successfully deleted. Response: `200 OK`.
- **Second call:** The room no longer exists. Response: `404 Not Found`.
- **Subsequent calls:** Identical to the second call — `404 Not Found` every time.

Idempotency is about the server state, not the response status code. After the first successful DELETE, the room is gone permanently. No matter how many additional times the client sends the identical DELETE request, the server state remains unchanged — the room stays deleted. The different response codes (200 vs 404) are expected and do not violate idempotency semantics.

---

### Part 3: Sensor Operations & Linking

**Q3.1: Explain the technical consequences if a client sends data in a different format (e.g., text/plain or application/xml) when the method uses `@Consumes(MediaType.APPLICATION_JSON)`.**

When a JAX-RS method is annotated with `@Consumes(MediaType.APPLICATION_JSON)`, the runtime enforces **content type validation** via content negotiation. If a client sends a request with a `Content-Type` header of `text/plain`, `application/xml`, or any format other than `application/json`, JAX-RS will reject the request **before** the method body executes.

The server returns:

```
HTTP 415 Unsupported Media Type
```

HTTP 415 is the standard status code indicating that the server refuses to accept the request because the payload format is not supported by the target resource for the requested method. The developer does not need to write any custom validation code — JAX-RS handles this automatically at the framework level through its content negotiation mechanisms.

---

**Q3.2: Contrast `@QueryParam` filtering with an alternative path-based design (e.g., /sensors/type/CO2). Why is the query parameter approach superior?**

| Aspect                | `@QueryParam` (`?type=CO2`)                                                                               | Path-based (`/sensors/type/CO2`)                                                                    |
| --------------------- | --------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| **Optionality**       | Naturally optional — omitting the parameter returns all sensors                                           | Would require a separate endpoint to get the complete unfiltered list                               |
| **Composability**     | Multiple filters naturally combine: `?type=CO2&status=ACTIVE&roomId=LAB-205`                              | Each combination requires a distinct path, leading to combinatorial explosion of routes             |
| **Resource Identity** | The base URL `/sensors` always identifies the same collection resource; filters adjust the representation | Each path variation implies a semantically distinct resource, which is incorrect for filtered views |
| **HTTP Caching**      | Query strings are naturally treated as cache keys by standard HTTP infrastructure                         | Path-based variants pollute the resource namespace and complicate cache invalidation                |
| **REST Convention**   | Universally understood and adopted standard for searching, filtering, and pagination                      | Non-standard and deviates from REST conventions                                                     |
| **Client Usability**  | Intuitive for developers; query strings feel like "parameters"                                            | Unclear semantics; clients may misunderstand these as separate resources                            |

The query parameter approach is clearly superior because it:

- Preserves clean RESTful resource semantics
- Offers flexible, composable filtering
- Follows established conventions
- Simplifies APIs as they scale

---

### Part 4: Deep Nesting with Sub-Resources

**Q4.1: Discuss the architectural benefits of the Sub-Resource Locator pattern.**

The Sub-Resource Locator pattern (where a resource method returns a sub-resource instance for further method dispatching) offers substantial architectural benefits:

1. **Separation of Concerns:** Each resource class has a single, well-defined responsibility. `SensorResource` manages sensor CRUD operations, while `SensorReadingResource` exclusively handles reading history. Neither class needs to understand the internals of the other.

2. **Code Organisation & Readability:** Instead of a monolithic controller class with dozens of methods handling `/sensors`, `/sensors/{id}`, `/sensors/{id}/readings`, and `/sensors/{id}/readings/{rid}`, the logic is distributed across focused, single-purpose classes. Each class remains readable and maintainable.

3. **Reusability:** A sub-resource class can potentially be reused in different contexts. If another parent resource also manages similar nested readings or metrics, the same sub-resource class can be instantiated from a different locator method.

4. **Testability:** Smaller, focused classes are significantly easier to unit test. You can test `SensorReadingResource` in isolation by simply constructing it with a known sensor ID and verifying its methods without requiring the entire application to be running.

5. **Scalability:** As the API grows and more nested resources are added (e.g., `/sensors/{id}/alerts`, `/sensors/{id}/calibration`, `/sensors/{id}/maintenance`), each new concern gets its own focused class without inflating the parent resource.

6. **Parallel Development:** Different team members can develop different layers of the resource hierarchy independently with minimal coupling.

This pattern mirrors the physical hierarchy of the domain (Campus → Room → Sensor → Reading) and makes the codebase intuitive to navigate and extend.

---

### Part 5: Advanced Error Handling, Exception Mapping & Logging

**Q5.1: Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?**

HTTP 404 (Not Found) specifically means "the target resource identified by the request URI does not exist." If a client sends `POST /api/v1/sensors` with a JSON body containing an invalid `roomId`, returning 404 is misleading because the endpoint `/api/v1/sensors` **does exist**. The URL structure is correct.

HTTP 422 (Unprocessable Entity) is semantically precise for this scenario:

- The request **URL and method are valid** → not a 404 situation
- The **JSON payload syntax is well-formed** → not a 400 Bad Request (malformed JSON)
- The **semantic content is logically invalid** because the `roomId` field references a `Room` resource that does not exist in the system

HTTP 422 communicates exactly the right information: "I understood your request structure, but the data you provided is logically inconsistent with the current system state." This gives clients a clear, actionable signal to validate the values within their payload rather than questioning the endpoint structure itself.

Our implementation uses `LinkedResourceNotFoundException` and `LinkedResourceNotFoundExceptionMapper` to return HTTP 422 in these scenarios, providing clear semantic accuracy.

---

**Q5.2: From a cybersecurity standpoint, explain the risks of exposing internal Java stack traces to external API consumers.**

Exposing raw stack traces is classified under **CWE-209: Information Exposure Through an Error Message** and represents a significant security vulnerability:

1. **Technology Fingerprinting:** Class names (e.g., `org.glassfish.jersey`, `com.mysql.jdbc`) reveal exact frameworks, libraries, and versions. Attackers can search for known CVEs (Common Vulnerabilities and Exposures) specific to these versions.

2. **Internal Architecture Disclosure:** Package and class names (e.g., `com.smartcampus.datastore.DataStore`) expose the internal structure and naming conventions, enabling attackers to guess other endpoint paths, class names, and internal logic flow.

3. **File System Path Leakage:** Stack traces often include absolute file paths (e.g., `/opt/tomcat/webapps/smart-campus-api/...`), revealing the operating system, deployment structure, server directory layout, and potential entry points.

4. **Database Schema Exposure:** Errors originating from database operations may include SQL statements, table names, column names, and field types, enabling SQL injection crafting and database structure mapping.

5. **Business Logic Patterns:** Method names and call chains reveal internal execution flow, decision trees, and validation logic, helping attackers understand how to bypass security controls.

Our `GlobalExceptionMapper` mitigates all these risks by:

- Catching **any unhandled `Throwable`** before it reaches the client
- **Logging the full stack trace server-side only** where it cannot be intercepted
- **Returning only a generic, safe `ErrorResponse`** to the client with no technical details

---

**Q5.3: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging?**

JAX-RS filters (implementing `ContainerRequestFilter` and `ContainerResponseFilter`) are ideal for handling cross-cutting concerns like logging:

1. **Centralisation:** Logging logic is defined once in a single filter class rather than duplicated across every resource method and service class. This follows the **DRY (Don't Repeat Yourself)** principle.

2. **Guaranteed Coverage:** The filter is automatically applied to **every request and response**, including endpoints added to the system in the future. There is no risk of accidentally forgetting to add logging to a new endpoint.

3. **Separation of Concerns:** Resource methods focus purely on business logic. They are not polluted with infrastructure concerns like logging, making them cleaner, more readable, and easier to unit test.

4. **Consistency:** Every log entry follows the same format and structure, making log files significantly easier to parse, analyse, and correlate.

5. **Maintainability:** If the logging format, level, or destination changes, there is only one place to update the code. Changes automatically apply to all requests.

6. **Configuration Flexibility:** Cross-cutting concerns can be conditionally enabled, disabled, or adjusted at deployment time without code changes.

This is an application of **Aspect-Oriented Programming (AOP)**, where concerns orthogonal to business logic are separated and applied uniformly across the entire application. JAX-RS filters are the framework's clean, built-in mechanism for achieving this separation.

Our `ApiLoggingFilter` demonstrates this by logging all incoming requests and outgoing responses in a consistent format, providing complete visibility into API traffic without any business logic classes needing to handle logging.

---

## Demo Data

The API includes pre-seeded demo data in the `DataStore` static initialiser block:

**Rooms:**

- `LIB-301` — Library Quiet Study (capacity: 50)
- `LAB-101` — Computer Science Lab (capacity: 30)
- `HALL-A` — Main Lecture Hall (capacity: 200)

**Sensors:**

- `TEMP-001` — Temperature sensor in LIB-301 (ACTIVE)
- `CO2-001` — CO2 sensor in LIB-301 (ACTIVE)
- `OCC-001` — Occupancy sensor in LAB-101 (MAINTENANCE)
- `LIGHT-001` — Light sensor in HALL-A (ACTIVE)

---

## Author

- **Name:** Kithsara Medawewa
- **UOW ID:** w2119877
- **IIT ID:** 20240505
- **Module:** 5COSC022W Client-Server Architectures
- **University:** University of Westminster
- **Academic Year:** 2025/26
