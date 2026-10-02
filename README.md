# Smart Queue API

A simple REST API for managing a customer queue in a small service center.

The application allows customers to join the queue, view waiting customers, check their current position, call the next customer, and remove customers from the queue.

The project was developed as part of the **NV ProjectLab Backend Interview Task**.

---

## Features

* Add a new customer to the queue
* View all waiting customers
* Check a customer's current queue position
* Call the next customer
* Remove a customer from the queue
* FIFO (First In, First Out) queue ordering
* Customer status management
* Request validation
* Consistent error responses
* Database persistence with MySQL
* Concurrent `/next` request protection
* Automated controller tests

---

## Tech Stack

* **Java 25**
* **Spring Boot 4.1.1**
* **Spring Web MVC**
* **Spring Data JPA**
* **Hibernate**
* **MySQL**
* **Maven**
* **JUnit 5**
* **MockMvc**

---

## Project Structure

```text
src/main/java/com/example/smartqueueapi
├── controller
│   └── QueueCustomerController.java
├── service
│   └── QueueCustomerService.java
├── repository
│   └── QueueCustomerRepository.java
├── dto
│   ├── QueueCustomerRequest.java
│   ├── QueueCustomerResponse.java
│   └── ErrorResponse.java
├── exception
│   └── GlobalExceptionHandler.java
└── entity
    ├── QueueCustomer.java
    └── QueueStatus.java
```

The application follows a simple layered architecture:

**Controller → Service → Repository → Database**

* **Controller** handles HTTP requests and responses.
* **Service** contains the queue business logic.
* **Repository** communicates with the database using Spring Data JPA.
* **Entity** represents database data.
* **DTOs** are used for API requests and responses.
* **Exception Handler** provides consistent error responses.

---

## Data Model

Each customer contains the following information:

| Field       | Type          | Description                             |
| ----------- | ------------- | --------------------------------------- |
| `id`        | Long          | Unique customer identifier              |
| `name`      | String        | Customer name                           |
| `createdAt` | LocalDateTime | Time when the customer joined the queue |
| `status`    | QueueStatus   | Current customer status                 |

### Customer Statuses

```text
WAITING
SERVING
COMPLETED
```

`position` is calculated dynamically for waiting customers and is not stored in the database.

---

## Queue Logic

The queue follows **FIFO (First In, First Out)** ordering.

Waiting customers are ordered by:

```text
createdAt ASC
id ASC
```

The `id` is used as a secondary ordering field to keep the order deterministic when two customers have the same `createdAt` value.

For example:

```text
Customer A → created first
Customer B → created second
Customer C → created third
```

Calling `/api/queue/next` will select:

```text
Customer A
```

After Customer A starts being served:

```text
Customer A → SERVING
Customer B → WAITING
Customer C → WAITING
```

The queue position is calculated from the current `WAITING` customers, so positions automatically change when customers are removed or served.

---

## API Endpoints

### 1. Add Customer

**POST** `/api/queue`

Adds a new customer to the queue.

#### Request

```json
{
  "name": "Ali"
}
```

#### Response

**201 Created**

```json
{
  "id": 1,
  "name": "Ali",
  "createdAt": "2026-10-02T10:30:00",
  "status": "WAITING",
  "position": 1
}
```

A new customer automatically receives:

```text
status = WAITING
createdAt = current time
```

---

### 2. Get Waiting Customers

**GET** `/api/queue`

Returns all customers currently waiting in the queue.

#### Response

**200 OK**

```json
[
  {
    "id": 1,
    "name": "Ali",
    "createdAt": "2026-10-02T10:30:00",
    "status": "WAITING",
    "position": 1
  },
  {
    "id": 2,
    "name": "Leyla",
    "createdAt": "2026-10-02T10:31:00",
    "status": "WAITING",
    "position": 2
  }
]
```

Only customers with `WAITING` status are returned.

---

### 3. Get Customer by ID

**GET** `/api/queue/{id}`

Returns a customer's information and current queue position.

#### Example

```text
GET /api/queue/1
```

#### Response

**200 OK**

```json
{
  "id": 1,
  "name": "Ali",
  "createdAt": "2026-10-02T10:30:00",
  "status": "WAITING",
  "position": 1
}
```

For customers who are no longer waiting, `position` is returned as `null`.

#### Customer Not Found

**404 Not Found**

```json
{
  "timestamp": "2026-10-02T10:35:00",
  "status": 404,
  "message": "Customer not found",
  "path": "/api/queue/999"
}
```

---

### 4. Call Next Customer

**POST** `/api/queue/next`

Selects the first waiting customer and changes their status from:

```text
WAITING → SERVING
```

#### Response

**200 OK**

```json
{
  "id": 1,
  "name": "Ali",
  "createdAt": "2026-10-02T10:30:00",
  "status": "SERVING",
  "position": null
}
```

If there are no waiting customers:

**404 Not Found**

```json
{
  "timestamp": "2026-10-02T10:40:00",
  "status": 404,
  "message": "No waiting customers",
  "path": "/api/queue/next"
}
```

---

## Concurrency Handling

One of the requirements of the task is to prevent the same customer from being called twice if two requests to `/next` arrive at the same time.

The application handles this using a **database pessimistic write lock** together with a **transaction**.

The repository uses:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<QueueCustomer> findFirstByStatusOrderByCreatedAtAscIdAsc(
        QueueStatus status);
```

The service method is transactional:

```java
@Transactional
public QueueCustomerResponse nextCustomer() {
    ...
}
```

### How it works

When the first `/next` request selects a waiting customer, the database places a write lock on that selected row.

If another `/next` request arrives at the same time, it cannot select and update the same locked customer. It waits for the first transaction to finish.

After the first request changes the customer's status:

```text
WAITING → SERVING
```

the second request continues and selects the next available `WAITING` customer.

Therefore, two concurrent `/next` requests cannot serve the same customer.

This behavior is also covered by an automated concurrent test.

---

## Validation

The customer name is required and cannot be blank.

For example:

```json
{
  "name": ""
}
```

returns:

**400 Bad Request**

```json
{
  "timestamp": "2026-10-02T10:45:00",
  "status": 400,
  "message": "Name cannot be blank",
  "path": "/api/queue"
}
```

---

## Error Handling

The application uses a global exception handler to return consistent error responses.

Handled cases include:

* Invalid request data → `400 Bad Request`
* Customer not found → `404 Not Found`
* No waiting customers → `404 Not Found`

---

## Database

The application uses **MySQL** for persistent storage.

Create the database:

```sql
CREATE DATABASE smart_queue;
```

The application connects using:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smart_queue
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

Hibernate automatically creates and updates the required table using:

```properties
spring.jpa.hibernate.ddl-auto=update
```

The main database table is:

```text
queue_customers
```

---

## How to Run

### 1. Requirements

Make sure the following are installed:

* Java 25
* Maven
* MySQL

### 2. Create the Database

Run:

```sql
CREATE DATABASE smart_queue;
```

### 3. Configure Database Credentials

Open:

```text
src/main/resources/application.properties
```

and update:

```properties
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

Use your local MySQL password.

### 4. Run the Application

Using Maven:

```bash
mvn spring-boot:run
```

Or run the main Spring Boot application from IntelliJ IDEA.

The API will be available at:

```text
http://localhost:8080
```

---

## Testing

The project contains automated controller tests covering the main API behavior.

The test suite covers:

* Application context loading
* Adding customers
* Request validation
* Getting waiting customers
* Getting a customer by ID
* Customer not found
* Calling the next customer
* Removing customers
* Queue position calculation
* Concurrent `/next` requests

All **11 controller tests pass successfully**.

---

## Example Request Flow

A typical queue flow can look like this:

```text
POST /api/queue
        ↓
Customer added as WAITING
        ↓
GET /api/queue
        ↓
Customer appears in the queue
        ↓
GET /api/queue/{id}
        ↓
Current position is returned
        ↓
POST /api/queue/next
        ↓
Customer becomes SERVING
        ↓
DELETE /api/queue/{id}
        ↓
Customer is removed
```

---

## API Summary

| Method | Endpoint          | Description                           |
| ------ | ----------------- | ------------------------------------- |
| POST   | `/api/queue`      | Add a new customer                    |
| GET    | `/api/queue`      | Get all waiting customers             |
| GET    | `/api/queue/{id}` | Get customer information and position |
| POST   | `/api/queue/next` | Call the next waiting customer        |
| DELETE | `/api/queue/{id}` | Remove a customer                     |

---

## Project Goal

The goal of this project is to provide a simple, clean, and functional queue management API while addressing the concurrency problem described in the interview task.
