# SmartLift — Платформа управления жизненным циклом лифтового оборудования

## Обзор

SmartLift — цифровая платформа для управления жизненным циклом лифтов: от регистрации и установки до обслуживания и списания. REST API на Spring Boot 3.4.4.

## Стек технологий

- **Java 22**, **Spring Boot 3.4.4**
- **Spring Data JPA** + **Hibernate** (PostgreSQL)
- **Flyway** — миграции БД
- **Lombok** — генерация boilerplate
- **Bean Validation** — валидация DTO
- **Docker Compose** — локальная БД

## Быстрый старт

```bash
# 1. Поднять PostgreSQL
docker compose up -d

# 2. Запустить приложение (профиль dev)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Приложение стартует на `http://localhost:8080`.

## Архитектура

### Модель данных

```
Organization (компании)
    └── User (сотрудники компаний)
            │
            ▼
        Lift (лифт) ──── LiftEvent (история событий)
            │
            ▼
        Maintenance (заявки на обслуживание)
            │
            ▼
        Document (прикреплённые файлы)
```

### Слои приложения

```
Controller → Service (interface) → ServiceImpl → Repository → Model
                                        ↕
                                   DTO / Mapper
```

---

## Бизнес-логика

### 1. Organizations — `/api/organizations`

Компании-участники процесса. Каждая организация имеет один тип:

| Тип | Роль в системе |
|-----|---------------|
| `MANUFACTURER` | Завод-изготовитель лифтов |
| `SERVICE` | Сервисная компания (ремонт, ТО) |
| `MANAGEMENT` | Управляющая компания (владеет зданием) |

**Эндпоинты:**

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/organizations` | Список всех организаций |
| `GET` | `/api/organizations/{id}` | Карточка организации |
| `POST` | `/api/organizations` | Зарегистрировать новую компанию |
| `PUT` | `/api/organizations/{id}` | Обновить контакты, адрес |
| `DELETE` | `/api/organizations/{id}` | Удалить (если нет привязанных пользователей/лифтов) |

**Бизнес-правила:**
- Имя организации уникально
- Удаление запрещено, если есть привязанные пользователи или лифты

---

### 2. Users — `/api/users`

Сотрудники организаций. Каждый пользователь принадлежит одной организации и имеет одну или несколько ролей: `ADMIN`, `SERVICE`, `MANAGEMENT`, `MANUFACTURER`.

**Эндпоинты:**

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/users` | Список пользователей |
| `GET` | `/api/users/{id}` | Профиль пользователя (с ролями и организацией) |
| `POST` | `/api/users` | Создать пользователя |
| `PUT` | `/api/users/{id}` | Изменить данные, переназначить роли |
| `DELETE` | `/api/users/{id}` | Удалить (если нет привязанных событий/заявок/документов) |

**Бизнес-правила:**
- `username` и `email` уникальны в системе
- Роли передаются по имени: `{"roleNames": ["SERVICE", "ADMIN"]}`

---

### 3. Lifts — `/api/lifts`

Центральная сущность — конкретный лифт с серийным номером. К лифту привязаны три организации:

```
Lift
├── manufacturerOrganization  → кто произвёл
├── serviceOrganization       → кто обслуживает
└── managementOrganization    → кто владеет (УК)
```

**Эндпоинты:**

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/lifts` | Список всех лифтов (с организациями) |
| `GET` | `/api/lifts/{id}` | Полная карточка лифта |
| `POST` | `/api/lifts` | Зарегистрировать новый лифт |
| `PUT` | `/api/lifts/{id}` | Обновить данные, переназначить организации |
| `DELETE` | `/api/lifts/{id}` | Удалить (если нет событий, заявок, документов) |

**Бизнес-правила:**
- `serialNumber` уникален
- При привязке организации проверяется её тип (нельзя назначить `MANUFACTURER`-организацию в поле `serviceOrganizationId`)
- Статус лифта **не задаётся напрямую** — он вычисляется из последнего события

**Жизненный цикл лифта (статусы):**

```
CREATED → INSTALLED → ACTIVE → FAULTY → IN_REPAIR → ... → DECOMMISSIONED
```

---

### 4. Lift Events — `/api/events`

Журнал событий лифта. Главный механизм управления статусом — каждое событие автоматически меняет статус лифта.

| Тип события | Лифт переходит в статус |
|------------|------------------------|
| `CREATED` | `CREATED` |
| `INSTALLED` | `INSTALLED` |
| `FAULT` | `FAULTY` |
| `REPAIR` | `IN_REPAIR` |

**Эндпоинты:**

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/events` | Все события (хронология системы) |
| `GET` | `/api/events?liftId=5` | События конкретного лифта |
| `GET` | `/api/events/{id}` | Детали события |
| `POST` | `/api/events` | Зафиксировать событие |
| `PUT` | `/api/events/{id}` | Исправить событие |
| `DELETE` | `/api/events/{id}` | Удалить событие |

**Ключевая бизнес-логика:**
- При создании/изменении/удалении события система **автоматически пересчитывает** статус лифта по последнему событию
- Время события не может быть в будущем
- Если `eventAt` не указано — подставляется текущее время
- `performedByUserId` — кто зафиксировал событие (опционально)

---

### 5. Maintenances — `/api/maintenances`

Заявки на техническое обслуживание. Workflow-процесс с тремя стадиями:

```
PENDING (создана) → IN_PROGRESS (техник работает) → DONE (завершена)
```

**Эндпоинты:**

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/maintenances` | Все заявки |
| `GET` | `/api/maintenances?liftId=5` | Заявки по конкретному лифту |
| `GET` | `/api/maintenances/{id}` | Детали заявки |
| `POST` | `/api/maintenances` | Создать заявку |
| `PUT` | `/api/maintenances/{id}` | Обновить (назначить техника, сменить статус) |
| `DELETE` | `/api/maintenances/{id}` | Удалить (если нет документов) |

**Автоматические timestamps:**
- `requestedAt` — ставится автоматически при создании
- `startedAt` — ставится автоматически при переходе в `IN_PROGRESS`
- `completedAt` — ставится автоматически при переходе в `DONE`

---

### 6. Documents — `/api/documents`

Метаданные прикреплённых файлов (акты, протоколы, фото). Документ может быть привязан к лифту, заявке или к обоим.

**Эндпоинты:**

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/documents` | Все документы |
| `GET` | `/api/documents?liftId=5` | Документы лифта (паспорт, сертификаты) |
| `GET` | `/api/documents?maintenanceId=3` | Документы заявки (акт выполненных работ) |
| `GET` | `/api/documents/{id}` | Метаданные документа |
| `POST` | `/api/documents` | Зарегистрировать документ |
| `PUT` | `/api/documents/{id}` | Обновить метаданные |
| `DELETE` | `/api/documents/{id}` | Удалить запись |

---

## Полный сценарий использования

```
1. Создаём организации:
   POST /api/organizations  {"name": "ОтисРус", "type": "MANUFACTURER", ...}
   POST /api/organizations  {"name": "ЛифтСервис", "type": "SERVICE", ...}
   POST /api/organizations  {"name": "УК Домком", "type": "MANAGEMENT", ...}

2. Создаём пользователей:
   POST /api/users  {"username": "admin", "roleNames": ["ADMIN"], ...}
   POST /api/users  {"username": "ivanov", "organizationId": 2, "roleNames": ["SERVICE"], ...}

3. Регистрируем лифт:
   POST /api/lifts  {
     "serialNumber": "OTIS-2024-001",
     "model": "Gen3",
     "manufacturerOrganizationId": 1,
     "serviceOrganizationId": 2,
     "managementOrganizationId": 3
   }
   → Лифт создан со статусом CREATED

4. Фиксируем установку:
   POST /api/events  {"liftId": 1, "type": "INSTALLED", "description": "Монтаж завершён"}
   → Статус лифта автоматически → INSTALLED

5. Поступает сигнал о неисправности:
   POST /api/events  {"liftId": 1, "type": "FAULT", "description": "Застревание кабины"}
   → Статус лифта → FAULTY

6. Создаём заявку на ремонт:
   POST /api/maintenances  {"liftId": 1, "title": "Ремонт привода дверей", "requestedByUserId": 1}
   → Заявка в статусе PENDING

7. Назначаем техника:
   PUT /api/maintenances/1  {"status": "IN_PROGRESS", "assignedTechnicianId": 2, ...}
   → startedAt ставится автоматически

8. Техник прикрепляет акт:
   POST /api/documents  {
     "fileName": "акт.pdf",
     "filePath": "/docs/act-001.pdf",
     "maintenanceId": 1,
     "uploadedByUserId": 2
   }

9. Закрываем заявку:
   PUT /api/maintenances/1  {"status": "DONE", ...}
   → completedAt ставится автоматически

10. Фиксируем ремонт:
    POST /api/events  {"liftId": 1, "type": "REPAIR", "description": "Привод заменён"}
    → Статус лифта → IN_REPAIR
```

## Структура проекта

```
src/main/java/com/smartlift/
├── SmartLiftApplication.java
├── controller/
│   ├── LiftController.java
│   ├── LiftEventController.java
│   ├── OrganizationController.java
│   ├── UserController.java
│   ├── MaintenanceController.java
│   └── DocumentController.java
├── service/
│   ├── LiftService.java
│   ├── LiftEventService.java
│   ├── OrganizationService.java
│   ├── UserService.java
│   ├── MaintenanceService.java
│   ├── DocumentService.java
│   └── impl/
│       ├── LiftServiceImpl.java
│       ├── LiftEventServiceImpl.java
│       ├── OrganizationServiceImpl.java
│       ├── UserServiceImpl.java
│       ├── MaintenanceServiceImpl.java
│       └── DocumentServiceImpl.java
├── repository/
│   ├── LiftRepository.java
│   ├── LiftEventRepository.java
│   ├── OrganizationRepository.java
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   ├── MaintenanceRepository.java
│   └── DocumentRepository.java
├── model/
│   ├── BaseEntity.java
│   ├── Lift.java
│   ├── LiftEvent.java
│   ├── LiftStatus.java
│   ├── LiftEventType.java
│   ├── Organization.java
│   ├── OrganizationType.java
│   ├── User.java
│   ├── Role.java
│   ├── RoleName.java
│   ├── Maintenance.java
│   ├── MaintenanceStatus.java
│   └── Document.java
├── dto/
│   ├── LiftRequest.java / LiftResponse.java
│   ├── LiftEventRequest.java / LiftEventResponse.java
│   ├── OrganizationRequest.java / OrganizationResponse.java
│   ├── OrganizationSummaryResponse.java
│   ├── UserRequest.java / UserResponse.java
│   ├── UserSummaryResponse.java
│   ├── MaintenanceRequest.java / MaintenanceResponse.java
│   ├── DocumentRequest.java / DocumentResponse.java
│   └── ApiErrorResponse.java
├── mapper/
│   └── SmartLiftMapper.java
└── exception/
    ├── ResourceNotFoundException.java
    ├── BadRequestException.java
    ├── ConflictException.java
    └── GlobalExceptionHandler.java
```

## Обработка ошибок

Все ошибки возвращаются в едином формате:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Lift not found: 42",
  "path": "/api/lifts/42"
}
```

| HTTP код | Когда |
|----------|-------|
| `400` | Невалидные данные, нарушение бизнес-правил |
| `404` | Сущность не найдена |
| `409` | Конфликт уникальности или нельзя удалить из-за связей |
| `500` | Непредвиденная ошибка |
