# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build         # Build the project
./gradlew bootRun       # Run the application (port 8083)
./gradlew test          # Run all tests
./gradlew clean         # Clean build artifacts
./gradlew bootJar       # Create executable JAR
```

To run a single test class:
```bash
./gradlew test --tests "com.example.imgserver.api.ImageRestControllerIntegrationTest"
```

## Architecture

This is a Spring Boot 3.5 / Java 21 image upload and serving server.

**Layered structure:**
```
REST Controllers (api/)
    ↓
Business Logic (service/ImageService)
    ↓
Storage Abstraction (storage/StorageService interface)
    ├── LocalStorageService  — local filesystem (current)
    └── MinioStorageService  — S3-compatible object storage (stub)
```

**Key design decisions:**

- **Storage strategy pattern**: `StorageService` interface allows switching backends via `image.storage` config property. `LocalStorageService` is the active implementation; `MinioStorageService` is a future stub.

- **File organization**: Images are stored with a date-based hierarchy — `{uploadDir}/{project}/{year}/{month}/{day}/{uuid}.{ext}`. The UUID-based filename prevents collisions and obscures originals.

- **Upload vs. retrieval split**: Upload goes through Spring Boot. In production, static file retrieval is served directly by Nginx (bypassing Spring Boot), using `X-Accel-Redirect` header for download endpoints. The Spring Boot preview/download endpoints exist as fallback or for development.

- **Exception hierarchy**: `GlobalExceptionHandler` maps domain exceptions to HTTP status codes — `InvalidFileException` → 400, `ImageNotFoundException` → 404, `ImageUploadException` → 500.

## Configuration (`application.yaml`)

| Property | Default | Purpose |
|---|---|---|
| `image.dir` | `D:/images` | Local filesystem upload root |
| `image.url-prefix` | `http://localhost:8083/api/v1/images/preview` | Prefix for returned image URLs |
| `server.port` | `8083` | HTTP port |
| `spring.servlet.multipart.max-file-size` | `20MB` | Max file size |

Production deployment uses `/var/www/images` as the image directory and a public IP/domain for `url-prefix`.

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/images?project={name}` | Upload image; returns `{ fileName, url }` |
| `GET` | `/api/v1/images/preview/{project}/{y}/{m}/{d}/{filename}` | Serve image inline |
| `GET` | `/api/v1/images/download/{project}/{y}/{m}/{d}/{filename}` | Serve image as attachment |

**Allowed extensions:** `.jpg`, `.jpeg`, `.png`, `.gif`, `.bmp`, `.webp`

## Logging

Logback is configured via `logback-spring.xml` with rolling file appenders (10MB/file, 30-day retention). Profile `dev`/`local` sets `DEBUG` level for application packages; `prod` uses `INFO`.
