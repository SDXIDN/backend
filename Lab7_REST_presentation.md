---
marp: true
theme: default
paginate: true
title: Лабораторна робота №7 — Spring Boot REST API
---

# Лабораторна робота №7
## Spring Boot REST API: вступ

Проєкт: **booking service**
Пакет: `com.course.booking`

> Мета: розробити REST-застосунок на Spring Boot з чітким розмежуванням Controller / Service / Repository та повним CRUD для кількох моделей без використання бази даних.

---

## Завдання

| # | Вимога |
|---|--------|
| 1 | Обрати предметну область |
| 2 | Не менше 5 моделей (класів) |
| 3–4 | Створити проєкт через Spring Initializr, обрати лише потрібні компоненти |
| 5 | Обрати принцип організації коду й дотримуватися його |
| 6–7 | Моделі, репозиторії, сервіси, контролери; розмежування концепцій |
| 8 | CRUD для всіх моделей: GET, POST, PUT, PATCH, DELETE |
| 9 | Без БД: in-memory структури даних |

---

## Предметна область: бронювання кімнат

| # | Модель | Поля | Зв'язки |
|---|--------|------|---------|
| 1 | `Venue` (заклад) | name, address, city | 1 — N `Room` |
| 2 | `Room` (кімната) | name, capacity, hourlyPrice | `venueId` |
| 3 | `Customer` (клієнт) | fullName, email, phone | |
| 4 | `Booking` (бронювання) | startTime, endTime, status | `roomId`, `customerId` |
| 5 | `Review` (відгук) | rating, comment | `roomId`, `customerId` |

Усі моделі успадковують `BaseEntity` з полем `id`. Зв'язки зберігаються як ідентифікатори.

---

## Обрані компоненти

```xml
spring-boot-starter-web          REST, Jackson, вбудований Tomcat
lombok                           getter/setter/конструктори
spring-boot-starter-test         JUnit 5, MockMvc, AssertJ
```

- Немає JPA, драйверів БД, Security, Liquibase: у цій роботі вони не потрібні
- Java 21, Spring Boot 3.5

---

## Принцип організації коду: package-by-feature

```
com.course.booking
 ├─ common/     InMemoryRepository, CrudService, винятки, GlobalExceptionHandler
 ├─ venue/      Venue, VenueRepository, VenueService, VenueController
 ├─ room/       Room, RoomRepository, RoomService, RoomController
 ├─ customer/   Customer, CustomerRepository, CustomerService, CustomerController
 ├─ booking/    Booking, BookingStatus, BookingRepository, BookingService, BookingController
 └─ review/     Review, ReviewRepository, ReviewService, ReviewController
```

Усе, що стосується однієї сутності, лежить в одному пакеті.

---

## Розмежування концепцій

```
HTTP-запит
   │
   ▼
Controller   приймає запит, віддає відповідь, нічого не вирішує
   │
   ▼
Service      правила предметної області: валідація, посилання, конфлікти
   │
   ▼
Repository   лише зберігання й пошук даних
```

| Шар | Відповідальність | Чого не робить |
|-----|------------------|----------------|
| Controller | маршрути, статус-коди | бізнес-правил |
| Service | валідація, перевірка зв'язків | роботи з HTTP |
| Repository | `save`, `findById`, `findAll`, `deleteById` | бізнес-логіки |

---

## «Штучний» репозиторій замість БД

```java
public abstract class InMemoryRepository<T extends BaseEntity> {

    private final Map<Long, T> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public T save(T entity) {
        if (entity.getId() == null) {
            entity.setId(sequence.incrementAndGet());
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    public Optional<T> findById(Long id) {
        return id == null ? Optional.empty() : Optional.ofNullable(store.get(id));
    }
}
```

- `ConcurrentHashMap` і `AtomicLong` безпечні для паралельних запитів
- Нові сутності отримують наступний id, наявні перезаписуються

---

## Спеціалізовані репозиторії

```java
@Repository
public class RoomRepository extends InMemoryRepository<Room> {

    public List<Room> findByVenueId(Long venueId) {
        return findAll().stream()
                .filter(r -> Objects.equals(r.getVenueId(), venueId))
                .toList();
    }

    public boolean existsByVenueId(Long venueId) {
        return findAll().stream().anyMatch(r -> Objects.equals(r.getVenueId(), venueId));
    }
}
```

Додаткові методи пошуку описуються в підкласі. У ЛР8 такі класи замінять інтерфейси `JpaRepository`, а сервіси й контролери лишаться без змін.

---

## Спільний каркас сервісів: CrudService

```java
public T create(T entity) {
    entity.setId(null);
    applyDefaults(entity);
    validate(entity);
    checkConstraints(entity);
    return repository.save(entity);
}

public T patch(Long id, T patch) {
    T merged = merge(findById(id), patch);
    merged.setId(id);
    validate(merged);
    checkConstraints(merged);
    return repository.save(merged);
}
```

Кожен сервіс лише описує свої правила: `validate`, `checkConstraints`, `merge`, `checkCanDelete`, `applyDefaults`.

---

## Приклад сервісу: RoomService

```java
@Override
protected void validate(Room room) {
    requireText(room.getName(), "name");
    requireNotNull(room.getCapacity(), "capacity");
    if (room.getCapacity() < 1) {
        throw new IllegalArgumentException("capacity must be at least 1");
    }
    requireNotNull(room.getVenueId(), "venueId");
}

@Override
protected void checkConstraints(Room room) {
    if (!venueRepository.existsById(room.getVenueId())) {
        throw new NotFoundException("Venue not found with id: " + room.getVenueId());
    }
}
```

---

## Контролер

```java
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public List<Room> getAll(@RequestParam(required = false) Long venueId) {
        return roomService.findAll(venueId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Room create(@RequestBody Room body) {
        return roomService.create(body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        roomService.delete(id);
    }
}
```

---

## CRUD для кожної моделі

| Метод | Шлях | Дія | Успіх |
|-------|------|-----|-------|
| GET | `/api/v1/{ресурс}` | список | 200 |
| GET | `/api/v1/{ресурс}/{id}` | одна сутність | 200 |
| POST | `/api/v1/{ресурс}` | створення | 201 |
| PUT | `/api/v1/{ресурс}/{id}` | повна заміна | 200 |
| PATCH | `/api/v1/{ресурс}/{id}` | часткове оновлення | 200 |
| DELETE | `/api/v1/{ресурс}/{id}` | видалення | 204 |

Ресурси: `venues`, `rooms`, `customers`, `bookings`, `reviews`.

Фільтри: `venues?city=`, `rooms?venueId=`, `bookings?roomId=&customerId=`, `reviews?roomId=`.

---

## PUT і PATCH

| | PUT | PATCH |
|---|-----|-------|
| Що змінює | усі поля | лише передані |
| Відсутнє поле | вважається порожнім → 400 | залишається як було |
| Приклад | `{"name":"A","address":"B","city":"C"}` | `{"city":"Odesa"}` |

```java
protected Venue merge(Venue existing, Venue patch) {
    return new Venue(
            orElse(patch.getName(), existing.getName()),
            orElse(patch.getAddress(), existing.getAddress()),
            orElse(patch.getCity(), existing.getCity()));
}
```

PATCH створює **нову** сутність і зберігає її лише після успішної перевірки, тому невдалий запит не псує збережені дані.

---

## Правила предметної області

| Модель | Правила |
|--------|---------|
| `Venue` | name, address, city не порожні |
| `Room` | capacity ≥ 1, hourlyPrice > 0, заклад існує |
| `Customer` | коректний email, унікальний без урахування регістру; phone: 7–15 цифр |
| `Booking` | кімната і клієнт існують, `endTime` пізніше `startTime`, статус за замовчуванням `PENDING`, без перетину в одній кімнаті |
| `Review` | rating від 1 до 5, кімната і клієнт існують, comment до 1000 символів |

Скасовані бронювання (`CANCELLED`) не блокують кімнату.

---

## Обробка помилок

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
```

| Код | Коли |
|-----|------|
| 400 | порожні чи некоректні поля, зламаний JSON, невідомий enum |
| 404 | сутності або посилання (`venueId`, `roomId`, `customerId`) не існує |
| 409 | дубль email, перетин бронювань, видалення сутності з залежностями |

Відповідь про помилку: `{status, error, message, timestamp}`.

---

## Цілісність посилань без БД

Зовнішніх ключів немає, тому їх замінюють перевірки в сервісах.

| Видалення | Умова відмови (409) |
|-----------|---------------------|
| `Venue` | у закладі є кімнати |
| `Room` | на кімнату є бронювання або відгуки |
| `Customer` | на клієнта є бронювання або відгуки |

```java
protected void checkCanDelete(Long id) {
    if (roomRepository.existsByVenueId(id)) {
        throw new ConflictException("Venue " + id + " still has rooms; delete them first");
    }
}
```

---

## Відповідність вимогам лабораторної

| Вимога | Реалізація |
|--------|-----------|
| 1. Предметна область | бронювання кімнат |
| 2. ≥ 5 моделей | `Venue`, `Room`, `Customer`, `Booking`, `Review` |
| 3–4. Новий проєкт, лише потрібні компоненти | `web`, `lombok`, `test` |
| 5. Принцип організації коду | package-by-feature |
| 6. Entity, Repository, Service, Controller | у кожному пакеті |
| 7. Розмежування шарів | Controller → Service → Repository |
| 8. CRUD, GET/POST/PUT/PATCH/DELETE | усі 5 ресурсів |
| 9. Без БД | `InMemoryRepository` на `ConcurrentHashMap` |

---

## Тестування

- **`InMemoryRepositoryTest`**: послідовні id, перезапис, видалення, порядок, пошук за містом
- **`ApiFlowTest`**: наскрізні тести через `MockMvc` (HTTP → контролер → сервіс → репозиторій)

| Сценарій | Очікування |
|----------|-----------|
| Повний цикл GET / POST / PUT / PATCH / DELETE | 200, 201, 204, потім 404 |
| Порожнє ім'я, некоректний email, рейтинг 6 | 400 |
| Невдалий PATCH | збережені дані не змінились |
| Неіснуючий `venueId`, `roomId`, `customerId` | 404 |
| Дубль email, перетин бронювань, видалення із залежностями | 409 |
| Скасоване бронювання | не блокує кімнату |

Запуск: `mvn test`.

---

## Демонстрація

1. `mvn spring-boot:run`
2. Створити заклад:
   ```
   curl -X POST localhost:8080/api/v1/venues -H "Content-Type: application/json" \
     -d '{"name":"Main Hall","address":"Shevchenka 1","city":"Chernivtsi"}'
   ```
3. Створити кімнату з `venueId` із відповіді, потім клієнта й бронювання
4. `PATCH /api/v1/rooms/1` з `{"capacity":30}`: змінилась лише місткість
5. Повторне бронювання на той самий час: `409 Conflict`
6. `DELETE /api/v1/venues/1`, поки є кімнати: `409 Conflict`

---

## Висновки

- Створено REST API на Spring Boot з **5 моделями** і повним CRUD (GET, POST, PUT, PATCH, DELETE)
- Код розділено на шари **Controller / Service / Repository** і організовано за принципом package-by-feature
- Замість БД використано **in-memory репозиторій**, який легко замінити на JPA у наступній роботі
- Правила предметної області й цілісність посилань забезпечують сервіси, помилки повертаються єдиним форматом із коректними HTTP-кодами
- Поведінку підтверджено юніт- та наскрізними тестами

**Дякую за увагу!**
