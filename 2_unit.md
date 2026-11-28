# Лабораторна робота №2

**Тема:** Unit-тестування  
**Дисципліна:** Розробка високонавантажених корпоративних систем  
**Виконав:** ПІБ, група  
**Проєкт:** `backend-template` — сервіс бронювання (Booking & Scheduling). Java 21, Spring Boot 3.5.16, PostgreSQL 16, Liquibase, MapStruct, Keycloak 26.

## Мета
Покрити наявний код unit-тестами не менше ніж на 60% (JaCoCo), переконатися, що бізнес-логіка поводиться як задумано, та довести, що тести справді перевіряють логіку.

## Інструменти та налаштування
* JUnit 5, Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, `ArgumentCaptor`), AssertJ, MockMvc у режимі `standaloneSetup`.
* Плагін `jacoco-maven-plugin` 0.8.14 у `pom.xml`: `prepare-agent`, `report` на фазі `test`, `check` на фазі `verify` з правилом `LINE COVEREDRATIO` $\ge 0.60$ для всього бандла.
* Запуск: `mvn test` (тести), `mvn verify` (тести + перевірка покриття). Звіт: `target/site/jacoco/index.html`.
* Docker і Keycloak для тестів не потрібні: Spring-контекст не піднімається, `@SpringBootTest` не використовується.

У проєкті 98 тестових методів у 19 тестових класах:

| Тестовий клас | Що перевіряє |
| :--- | :--- |
| `BookingServiceTest` | бізнес-логіка сервісу (`findAll`, `create`, `update`, `delete`) |
| `BookingControllerTest`, `BookingUIControllerTest` | делегування в сервіс, передача параметрів |
| `BookingMapperTest` | `toDto`, `toEntity`, `updateEntity`, `null`-вхід |
| `BookingTimeRangeValidatorTest` | кастомна валідація часового проміжку |
| `AuditAspectTest`, `MaskEmailAspectTest`, `RateLimitAspectTest` | логіка advice аспектів |
| `AopWiringAndSelfInvocationTest` | підключення анотацій, self-invocation |
| `CreatedPostTest`, `CurrentUsernameArgumentResolverTest` | кастомні анотації |
| `GlobalExceptionHandlerTest`, `ApiErrorTest` | обробка помилок |
| `CurrentUserAuthenticationConverterTest`, `CurrentUserTest`, `RestAuthEntryPointTest`, `RestAccessDeniedHandlerTest`, `TokenControllerTest`, `ApiControllerTest`, `AdminControllerTest` | безпека та прості контролери |

## Основний SUT: `BookingService`
Клас `BookingService` залежить від `BookingRepository` і `BookingMapper`; обидві залежності мокаються. Приклад тесту з перевіркою побічного ефекту через `ArgumentCaptor`:

```java
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingMapper bookingMapper;
    @InjectMocks private BookingService bookingService;

    @Test
    void create_validRequest_savesMappedEntityAndReturnsDto() {
        // Arrange
        CreateBookingRequest request = request("Team sync");
        BookingEntity toSave = entity(null, "Team sync");
        BookingEntity saved = entity(10L, "Team sync");
        when(bookingMapper.toEntity(request)).thenReturn(toSave);
        when(bookingRepository.save(toSave)).thenReturn(saved);
        when(bookingMapper.toDto(saved)).thenReturn(dto(10L, "Team sync"));

        // Act
        BookingDto result = bookingService.create(request);

        // Assert
        assertEquals(10L, result.getId());
        ArgumentCaptor<BookingEntity> captor = ArgumentCaptor.forClass(BookingEntity.class);
        verify(bookingRepository).save(captor.capture());
        assertEquals(BookingStatus.PENDING, captor.getValue().getStatus());
    }
}
```

## Відповідність критеріям

* **AC1 — штатні сценарії з реальними assertions.** Для кожного методу є тест із типовими даними та перевіркою результату: `findAll_bookingsExist_returnsMappedDtosInOrder`, `create_validRequest_savesMappedEntityAndReturnsDto`, `update_existingId_mapsRequestOntoEntityAndSaves`, `delete_existingId_deletesById`.
* **AC2 — невалідні дані, межі та відмови.**
  * `update_unknownId_throwsEntityNotFoundAndDoesNotSave`, `delete_unknownId_throwsEntityNotFoundAndDoesNotDelete` — очікуваний виняток із повідомленням, що містить `id`;
  * `create_repositoryFails_propagatesExceptionAndDoesNotMapResult`, `findAll_repositoryFails_propagatesException` — відмова залежності;
  * `findAll_noBookings_returnsEmptyListAndNeverUsesMapper` — порожній результат;
  * `validate_exactlyMinAndMaxDuration_noViolation`, `validate_tooShort_…`, `validate_tooLong_…` — граничні значення (15 та 480 хв);
  * `maskEmail_variants`, `mask_nullResult_returnsNull` — `null` та нестандартні вхідні дані.
* **AC3 — залежності імітуються, Spring не потрібен.** Використано `@Mock` / `@InjectMocks` з `MockitoExtension`; контролери тестуються через `MockMvcBuilders.standaloneSetup(...)`, без Spring-бінів і контексту.
* **AC5 — ізольованість і детермінованість.** Тести не використовують БД, мережу чи файлову систему, `Thread.sleep` та системний час. Час в аспектах вводиться через `java.time.Clock`: у `AuditAspectTest` — `Clock.fixed(...)`, у `RateLimitAspectTest` — власний `MutableClock`, який тест «програє» вручну. `SecurityContextHolder` очищається в `@AfterEach`, тому тести не залежать від порядку виконання.
* **AC6 — іменування та структура.** Імена мають вигляд `метод_сценарій_очікуванийРезультат` (`delete_unknownId_throwsEntityNotFoundAndDoesNotDelete`). Тіла тестів структуровані за Arrange-Act-Assert (Given-When-Then).
* **AC7 — перевірка побічних ефектів.** Використовуються `Mockito.verify(...)`, `verify(..., never())` та `ArgumentCaptor`: `verify(bookingRepository).deleteById(3L)`, `verify(bookingRepository, never()).save(any())`, `verify(bookingMapper).updateEntity(request, existing)`, захоплення `BookingEntity` у `create` з перевіркою статусу `PENDING`.
* **AC8 — усі публічні методи класу.** Для всіх чотирьох публічних методів `BookingService` є і штатні (AC1), і негативні (AC2) тести:

| Метод | Штатний сценарій (AC1) | Негативний / граничний (AC2) |
| :--- | :--- | :--- |
| `findAll` | `findAll_bookingsExist_returnsMappedDtosInOrder` | `findAll_noBookings_…`, `findAll_repositoryFails_…` |
| `create` | `create_validRequest_savesMappedEntityAndReturnsDto` | `create_repositoryFails_…` |
| `update` | `update_existingId_mapsRequestOntoEntityAndSaves` | `update_unknownId_throwsEntityNotFoundAndDoesNotSave` |
| `delete` | `delete_existingId_deletesById` | `delete_unknownId_throwsEntityNotFoundAndDoesNotDelete` |

### AC4 — доказ, що тест справді перевіряє логіку
У методі `BookingService.delete` тимчасово закоментовано перевірку існування запису:

```java
// if (!bookingRepository.existsById(id)) {
//     throw new EntityNotFoundException("Booking not found with id: " + id);
// }
bookingRepository.deleteById(id);
```

Запуск `mvn test -Dtest=BookingServiceTest` — тест `delete_unknownId_throwsEntityNotFoundAndDoesNotDelete` падає, бо виняток більше не викидається, а `deleteById` викликається для неіснуючого `id`.

> *(Примітка: у вихідному документі на цьому місці додано скріншот термінала з виконанням команди `mvn test -Dtest=BookingServiceTest`)*

Після повернення коду до початкового стану повторний запуск `mvn test -Dtest=BookingServiceTest` — усі тести знову проходять.

## Висновок
Написано unit-тести для бізнес-логіки, контролерів, мапера, валідатора, аспектів, обробників помилок та компонентів безпеки. Залежності замоковані, Spring-контекст не потрібен, тести детерміновані та ізольовані. Мінімальний поріг покриття 60% зафіксовано в збірці (`mvn verify`), а AC4 підтверджено навмисним порушенням логіки `delete` із падінням і подальшим відновленням тесту.