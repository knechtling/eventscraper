# eventscraper

A Spring Boot app that scrapes upcoming events in Dresden, stores them, and renders a simple UI with server-side filtering and pagination.

## Quick start (dev)
- Build and run (dev, H2):
  - `./mvnw -q spring-boot:run`
- Package jar:
  - `./mvnw -q clean package`

## Server-side filters (UI)
The homepage `/` supports these query parameters and persists them across pagination:
- `search`  — free-text in title/description
- `start`   — start date (dd.MM.yyyy)
- `end`     — end date (dd.MM.yyyy), optional
- `loc`     — exact location match (e.g., `Chemiefabrik`, `Hanse 3`, `Scheune Dresden`)
- `page`    — zero-based page index (default 0)
- `size`    — page size (default 24)

Examples:
- `/?start=01.09.2025&end=30.09.2025`
- `/?loc=Chemiefabrik`
- `/?search=punk&loc=Hanse%203`

## Docker Compose (Postgres + app, prod profile)
A compose setup is provided to run Postgres and the app in containers:

1) Build the app jar
- `./mvnw -q -DskipTests package`

2) Start containers
- `docker compose up --build`

Defaults in `docker-compose.yml`:
- DB: postgres:16-alpine, database `eventscraper`, user `events`, password `events`
- App: profile `prod`, connects to `jdbc:postgresql://db:5432/eventscraper`, exposes `8080`

Stop with `docker compose down`.

## Tests and scheduler
- Tests use an in-memory H2 database.
- The scraping scheduler is silenced during tests to avoid background scraping and DB lock noise by setting:
  - `spring.task.scheduling.enabled=false`
  - `spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration`

## Notes
- Flyway migration creates `event` and `scrape_run` tables and an index on `event.date`.
- Actuator exposes basic health/info endpoints.
