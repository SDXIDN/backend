# ЛР7 — Spring Boot REST API

Вимоги: JDK 21, Maven 3.9+ (БД не потрібна).

Запуск:
```
mvn spring-boot:run
```
Тести:
```
mvn test
```
Зібрати й запустити jar:
```
mvn clean package
java -jar target/booking-lab7-1.0.0-SNAPSHOT.jar
```
Сервер: http://localhost:8080

Ресурси: /api/v1/venues, /api/v1/rooms, /api/v1/customers, /api/v1/bookings, /api/v1/reviews
(GET, GET /{id}, POST, PUT /{id}, PATCH /{id}, DELETE /{id})

Приклади:
```
curl -X POST localhost:8080/api/v1/venues -H "Content-Type: application/json" -d '{"name":"Main Hall","address":"Shevchenka 1","city":"Chernivtsi"}'
curl -X POST localhost:8080/api/v1/rooms -H "Content-Type: application/json" -d '{"name":"Room A","capacity":20,"hourlyPrice":250,"venueId":1}'
curl localhost:8080/api/v1/rooms
curl -X PATCH localhost:8080/api/v1/rooms/1 -H "Content-Type: application/json" -d '{"capacity":30}'
curl -X DELETE localhost:8080/api/v1/rooms/1
```
Дані зберігаються в пам'яті й зникають після перезапуску.
