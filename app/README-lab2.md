# Лабораторна №2: Unit-тести

## Що додати
1. Скопіюй `src/test/java/**` у проєкт.
2. Скопіюй `lombok.config` у корінь (поруч із `pom.xml`).
3. Додай плагін із `pom-jacoco-snippet.xml` у `<build><plugins>` у `pom.xml`.

## Запуск
    mvn test                 # тести
    mvn verify               # тести + перевірка покриття >= 60%
Звіт покриття: `target/site/jacoco/index.html`.

Docker і Keycloak для тестів не потрібні: Spring-контекст не піднімається.

## AC4: доказ, що тест справді перевіряє логіку
У `BookingService.delete` тимчасово закоментуй перевірку:

    if (!bookingRepository.existsById(id)) {
        throw new EntityNotFoundException("Booking not found with id: " + id);
    }

Запусти `mvn test -Dtest=BookingServiceTest`: впаде
`delete_unknownId_throwsEntityNotFoundAndDoesNotDelete` — зроби скріншот.
Поверни код назад, запусти ще раз — усе зелене, зроби другий скріншот.

Інший варіант: у `update` прибери рядок `bookingMapper.updateEntity(request, entity);`
— впаде `update_existingId_mapsRequestOntoEntityAndSaves`.
