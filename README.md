# Интеграция фаззинг-тестирования в CI/CD

Демонстрационный проект с интеграцией фаззинг-тестирования Web-приложений в конвейеры **GitLab CI** и **GitHub Actions**. Настроено по аналогии с DAST-тестированием.

**Стек:** Java 17, Spring Boot 3.2

## Структура проекта

```
.
├── pom.xml
├── src/main/java/com/demo/
│   ├── FuzzTestApplication.java
│   └── controller/ApiController.java
├── src/main/resources/application.properties
├── Dockerfile
├── .gitlab-ci.yml      # GitLab CI с этапом fuzz
├── .github/
│   └── workflows/
│       └── fuzz-test.yml   # GitHub Actions workflow
└── README.md
```

## Используемые инструменты

| Платформа   | Инструмент      | Описание                                      |
|------------|-----------------|-----------------------------------------------|
| GitLab CI  | OWASP ZAP       | Полное сканирование (spider + active scan)   |
| GitHub     | OWASP ZAP       | action-full-scan — DAST и фаззинг параметров   |

OWASP ZAP выполняет:
- **Spider** — обход приложения
- **Active Scan** — фаззинг параметров, поиск XSS, SQLi и др.
- **Отчёты** — HTML/Markdown с найденными уязвимостями

## GitLab CI

### Требования

- GitLab Runner с Docker executor
- Включён Container Registry проекта

### Этапы пайплайна

1. **build** — сборка Docker-образа и push в registry
2. **fuzz** — запуск приложения как сервиса и сканирование OWASP ZAP

### Результаты

- Артефакты: `zap-report.html`, `zap-report.md`
- `allow_failure: true` — пайплайн не падает при обнаружении уязвимостей

### Вариант без Container Registry

Если registry недоступен, можно использовать вариант с локальной сборкой в одном job:

```yaml
fuzz_local:
  stage: fuzz
  image: docker:24
  services:
    - docker:24-dind
  variables:
    DOCKER_TLS_CERTDIR: "/certs"
  script:
    - docker build -t webapp:test .
    - docker run -d --name app -p 5000:5000 webapp:test
    - apk add --no-cache curl && sleep 5 && curl -f http://localhost:5000/health
    - docker run --rm --network host ghcr.io/zaproxy/zaproxy:stable zap-full-scan.py -t http://localhost:5000 -r report.html -I
  artifacts:
    when: always
    paths: [report.html]
```

## GitHub Actions

### Триггеры

- Push в `main`/`master`
- Pull Request в `main`/`master`
- Расписание: еженедельно (воскресенье, 02:00 UTC)
- Ручной запуск (`workflow_dispatch`)

### Этапы

1. **build** — сборка образа, сохранение в artifact
2. **fuzz** — загрузка образа, запуск приложения, ZAP Full Scan

### Результаты

- Артефакт `zap-report` с HTML-отчётом (если action его создаёт)
- ZAP может создавать issue с результатами сканирования

## Локальный запуск

### Приложение (Maven)

```bash
./mvnw spring-boot:run
# или
mvn spring-boot:run
# Приложение: http://localhost:5000
```

### OWASP ZAP (Docker)

```bash
docker run -t ghcr.io/zaproxy/zaproxy:stable zap-full-scan.py -t http://host.docker.internal:5000 -r report.html
```

### ffuf (альтернативный фаззер)

```bash
# Установка: go install github.com/ffuf/ffuf/v2@latest
ffuf -u "http://localhost:5000/api/users?id=FUZZ" -w wordlist.txt -mc 200
```

## Ссылки

- [GitLab: Web API Fuzzing](https://docs.gitlab.com/user/application_security/api_fuzzing/) (Ultimate)
- [OWASP ZAP](https://www.zaproxy.org/)
- [ZAP Full Scan GitHub Action](https://github.com/zaproxy/action-full-scan)
- [ffuf](https://github.com/ffuf/ffuf)
