# USAGE

## 1) Запуск профилей

### DEV (H2 in-memory)

```powershell
./gradlew bootRun --args="--spring.profiles.active=dev"
```

### PROD (PostgreSQL в Docker)

1) Поднять БД:

```powershell
docker-compose up -d
```

2) Запустить приложение:

```powershell
./gradlew bootRun --args="--spring.profiles.active=prod"
```

## 2) Обязательные переменные окружения

### JWT secret

Приложение читает JWT секрет из `APP_JWT_SECRET`.

Требование:
- Base64
- минимум 32 байта

Генерация (PowerShell):

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:APP_JWT_SECRET = [Convert]::ToBase64String($bytes)
```

### Card encryption secret

Шифрование номера карты читает секрет из `APP_CRYPTO_CARD_SECRET`.

Требование:
- Base64
- ровно 32 байта

Генерация (PowerShell):

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:APP_CRYPTO_CARD_SECRET = [Convert]::ToBase64String($bytes)
```

### Bootstrap ADMIN (опционально)

Если задать `APP_ADMIN_USERNAME` и `APP_ADMIN_PASSWORD`, при старте создастся bootstrap админ (если пользователя ещё нет).

```powershell
$env:APP_ADMIN_USERNAME = "admin"
$env:APP_ADMIN_PASSWORD = "AdminStrongPass123"
```

## 3) Auth API

### Регистрация USER

`POST /api/auth/register`

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"StrongPass123"}'
```

### Логин (получение access token)

`POST /api/auth/login`

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"StrongPass123"}'
```

Ответ содержит:
- `accessToken`
- `tokenType` ("Bearer")

## 4) Cards API

### Получение карт

`GET /api/cards`

- USER: видит только свои карты (owner берётся из JWT principal)
- ADMIN: видит все, параметр `owner` — фильтр

Пример:

```bash
curl http://localhost:8080/api/cards \
  -H "Authorization: Bearer <accessToken>"
```

### Создание карты (только ADMIN)

`POST /api/cards`

Пример:

```bash
curl -X POST http://localhost:8080/api/cards \
  -H "Authorization: Bearer <adminAccessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber":"4111 1111 1111 1111",
    "owner":"user1",
    "expiryDate":"2030-12-31",
    "status":"ACTIVE",
    "balance":100.00
  }'
```

### Получение карты по id

`GET /api/cards/{id}`

```bash
curl http://localhost:8080/api/cards/<uuid> \
  -H "Authorization: Bearer <accessToken>"
```

### Запрос блокировки карты (USER)

`POST /api/cards/{id}/block-request`

```bash
curl -X POST http://localhost:8080/api/cards/<uuid>/block-request \
  -H "Authorization: Bearer <accessToken>"
```

### Обновление статуса карты (ADMIN)

`PATCH /api/cards/{id}/status`

```bash
curl -X PATCH http://localhost:8080/api/cards/<uuid>/status \
  -H "Authorization: Bearer <adminAccessToken>" \
  -H "Content-Type: application/json" \
  -d '{"status":"BLOCKED"}'
```

### Удаление карты (ADMIN)

`DELETE /api/cards/{id}`

```bash
curl -X DELETE http://localhost:8080/api/cards/<uuid> \
  -H "Authorization: Bearer <adminAccessToken>"
```

## 5) Transfers API

`POST /api/transfers`

Доступ:
- USER: только между своими картами
- ADMIN: только между картами одного owner

Пример:

```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "fromCardId":"<uuid>",
    "toCardId":"<uuid>",
    "amount": 10.00
  }'
```

## 6) Swagger

Swagger UI:
- `http://localhost:8080/swagger-ui/index.html`

Swagger защищён: нужен JWT (через Authorize / Bearer token).

OpenAPI spec file:
- `docs/openapi.yaml`

## 7) Admin Users API (ADMIN)

### Список пользователей

`GET /api/admin/users`

```bash
curl http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer <adminAccessToken>"
```

### Включить/выключить пользователя

`PATCH /api/admin/users/{id}/enabled`

```bash
curl -X PATCH http://localhost:8080/api/admin/users/<uuid>/enabled \
  -H "Authorization: Bearer <adminAccessToken>" \
  -H "Content-Type: application/json" \
  -d '{"enabled":false}'
```

### Назначить роли пользователю

`PATCH /api/admin/users/{id}/roles`

```bash
curl -X PATCH http://localhost:8080/api/admin/users/<uuid>/roles \
  -H "Authorization: Bearer <adminAccessToken>" \
  -H "Content-Type: application/json" \
  -d '{"roles":["ROLE_USER"]}'
```

## 8) Тесты

### Запуск всех тестов

```powershell
./gradlew test
```

### Запуск одного тест-класса

```powershell
./gradlew test --tests org.example.bankcardmanagement.security.AuthControllerIT
```

### Запуск одного тест-метода

```powershell
./gradlew test --tests org.example.bankcardmanagement.security.AuthControllerIT.registerAndLogin_success
```
