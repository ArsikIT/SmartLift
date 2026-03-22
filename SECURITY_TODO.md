# Security TODO

## C5 — No Rate Limiting on Login/Register
- `/api/auth/login` и `/api/auth/register` не ограничены по количеству запросов
- Возможен brute-force паролей и спам-регистрация организаций
- **Решение:** Bucket4j или Spring Cloud Gateway rate limiter (напр. 5 попыток/мин на IP для login, 3 регистрации/час на IP)

## M1 — No JWT Revocation
- После выдачи JWT он валиден до истечения (1 час), отозвать нельзя
- Если юзера отключили — его токен всё ещё работает
- **Решение:** token blacklist (Redis) или `tokenVersion` в User entity + проверка при каждом запросе

## M2 — Disabled Users Can Authenticate with Existing JWT
- `JwtAuthenticationFilter` не проверяет `isEnabled()` после валидации токена
- **Решение:** добавить `if (!userDetails.isEnabled()) return;` перед установкой Authentication в фильтре

## M3 — Password Required on Every User Update
- `UserRequest.password` — `@NotBlank`, каждый update требует пароль
- **Решение:** сделать пароль optional при update, хешировать только если передан

## M4 — User Enumeration via Error Messages
- Ошибки типа "Username already exists: admin" раскрывают существование аккаунтов
- **Решение:** общее сообщение "Registration failed: username, email, or organization name is unavailable"

## M5 — No Security Headers
- Нет HSTS, X-Frame-Options, X-Content-Type-Options, CSP
- **Решение:** добавить `.headers(...)` в SecurityConfig

## M6 — filePath Not Sanitized (Path Traversal Risk)
- `DocumentRequest.filePath` не проверяется на `..` и принимается как есть
- **Решение:** валидация и нормализация пути, хранить UUID-ключ вместо реального пути

## M7 — ADMIN Can See All Organizations (FIXED — now scoped to own org)

## L1 — JWT Lacks Role Claims, DB Hit on Every Request
- JWT содержит только username, роли загружаются из БД на каждый запрос
- **Решение:** добавить `roles` claim в JWT

## L2 — CORS Allows All Headers
- `allowedHeaders: *` — слишком широко
- **Решение:** ограничить до `Authorization, Content-Type, Accept`

## L3 — CORS Origins Hardcoded
- Только localhost:3000 и localhost:5173, нет конфига для prod
- **Решение:** вынести в `${CORS_ORIGINS}` переменную окружения

## L4 — No Password Complexity Validation
- Только `@Size(min=8)`, нет требований к символам
- **Решение:** `@Pattern` с regex для uppercase, digit, спецсимвол

## L5 — show-sql Enabled in Dev
- Если dev профиль случайно активен в prod — SQL в логах
- **Решение:** убедиться что prod профиль всегда явно задаётся

## L6 — No @JsonIgnore on User.password
- Если User entity случайно сериализуется — bcrypt hash утечёт
- **Решение:** добавить `@JsonIgnore` на поле `password`

## L7 — No Audit/Security Event Logging
- Нет логов login attempts, регистраций, ошибок авторизации
- **Решение:** добавить SLF4J логирование в AuthServiceImpl, RegistrationServiceImpl, GlobalExceptionHandler
