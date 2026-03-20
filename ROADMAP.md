# SmartLift — Roadmap (Фазы 2–5)

## Фаза 2 — Пагинация и фильтрация

1. Внедрить `Pageable` во все list-эндпоинты (возвращать `Page<T>` вместо `List<T>`)
2. Добавить фильтрацию:
   - Lifts: по статусу, организации, manufacturer
   - Events: по типу, дате (from/to), liftId
   - Maintenances: по статусу, assignedTechnicianId, дате
   - Documents: по contentType
   - Users: по organizationId, роли, enabled
   - Organizations: по типу
3. Specification API или QueryDSL для сложных комбинированных фильтров
4. Сортировка через `Sort` параметры (`?sort=createdAt,desc`)

---

## Фаза 3 — Безопасность (Spring Security + JWT)

1. **Подключить Spring Security**
   - Добавить `spring-boot-starter-security` в pom.xml
   - Настроить `SecurityFilterChain` (stateless, без сессий)

2. **JWT аутентификация**
   - Добавить библиотеку `jjwt` (io.jsonwebtoken)
   - Создать `JwtTokenProvider` — генерация и валидация токенов
   - Создать `JwtAuthenticationFilter` — фильтр для извлечения токена из заголовка `Authorization: Bearer <token>`
   - Настроить срок жизни access-токена (напр. 1 час)

3. **Эндпоинты аутентификации**
   - `POST /api/auth/register` — регистрация нового пользователя
   - `POST /api/auth/login` — вход, возврат JWT-токена
   - AuthRequest / AuthResponse DTO

4. **Хеширование паролей**
   - Подключить `BCryptPasswordEncoder`
   - Хешировать пароль при создании/обновлении пользователя
   - Обновить `UserServiceImpl`

5. **UserDetails интеграция**
   - Создать `CustomUserDetailsService implements UserDetailsService`
   - Загрузка пользователя по username с ролями

6. **Role-based доступ (RBAC)**
   - `@PreAuthorize` на эндпоинтах:
     - `ADMIN` — полный доступ ко всему
     - `MANUFACTURER` — CRUD своих лифтов, чтение событий
     - `SERVICE` — CRUD заявок и событий, чтение лифтов
     - `MANAGEMENT` — чтение лифтов, создание заявок, загрузка документов
   - Включить `@EnableMethodSecurity`

7. **CORS настройка**
   - Разрешить фронтенд-домены (настраиваемо через application.yml)

8. **Открытые эндпоинты**
   - `/api/auth/**` — без авторизации
   - Actuator health — без авторизации
   - Всё остальное — требует JWT

---

## Фаза 4 — Бизнес-логика (расширенная)

1. **State machine для лифта**
   - Валидация допустимых переходов статусов:
     ```
     CREATED → INSTALLED → ACTIVE
     ACTIVE → FAULTY → IN_REPAIR → ACTIVE
     любой → DECOMMISSIONED (необратимо)
     ```
   - Запрет недопустимых переходов (напр. CREATED → IN_REPAIR)
   - Добавить новые типы событий: `ACTIVATED`, `DECOMMISSIONED`

2. **Maintenance workflow валидация**
   - PENDING → IN_PROGRESS: требуется `assignedTechnicianId`
   - IN_PROGRESS → DONE: разрешено только назначенному технику
   - DONE → нельзя менять (закрытая заявка)
   - Запрет обратных переходов (DONE → PENDING)

3. **Загрузка файлов**
   - `POST /api/documents/upload` — multipart file upload
   - Сохранение в локальное хранилище (настраиваемый путь) или S3-совместимое
   - Генерация уникального filePath
   - `GET /api/documents/{id}/download` — скачивание файла
   - Ограничение размера файла (application.yml)
   - Валидация допустимых content-type

4. **Уведомления / события**
   - Spring Application Events при смене статуса лифта
   - Spring Application Events при создании/завершении заявки
   - Слушатели событий (логирование, будущие webhook/email)

5. **Аудит**
   - Кто и когда изменил сущность
   - Spring Data JPA `@CreatedBy` / `@LastModifiedBy` через `AuditorAware`
   - Интеграция с SecurityContext для автоматического заполнения

6. **Статистика / дашборд**
   - `GET /api/stats/lifts` — количество лифтов по статусам
   - `GET /api/stats/maintenances` — открытые/закрытые заявки, среднее время решения
   - `GET /api/stats/organizations/{id}` — статистика по организации

---

## Фаза 5 — Качество и деплой

1. **Unit-тесты**
   - Тесты сервисного слоя (Mockito для репозиториев)
   - Покрытие бизнес-логики: валидация, state machine, timestamps
   - Тесты маппера

2. **Integration-тесты**
   - `@SpringBootTest` + Testcontainers (PostgreSQL)
   - Тесты контроллеров через `MockMvc` или `WebTestClient`
   - Тесты Flyway миграций
   - Тесты security (авторизация, роли)

3. **API документация**
   - Подключить `springdoc-openapi` (Swagger UI)
   - Аннотировать контроллеры: `@Operation`, `@ApiResponse`, `@Tag`
   - Swagger UI на `/swagger-ui.html`
   - OpenAPI spec на `/v3/api-docs`

4. **Dockerfile**
   - Multi-stage build (Maven build → JRE runtime)
   - Оптимизация слоёв (layers)
   - `.dockerignore`

5. **Docker Compose (полный)**
   - PostgreSQL + приложение в одной сети
   - Healthcheck для PostgreSQL
   - Environment variables для prod-профиля

6. **CI/CD (опционально)**
   - GitHub Actions: build → test → docker build → push
   - Автоматический прогон тестов на PR

7. **Мониторинг (опционально)**
   - Spring Boot Actuator (`/actuator/health`, `/actuator/metrics`)
   - Логирование: structured JSON logs для production
