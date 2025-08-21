# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

Repository overview
- Stack: Java 17, Spring Boot 3 (web, Thymeleaf, data-jpa), H2 (in‑memory), Jsoup, ical4j
- Build tool: Maven (via Maven Wrapper ./mvnw)
- App type: Server‑rendered Spring MVC with Thymeleaf templates and static assets
- Docker: Builds a runtime image from the shaded/packaged jar

Common commands
- Build (compile + unit tests)
  ./mvnw -q clean verify

- Build without tests
  ./mvnw -q -DskipTests clean package

- Run the application (dev)
  ./mvnw -q spring-boot:run
  • Default port: 8080 (see src/main/resources/application.properties and Dockerfile)

- Package runnable jar
  ./mvnw -q clean package
  java -jar target/eventscraper-0.0.1-SNAPSHOT.jar

- Run a single test class
  ./mvnw -q -Dtest=SomeTestClass test

- Run tests matching a pattern
  ./mvnw -q -Dtest="*ServiceTest" test

- Generate dependency tree (debug builds)
  ./mvnw -q dependency:tree

Docker
- Build image (requires the jar in target/)
  ./mvnw -q -DskipTests package
  docker build -t eventscraper:local .

- Run container on port 8080
  docker run --rm -p 8080:8080 eventscraper:local

High‑level architecture and structure
- Spring Boot application (conventional layout)
  • Application and controllers are expected under src/main/java/... (none are currently present in the repo). The app likely defines MVC controllers that render Thymeleaf templates and possibly REST endpoints for scraping/feeds.
  • Dependencies suggest: 
    - Web + Thymeleaf for server‑side rendering.
    - Data JPA + H2 for ephemeral persistence during runtime (in‑memory DB). No schema files are present; expect entity auto‑DDL if entities exist later.
    - Jsoup for HTML parsing/scraping of event pages.
    - ical4j for exporting events as iCalendar (.ics) feeds.

- View layer (present in repo)
  • Templates: src/main/resources/templates/
    - welcome.html: main listing page with search, date range filter (Flatpickr), and location filter; renders events with lazy‑loaded thumbnails and links to details.
    - eventDetails.html: detail view for a single event (title, location, date, times, price, description, misc) and back navigation.
    - fragments/: navbar.html and darkmode-button.html (toggle stored in localStorage).
  • Static assets: src/main/resources/static/
    - css/: styles.css (layout, grid cards) and dark-mode.css (theme overrides).
    - js/: lazyLoadImages.js (IntersectionObserver to progressively load event thumbnails).
    - images/: placeholder.webp used as default thumbnail background.

- Configuration
  • src/main/resources/application.properties
    - spring.application.name=eventscraper
    - H2 in‑memory datasource (jdbc:h2:mem:testdb); username=sa, password=password
    - Disables system CPU metrics
  • Dockerfile
    - FROM openjdk:17-jdk-slim; copies target/eventscraper-0.0.1-SNAPSHOT.jar to app.jar and runs java -jar app.jar on port 8080.

- Expected runtime flow (given current assets and dependencies)
  1) Controller for "/" queries repository/service for events, passes list to welcome.html; client filters by date range and location using Flatpickr + JS.
  2) Controller for "/event/details/{id}" renders eventDetails.html for a selected event.
  3) Services likely use Jsoup to scrape upstream sources and map into domain objects; optional export endpoint may use ical4j to produce .ics.

Notes for Warp
- There are currently no Java source files (src/main/java) or tests (src/test/java) in the repository. Build and run commands will succeed only after sources are added.
- No lint/format/static‑analysis plugins are configured (e.g., Checkstyle/SpotBugs/Spotless). If needed, add corresponding Maven plugins and document their usage here.
- README.md is minimal and does not override any of the instructions above. No CLAUDE/Cursor/Copilot rules are present in the repository.

