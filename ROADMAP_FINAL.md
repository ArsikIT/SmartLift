# SmartLift Final Exam Roadmap

Источник требований: `Final.pdf` (Endterm & Final Exam: SRE Capstone Project - Production Readiness Review).

Цель финала: довести SmartLift до состояния production-readiness review с обязательными блоками:
- IaC
- CI/CD
- Observability + Alerting
- SRE Operations: SLI/SLO, autoscaling, load testing
- Документация, demo и defense

Ниже план идет строго в порядке, который задан в `Final.pdf`.

## 0. Стартовая фиксация текущего состояния

Перед началом основной реализации нужно зафиксировать baseline, чтобы дальше не смешивать доработки платформы и доработки SRE-части.

Что уже есть в репозитории:
- `backend/` и `frontend/`
- контейнеризация через Docker
- корневой `docker-compose.yml`
- `observability/prometheus/*`
- `observability/grafana/*`
- Spring Boot Actuator + Prometheus endpoint
- тесты backend

Что отсутствует или не доведено до финального требования:
- отдельная `terraform/` директория
- полноценный CI/CD pipeline в `.github/workflows/`
- deployment в кластер с автодеплоем
- Alertmanager в инфраструктуре
- формально зафиксированные SLI/SLO
- autoscaling на уровне кластера
- load testing сценарий и артефакты
- финальный отчет и пакет доказательств для защиты

Результат этапа:
- создать рабочую ветку под финал
- зафиксировать текущую архитектуру в 1 схеме
- составить список артефактов, которые должны появиться к защите

## 1. Infrastructure as Code (IaC) and Configuration

Это первый обязательный этап по PDF. До CI/CD, observability и нагрузочных тестов инфраструктура должна стать воспроизводимой с нуля.

### 1.1. Определить целевую среду развертывания

Нужно выбрать один реальный target и больше не распыляться:
- `k3s` / `kubernetes` на VPS
- локальный `minikube` / `kind` для демо
- managed Kubernetes, если есть доступ

Для финала оптимальный путь:
- локально: `kind` или `minikube` для воспроизводимого демо
- структура Terraform сразу закладывается так, чтобы можно было перенести на VPS/cloud

### 1.2. Создать директорию `terraform/`

Минимальная структура:
- `terraform/providers.tf`
- `terraform/versions.tf`
- `terraform/variables.tf`
- `terraform/outputs.tf`
- `terraform/main.tf`
- `terraform/terraform.tfvars.example`
- `terraform/environments/dev/*`
- `terraform/environments/prod/*`

### 1.3. Описать инфраструктуру в Terraform

Нужно покрыть:
- namespace для приложения
- namespace для observability
- secrets/configs
- backend deployment
- frontend deployment
- postgres deployment/statefulset или внешний managed postgres
- services/ingress
- prometheus/grafana/alertmanager deployment

Если Terraform будет только провиженить кластер, а манифесты будут отдельно, это слабее для защиты. Лучше:
- либо Terraform + Helm releases
- либо Terraform + Kubernetes provider

### 1.4. Управление переменными и state

Обязательно закрыть требования PDF:
- вынести все environment-specific значения в variables
- не хранить секреты в коде
- подготовить `terraform.tfvars.example`
- описать backend для state

Минимально допустимо для демо:
- локальный state для разработки

Лучше для финала:
- remote state, если есть доступ к backend

### 1.5. Проверка полной воспроизводимости

Нужно пройти сценарий:
1. пустая среда
2. `terraform init`
3. `terraform plan`
4. `terraform apply`
5. приложение и observability поднимаются без ручных шагов

Deliverable этапа:
- готовая директория `terraform/` со всеми `.tf` файлами
- скриншоты `terraform plan/apply`
- архитектурная схема инфраструктуры

## 2. Continuous Integration and Deployment (CI/CD)

Это второй обязательный этап по PDF. Начинать его нужно только после того, как IaC уже определен и есть понятная целевая среда доставки.

### 2.1. Выбрать CI/CD платформу

Наиболее прямой вариант для этого репозитория:
- GitHub Actions

### 2.2. Подготовить Docker build path

Перед pipeline нужно стабилизировать сборку:
- проверить multi-stage Dockerfile backend
- проверить Dockerfile frontend
- убедиться, что образы собираются без ручных зависимостей
- привести теги образов к единой схеме: `sha`, `latest`, `release`

### 2.3. Создать workflow в `.github/workflows/`

Минимальный pipeline:
1. checkout
2. backend tests
3. frontend build/test if applicable
4. docker build backend
5. docker build frontend
6. push image в registry
7. deploy в cluster

### 2.4. Подключить registry

Подойдет:
- GitHub Container Registry
- Docker Hub

Нужно настроить:
- secrets для auth
- naming convention для образов
- version tagging

### 2.5. Автоматический deployment

Требование PDF: updates должны автоматически деплоиться в cluster.

Значит в pipeline должен быть отдельный deploy job:
- `kubectl apply`
- или `helm upgrade --install`
- или deployment через Terraform/Helm step

Минимально корректный вариант:
- после успешного build/push workflow обновляет deployment image tag в кластере

### 2.6. Зафиксировать успешный end-to-end прогон

Нужно подготовить доказательства:
- скриншот успешного workflow
- скриншот обновления pod/deployment после push
- короткое описание триггера: push в main или release branch

Deliverable этапа:
- pipeline configuration file
- screenshots успешного выполнения
- описание deployment flow

## 3. Observability and Alerting

Это третий обязательный этап по PDF. В репозитории уже есть задел, но его нужно довести до защитимого production-readiness состояния.

### 3.1. Провести аудит текущего observability слоя

Проверить и подтвердить:
- backend отдает `/actuator/prometheus`
- Prometheus реально скрапит backend
- Grafana читает Prometheus datasource
- текущий dashboard отражает ключевые метрики

### 3.2. Довести Prometheus конфигурацию

Нужно обеспечить scraping минимум для:
- backend application metrics
- node/container metrics
- при наличии k8s: kube-state-metrics / cadvisor / ingress metrics

### 3.3. Формализовать SLI-ориентированные дашборды

Grafana должна показывать не просто CPU/RAM, а именно сигналы надежности:
- availability
- request rate
- error rate
- latency p50/p95/p99
- saturation CPU/memory
- количество активных инстансов
- бизнес-метрики, если есть смысл: число maintenance/event операций

### 3.4. Добавить Alertmanager в инфраструктуру

Сейчас в проекте есть alert rules, но финальное требование PDF прямо говорит про Alertmanager rules и alert evidence.

Нужно:
- развернуть Alertmanager
- подключить его к Prometheus
- настроить минимум один receiver

Для демо достаточно:
- webhook
- email
- Telegram, если это уже готово и стабильно

### 3.5. Реализовать минимальный набор alert rules

Обязательный базовый набор:
- backend down
- high error rate
- high latency
- high CPU/memory
- postgres unavailable
- отсутствие scrape targets

### 3.6. Подготовить доказательства observability

Нужно заранее получить:
- screenshot Grafana dashboard
- screenshot firing alert
- screenshot resolved alert

Deliverable этапа:
- dashboard screenshots
- alert screenshots
- финальные конфиги Prometheus/Grafana/Alertmanager

## 4. SRE Operations: SLOs, Scaling and Testing

Это четвертый обязательный этап по PDF. Делать его раньше observability нельзя, потому что SLO и scaling надо измерять через метрики.

### 4.1. Зафиксировать SLIs

Для SmartLift рационально взять:
- Availability: доля успешных запросов backend
- Latency: p95 времени ответа для API
- Error Rate: доля 5xx/4xx по ключевым endpoint
- Throughput: requests per second

### 4.2. Зафиксировать SLOs

Пример рабочего набора:
- Availability SLO: 99.0% или 99.5%
- Latency SLO: 95% запросов быстрее 500 ms
- Error Rate SLO: не более 1% 5xx

Важно:
- SLO должны быть реалистичны под вашу среду
- каждое SLO должно быть измеримо из Grafana/Prometheus

### 4.3. Реализовать auto-scaling policies

По PDF нужен именно scaling.

Если deployment идет в Kubernetes, нужен HPA:
- scale by CPU
- при возможности scale by memory
- при более сильной защите: custom metrics через Prometheus Adapter

Минимальный путь:
- HPA для backend
- если frontend stateless, можно оставить фиксированным или тоже масштабировать

### 4.4. Подготовить load-testing сценарий

Нужно выбрать инструмент:
- `Locust` предпочтительнее
- `ab` допустим, но слабее для демонстрации

Сценарии нагрузки должны отражать реальный продукт:
- login
- получение списка lifts
- создание maintenance
- просмотр events

### 4.5. Провести нагрузочный тест и показать scaling

Целевой сценарий для защиты:
1. старт с 1 pod backend
2. запуск нагрузки
3. рост CPU/latency
4. срабатывание HPA
5. рост числа реплик
6. стабилизация latency/error rate

### 4.6. Зафиксировать артефакты SRE-этапа

Нужно собрать:
- screenshot HPA
- screenshot увеличения числа pod
- screenshot нагрузки из Locust
- screenshot метрик до/во время/после spike

Deliverable этапа:
- зафиксированные SLI/SLO
- autoscaling manifests/configuration
- load testing scripts
- screenshots scaling during traffic spikes

## 5. Documentation Package

Хотя в PDF документация идет в Submission Instructions, по факту ее нужно собирать сразу после завершения технических блоков.

### 5.1. Подготовить структуру финального отчета

Отчет должен включать:
- цель проекта
- архитектуру системы
- инфраструктурную схему
- описание Terraform
- описание CI/CD
- observability стек
- SLI/SLO
- scaling strategy
- load testing results
- screenshots
- вывод по production readiness

### 5.2. Очистить и структурировать репозиторий

Нужно, чтобы GitHub repo выглядел как deliverable:
- понятный root README
- разделы `backend/`, `frontend/`, `observability/`, `terraform/`, `load-tests/`, `.github/workflows/`
- инструкции запуска
- инструкции деплоя
- инструкции демо

### 5.3. Подготовить артефакты в одном списке

Чеклист артефактов:
- public GitHub repository
- report PDF
- terraform files
- CI/CD config
- dashboard screenshots
- alert screenshots
- scaling screenshots
- demo script

## 6. Live Demo and Defense Preparation

Это не отдельный технический этап в rubric, но по баллам он самый тяжелый, поэтому его надо готовить как часть финального роадмапа.

### 6.1. Подготовить demo flow

Рекомендуемая последовательность демо:
1. показать архитектуру
2. показать Terraform структуру
3. показать CI/CD workflow
4. показать running system
5. показать Grafana dashboard
6. запустить load test
7. показать autoscaling
8. показать alert
9. завершить выводом по SLO/production readiness

### 6.2. Подготовить defense questions

Нужно уметь ответить:
- почему выбрали эти SLI/SLO
- почему такой способ деплоя
- как хранится Terraform state
- как работает rollback
- почему алерты именно такие
- как доказывается reproducibility
- где узкие места системы

## Строгий порядок реализации

Ниже итоговый порядок без перестановок:

1. Зафиксировать baseline проекта и целевую среду.
2. Создать `terraform/` и описать всю инфраструктуру как код.
3. Добиться полного воспроизводимого развертывания с нуля.
4. Подготовить и стандартизировать Docker build для backend/frontend.
5. Создать CI/CD pipeline.
6. Подключить image registry.
7. Автоматизировать deploy в cluster.
8. Проверить end-to-end delivery после push.
9. Довести Prometheus scraping.
10. Довести Grafana dashboards под SLI.
11. Развернуть и подключить Alertmanager.
12. Настроить alert rules и получить firing/resolved alerts.
13. Формально определить SLIs.
14. Формально определить SLOs.
15. Настроить autoscaling policies.
16. Написать load testing сценарии.
17. Провести нагрузочный тест и зафиксировать scaling behavior.
18. Собрать технический отчет с архитектурой, SLO, scaling strategy и скриншотами.
19. Привести репозиторий в чистый публичный deliverable-вид.
20. Подготовить live demo script и defense answers.

## Практический приоритет для SmartLift

Если делать это именно для текущего репозитория без лишних развилок, оптимальная реализация такая:
- Kubernetes как целевая среда
- Terraform для namespaces, secrets, deploy/helm releases
- GitHub Actions как CI/CD
- GHCR как registry
- Prometheus + Grafana + Alertmanager в `observability/`
- HPA для backend
- Locust для нагрузочного теста

## Definition of Done

Финал можно считать завершенным только если одновременно выполнено все ниже:
- инфраструктура поднимается с нуля по IaC
- pipeline автоматически собирает, пушит и деплоит
- observability стек показывает рабочие метрики и SLI
- alerts реально срабатывают
- autoscaling реально увеличивает реплики под нагрузкой
- есть артефакты и скриншоты для каждого обязательного deliverable из PDF
- есть готовый PDF report и сценарий live demo
