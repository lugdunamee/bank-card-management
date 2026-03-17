# bank-card-management

## Progress Log

- Step 1 (Card entity + statuses enum + repository + Liquibase migration for `cards`): DONE
- Step 2 (CardDto + MapStruct CardMapper + базовый CardService): DONE
- Step 3 (CardRequestDto + validation + CardController endpoints + pagination): DONE
- Build fix: MapStruct -> Card instantiation (public no-args constructor): DONE
- Step 4A (Global error handling: @RestControllerAdvice + unified ApiError): DONE
- Step 4B (AES/GCM encryption for card number + store last4 + Liquibase 002): DONE
- Step 5 (Security: users/roles + JWT + ADMIN bootstrap + basic bruteforce mitigation): DONE

## Структура проекта (коротко)

- `docker-compose.yml`
  - PostgreSQL 15 в Docker.
  - Хост-порт: `5433` -> контейнерный порт: `5432`.
- `build.gradle`, `gradlew`, `gradlew.bat`
  - Gradle + Spring Boot.
- `src/main/resources/`
  - `application.properties` — базовые настройки (по умолчанию активен профиль `dev`).
  - `application-dev.properties` — профиль `dev` (H2 in-memory).
  - `application-prod.properties` — профиль `prod` (PostgreSQL).
  - `db/migration/db.migration-master.xml` — мастер changelog Liquibase.
- `src/main/java/` — исходный код приложения.

## Запуск базы данных (PostgreSQL в Docker)

Из корня проекта:

```bash
docker-compose up -d
docker-compose ps
```

Ожидаемо для сервиса `db`:

- контейнер `bank_db_container`
- статус `Up ...`
- порты: `0.0.0.0:5433->5432/tcp`

## Запуск приложения с профилем PROD (PostgreSQL + Liquibase)

1) Убедись, что PostgreSQL контейнер поднят (`docker-compose ps`).

2) Запусти приложение:

```bash
./gradlew bootRun --args="--spring.profiles.active=prod"
```

Что проверять в логах:

- Подключение к PostgreSQL:
  - `HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection...`
  - `Database JDBC URL [jdbc:postgresql://localhost:5433/bank_db]`
- Liquibase отработал и применил миграции (или подтвердил, что изменений нет):
  - `Creating database changelog table with name: public.databasechangelog`
  - `Database is up to date, no changesets to execute`
  - `Command execution complete`

## Запуск приложения с профилем DEV (H2)

```bash
./gradlew bootRun --args="--spring.profiles.active=dev"
```

Что проверять в логах:

- Подключение к H2:
  - `Database JDBC URL [jdbc:h2:mem:bank_db]`
  - `Database driver: H2 JDBC Driver`

## Остановка

- Приложение: остановить процесс (Ctrl+C в терминале) или завершить PID.
- База данных:

```bash
docker-compose down
```

## Шифрование номера карты (Step 4B)

Для шифрования используется AES/GCM. Ключ берётся из переменной окружения `APP_CRYPTO_CARD_SECRET`.

- Требование к ключу: Base64 от 32 байт (256-bit).

Пример (PowerShell) с генерацией ключа:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Запуск с переменной окружения (пример):

```powershell
$env:APP_CRYPTO_CARD_SECRET = "<base64-ключ>"
./gradlew bootRun --args="--spring.profiles.active=dev"
```

## Security (JWT + роли)

### Переменные окружения

- `APP_JWT_SECRET` — Base64 секрет для подписи JWT (HMAC). Минимум 32 байта.
- `APP_ADMIN_USERNAME` и `APP_ADMIN_PASSWORD` — опционально. Если заданы, при старте приложения будет создан bootstrap-админ (если такого пользователя ещё нет).

Генерация `APP_JWT_SECRET` (PowerShell):

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:APP_JWT_SECRET = [Convert]::ToBase64String($bytes)
```

Bootstrap ADMIN (пример):

```powershell
$env:APP_ADMIN_USERNAME = "admin"
$env:APP_ADMIN_PASSWORD = "AdminStrongPass123"
```

### Auth endpoints

- `POST /api/auth/register` — регистрация USER
- `POST /api/auth/login` — логин (получение access token)

Пример register:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"StrongPass123"}'
```

Пример login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"StrongPass123"}'
```

В ответе будет `accessToken` (Bearer).

### Cards endpoints (роль/доступ)

- `GET /api/cards`
  - USER: видит только свои карты (owner берётся из JWT principal)
  - ADMIN: может смотреть все, параметр `owner` — фильтр
- `POST /api/cards` — только ADMIN

Дополнительно:

- `GET /api/cards/{id}`
  - USER: только свою карту
  - ADMIN: любую
- `POST /api/cards/{id}/block-request` — USER (запрос блокировки)
- `PATCH /api/cards/{id}/status` — ADMIN (смена статуса)
- `DELETE /api/cards/{id}` — ADMIN

### Transfers endpoints

- `POST /api/transfers` — аутентифицированные
  - USER: только между своими картами
  - ADMIN: только между картами одного владельца

### Admin users endpoints (ADMIN)

- `GET /api/admin/users`
- `PATCH /api/admin/users/{id}/enabled`
- `PATCH /api/admin/users/{id}/roles`

## Документация

- `docs/USAGE.md` — как запускать и как пользоваться API
- `docs/openapi.yaml` — OpenAPI spec file

## Тесты

Запуск всех тестов:

```bash
./gradlew test
```

Запуск одного тест-класса:

```bash
./gradlew test --tests org.example.bankcardmanagement.security.AuthControllerIT
```

Пример вызова с токеном:

```bash
curl http://localhost:8080/api/cards \
  -H "Authorization: Bearer <accessToken>"
```
