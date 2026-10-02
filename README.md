# 🚀 Smart Queue API

> Kiçik xidmət mərkəzi üçün hazırlanmış sadə, səliqəli və təhlükəsiz növbə idarəetmə REST API-si.

**NV ProjectLab Backend Interview Task** çərçivəsində hazırlanmışdır.

Smart Queue API müştərilərin növbəyə əlavə olunmasını, növbədəki mövqelərinin yoxlanılmasını, növbəti müştərinin çağırılmasını və müştərilərin növbədən silinməsini təmin edir.

---

## ✨ Əsas imkanlar

* 👤 Yeni müştərinin növbəyə əlavə edilməsi
* 📋 Gözləyən müştərilərin siyahısının göstərilməsi
* 🔢 Müştərinin cari növbə mövqeyinin hesablanması
* 📢 Növbəti müştərinin çağırılması
* 🗑️ Müştərinin növbədən silinməsi
* 🔄 **FIFO (First In, First Out)** növbə prinsipi
* 🟢 Müştəri statuslarının idarə olunması
* ✅ Request validation
* ⚠️ Vahid error response strukturu
* 💾 MySQL ilə məlumatların saxlanılması
* 🔒 **Concurrent `/next` sorğularından qorunma**
* 🧪 Avtomatlaşdırılmış controller testləri

---

## 🛠️ Texnologiyalar

| Texnologiya           | İstifadə məqsədi               |
| --------------------- | ------------------------------ |
| **Java 25**           | Əsas proqramlaşdırma dili      |
| **Spring Boot 4.1.1** | Backend framework              |
| **Spring Web MVC**    | REST API                       |
| **Spring Data JPA**   | Database interaction           |
| **Hibernate**         | ORM                            |
| **MySQL**             | Relational database            |
| **Maven**             | Dependency və build management |
| **JUnit 5**           | Testing                        |
| **MockMvc**           | Controller testləri            |

---

## 🏗️ Layihə arxitekturası

```text
src/main/java/com/example/smartqueueapi
│
├── controller
│   └── QueueCustomerController.java
│
├── service
│   └── QueueCustomerService.java
│
├── repository
│   └── QueueCustomerRepository.java
│
├── dto
│   ├── QueueCustomerRequest.java
│   └── QueueCustomerResponse.java
│
├── exception
│   ├── ErrorResponse.java
│   └── GlobalExceptionHandler.java
│
└── entity
    ├── QueueCustomer.java
    └── QueueStatus.java
```

Layihədə sadə **layered architecture** istifadə olunur:

**Controller → Service → Repository → Database**

* **Controller** — HTTP request-ləri qəbul edir və response qaytarır.
* **Service** — növbənin əsas business logic hissəsini idarə edir.
* **Repository** — database ilə əlaqəni təmin edir.
* **Entity** — database-dəki məlumatları modelləşdirir.
* **DTO** — API-yə daxil olan və API-dən çıxan məlumatları idarə edir.
* **Exception Handler** — xətaları vahid formatda qaytarır.

---

## 👤 Data Model

Hər müştəri aşağıdakı məlumatları saxlayır:

| Field       | Type          | İzah                    |
| ----------- | ------------- | ----------------------- |
| `id`        | Long          | Unikal müştəri ID-si    |
| `name`      | String        | Müştərinin adı          |
| `createdAt` | LocalDateTime | Növbəyə qoşulma vaxtı   |
| `status`    | QueueStatus   | Müştərinin cari statusu |

### Statuslar

```text
🟡 WAITING
🔵 SERVING
🟢 COMPLETED
```

`position` database-də saxlanılmır.

O, yalnız `WAITING` vəziyyətində olan müştərilər əsasında **dinamik hesablanır**.

---

# 🔄 Növbə məntiqi

Layihə **FIFO — First In, First Out** prinsipindən istifadə edir.

Yəni növbəyə birinci daxil olan müştəri birinci çağırılır.

Gözləyən müştərilər aşağıdakı qaydada sıralanır:

```text
createdAt ASC
id ASC
```

`createdAt` əsas sıralama meyarıdır.

Əgər iki müştərinin `createdAt` dəyəri eyni olarsa, `id` ikinci meyar kimi istifadə olunur. Bu, növbənin deterministik qalmasını təmin edir.

Məsələn:

```text
👤 A → birinci daxil oldu
👤 B → ikinci daxil oldu
👤 C → üçüncü daxil oldu
```

`POST /api/queue/next` çağırıldıqda:

```text
A → SERVING
B → WAITING
C → WAITING
```

Müştəri növbədən silindikdə və ya `SERVING` vəziyyətinə keçdikdə qalan müştərilərin `position` dəyəri avtomatik olaraq yenidən hesablanır.

---

# 🔒 Concurrency Protection

> **Layihənin əsas texniki hissələrindən biri**

Task-da tələb olunur ki, iki `/next` request-i eyni anda gəldikdə **eyni müştəri iki dəfə çağırılmasın**.

Bunun üçün layihədə:

* `@Transactional`
* `PESSIMISTIC_WRITE` database lock

istifadə olunur.

Repository-də:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<QueueCustomer> findFirstByStatusOrderByCreatedAtAscIdAsc(
        QueueStatus status);
```

Service-də isə:

```java
@Transactional
public QueueCustomerResponse nextCustomer() {
    ...
}
```

### ⚙️ Mexanizm necə işləyir?

Birinci `/next` request-i `WAITING` vəziyyətində olan müştərini seçdikdə həmin database sətrinə **write lock** qoyulur.

Eyni anda ikinci `/next` request-i gəldikdə həmin müştərini götürə bilmir və birinci transaction-un tamamlanmasını gözləyir.

Birinci request:

```text
WAITING → SERVING
```

dəyişiklik etdikdən sonra ikinci request davam edir və növbədəki digər `WAITING` müştərini seçir.

Beləliklə:

```text
Request 1 → Customer A
Request 2 → Customer B
```

və:

```text
❌ Request 1 → Customer A
❌ Request 2 → Customer A
```

vəziyyətinin qarşısı alınır.

Bu davranış ayrıca **concurrent controller test** ilə də yoxlanılmışdır.

---

# 🌐 API Endpoints

## 1️⃣ Müştəri əlavə etmək

### `POST /api/queue`

Yeni müştərini növbəyə əlavə edir.

**Request:**

```json
{
  "name": "Ali"
}
```

**Response — `201 Created`:**

```json
{
  "id": 1,
  "name": "Ali",
  "createdAt": "2026-10-02T10:30:00",
  "status": "WAITING",
  "position": 1
}
```

Yeni müştəri avtomatik olaraq:

```text
status = WAITING
createdAt = current time
```

alır.

---

## 2️⃣ Gözləyən müştəriləri göstərmək

### `GET /api/queue`

Hazırda `WAITING` vəziyyətində olan bütün müştəriləri qaytarır.

**Response — `200 OK`:**

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

---

## 3️⃣ Müştərini ID ilə tapmaq

### `GET /api/queue/{id}`

Müştərinin məlumatlarını və cari növbə mövqeyini qaytarır.

**Məsələn:**

```text
GET /api/queue/1
```

**Response — `200 OK`:**

```json
{
  "id": 1,
  "name": "Ali",
  "createdAt": "2026-10-02T10:30:00",
  "status": "WAITING",
  "position": 1
}
```

Müştəri artıq gözləmirsə:

```json
"position": null
```

qaytarılır.

Müştəri mövcud deyilsə:

**`404 Not Found`**

```json
{
  "timestamp": "2026-10-02T10:35:00",
  "status": 404,
  "message": "Customer not found",
  "path": "/api/queue/999"
}
```

---

## 4️⃣ Növbəti müştərini çağırmaq

### `POST /api/queue/next`

Növbədəki ilk `WAITING` müştərini seçir və statusunu:

```text
WAITING → SERVING
```

dəyişir.

**Response — `200 OK`:**

```json
{
  "id": 1,
  "name": "Ali",
  "createdAt": "2026-10-02T10:30:00",
  "status": "SERVING",
  "position": null
}
```

Növbədə heç kim yoxdursa:

**`404 Not Found`**

```json
{
  "timestamp": "2026-10-02T10:40:00",
  "status": 404,
  "message": "No waiting customers",
  "path": "/api/queue/next"
}
```

---

## 5️⃣ Müştərini silmək

### `DELETE /api/queue/{id}`

Müştərini database-dən silir.

Uğurlu olduqda:

**`204 No Content`**

Müştəri mövcud deyilsə:

**`404 Not Found`**

---

# ✅ Validation

Müştərinin adı boş ola bilməz.

Məsələn:

```json
{
  "name": ""
}
```

request-i:

**`400 Bad Request`**

qaytarır.

```json
{
  "timestamp": "2026-10-02T10:45:00",
  "status": 400,
  "message": "Name cannot be blank",
  "path": "/api/queue"
}
```

Validation üçün `@NotBlank` istifadə olunur.

---

# ⚠️ Error Handling

Layihədə bütün əsas xətalar üçün **Global Exception Handler** istifadə olunur.

Əsas hallara:

* `400 Bad Request` — düzgün olmayan request
* `404 Not Found` — müştəri tapılmadıqda
* `404 Not Found` — növbədə gözləyən müştəri olmadıqda

daxildir.

Error response-lar vahid formatda qaytarılır:

```json
{
  "timestamp": "...",
  "status": 404,
  "message": "...",
  "path": "..."
}
```

---

# 💾 Database

Layihədə məlumatların saxlanılması üçün **MySQL** istifadə olunur.

Database yaratmaq üçün:

```sql
CREATE DATABASE smart_queue;
```

Database connection:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smart_queue
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}
```

MySQL password təhlükəsizlik məqsədilə `application.properties` daxilində saxlanılmır.

`DB_PASSWORD` environment variable olaraq təyin edilməlidir.

Hibernate:

```properties
spring.jpa.hibernate.ddl-auto=update
```

ilə lazımi table-ı avtomatik yaradır və yeniləyir.

Əsas table:

```text
queue_customers
```

---

# ▶️ Layihəni işə salmaq

## 1. Tələblər

Aşağıdakılar sistemdə quraşdırılmış olmalıdır:

* Java 25
* Maven
* MySQL

## 2. Database yarat

```sql
CREATE DATABASE smart_queue;
```

## 3. Database password təyin et

`DB_PASSWORD` environment variable-ına lokal MySQL password-unu əlavə et.

Application bu məlumatı belə oxuyur:

```properties
spring.datasource.password=${DB_PASSWORD}
```

## 4. Application-u işə sal

Maven ilə:

```bash
mvn spring-boot:run
```

və ya IntelliJ IDEA daxilindən əsas Spring Boot application-unu run etmək olar.

API:

```text
http://localhost:8080
```

---

# 🧪 Testing

Layihədə əsas API davranışlarını yoxlayan **11 controller test** mövcuddur.

Testlər aşağıdakı halları əhatə edir:

* ✅ Application context
* ✅ Customer əlavə edilməsi
* ✅ Request validation
* ✅ Waiting customer-ların gətirilməsi
* ✅ ID ilə customer tapılması
* ✅ Customer not found
* ✅ Next customer çağırılması
* ✅ Customer silinməsi
* ✅ Queue position hesablanması
* ✅ Concurrent `/next` request-ləri

**Bütün 11 test uğurla keçir.**

---

# 🔁 Nümunə Request Flow

Tipik istifadə ssenarisi:

```text
POST /api/queue
        │
        ▼
👤 Customer əlavə olunur
        │
        ▼
🟡 WAITING
        │
        ▼
GET /api/queue
        │
        ▼
📋 Queue və position göstərilir
        │
        ▼
GET /api/queue/{id}
        │
        ▼
🔢 Cari position göstərilir
        │
        ▼
POST /api/queue/next
        │
        ▼
🔵 SERVING
        │
        ▼
DELETE /api/queue/{id}
        │
        ▼
🗑️ Customer silinir
```

---

# 📌 API Summary

| Method   | Endpoint          | Təyinat                               |
| -------- | ----------------- | ------------------------------------- |
| `POST`   | `/api/queue`      | Yeni müştəri əlavə et                 |
| `GET`    | `/api/queue`      | Gözləyən müştəriləri göstər           |
| `GET`    | `/api/queue/{id}` | Müştəri və position məlumatını göstər |
| `POST`   | `/api/queue/next` | Növbəti müştərini çağır               |
| `DELETE` | `/api/queue/{id}` | Müştərini sil                         |

-