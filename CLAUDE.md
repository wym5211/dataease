# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DataEase is an open-source BI (Business Intelligence) tool built with:
- **Frontend**: Vue.js 3 + Element Plus + Vite
- **Backend**: Spring Boot 3.3 + Java 21
- **Database**: MySQL (standalone/distributed mode) or H2 (desktop mode)
- **Data Processing**: Apache Calcite, Apache SeaTunnel

## Project Structure

```
dataease/
├── core/
│   ├── core-backend/          # Spring Boot backend
│   │   ├── src/main/java/io/dataease/
│   │   │   ├── substitute/    # Substitute implementations for community edition
│   │   │   └── ...
│   │   └── src/main/resources/
│   │       ├── application.yml              # Base config
│   │       ├── application-standalone.yml   # Standalone mode (MySQL)
│   │       ├── application-desktop.yml      # Desktop mode (H2)
│   │       ├── application-distributed.yml  # Enterprise mode
│   │       └── db/
│   │           ├── migration/   # Flyway migrations for standalone
│   │           └── desktop/     # Flyway migrations for desktop
│   └── core-frontend/         # Vue.js frontend
│       ├── src/
│       └── package.json
├── sdk/
│   ├── api/                   # API definitions
│   ├── common/                # Common utilities, filters, auth
│   ├── distributed/           # Enterprise distributed components
│   └── extensions/            # Extension points
└── de-xpack/                  # Enterprise extensions (separate module)
```

## Build Commands

### Prerequisites
- Java 21 (Eclipse Temurin recommended)
- Maven 3.8+
- Node.js 20+
- MySQL 8.0 (for standalone mode)

### Tool Paths (Windows)

```bash
# Java 21
JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot"

# Maven (bundled with IntelliJ IDEA)
MAVEN_PATH="C:\Program Files\JetBrains\IntelliJ IDEA 2023.2.1\plugins\maven\lib\maven3\bin"

# Add to PATH
export PATH="$JAVA_HOME\bin:$MAVEN_PATH:$PATH"
```

**Build with specific Java/Maven:**
```bash
cd core/core-backend
export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot"
export PATH="$JAVA_HOME\bin:$PATH"
"C:\Program Files\JetBrains\IntelliJ IDEA 2023.2.1\plugins\maven\lib\maven3\bin\mvn" clean package -DskipTests
```

### Backend Build

```bash
# Build entire project (skip tests)
mvn clean package -DskipTests

# Build core-backend only
cd core/core-backend
mvn clean package -DskipTests

# Build with specific profile (standalone is default)
mvn clean package -DskipTests -P standalone
mvn clean package -DskipTests -P desktop
mvn clean package -DskipTests -P distributed
```

### Frontend Build

```bash
cd core/core-frontend

# Install dependencies
npm install

# Development server
npm run dev          # Linux/Mac
npm run dev:win      # Windows

# Build for production
npm run build:base
```

### Full Build (Frontend + Backend)

```bash
# 1. Build frontend first
cd core/core-frontend
npm install
npm run build:base

# 2. Build backend (automatically copies frontend dist to static resources)
cd ../core-backend
mvn clean package -DskipTests
```

## Running the Application

### Standalone Mode (MySQL)

```bash
# 1. Start MySQL and create database
create database dataease10 character set utf8mb4;

# 2. Update application-standalone.yml with correct MySQL credentials

# 3. Run
cd core/core-backend
java -jar target/CoreApplication.jar --spring.profiles.active=standalone
```

### Desktop Mode (H2)

```bash
cd core/core-backend
java -jar target/CoreApplication.jar --spring.profiles.active=desktop
```

### Development Mode (Frontend + Backend separately)

```bash
# Terminal 1: Backend
cd core/core-backend
java -jar target/CoreApplication.jar --spring.profiles.active=standalone

# Terminal 2: Frontend
cd core/core-frontend
npm run dev:win
# Frontend: http://localhost:8081
# Backend: http://localhost:8100
```

## Default Credentials

- Username: `admin`
- Password: `DataEase@123456`

## Architecture Patterns

### Profile-Based Deployment

The project uses Spring profiles for different deployment modes:

1. **desktop** (`application-desktop.yml`):
   - Uses H2 embedded database
   - Simplified authentication (substitute implementations)
   - For local development/single-user

2. **standalone** (`application-standalone.yml`):
   - Uses MySQL database
   - Includes frontend static resources
   - Default profile

3. **distributed** (`application-distributed.yml`):
   - Enterprise edition
   - Excludes substitute implementations
   - Requires `de-xpack` module

### Authentication Flow

```
TokenFilter (order=0)
    ↓
CommunityTokenFilter (order=5) - validates JWT
    ↓
Login API: /de2api/login/localLogin
```

Key classes:
- `TokenFilter` - First filter, checks desktop mode and whitelists
- `CommunityTokenFilter` - JWT validation for community edition
- `WhitelistUtils` - Defines public paths (login, static resources, etc.)
- `ModelUtils` - Detects running mode (desktop/standalone/distributed)

### Substitute Pattern (Community Edition)

The `substitute` package provides fallback implementations when enterprise features are not available:

```
core-backend/src/main/java/io/dataease/substitute/
├── permissions/
│   └── login/
│       └── SubstituleLoginServer.java  # Simple admin login
└── ...
```

These are excluded from distributed (enterprise) builds via Maven compiler exclusion.

### Frontend Structure

```
core-frontend/src/
├── api/           # API client functions
├── assets/        # Static assets
├── components/    # Vue components
├── layouts/       # Page layouts
├── router/        # Vue Router config
├── stores/        # Pinia stores
├── utils/         # Utilities (request, cache, etc.)
├── views/         # Page views
└── websocket/     # WebSocket/STOMP client
```

## Key Configuration Files

| File | Purpose |
|------|---------|
| `core/core-backend/src/main/resources/application-standalone.yml` | Standalone mode config (MySQL) |
| `core/core-backend/src/main/resources/application-desktop.yml` | Desktop mode config (H2) |
| `core/core-frontend/.env.dev` | Frontend dev environment |
| `pom.xml` (root) | Parent POM with dependency versions |

## Testing

```bash
# Backend tests (skipped by default in pom.xml)
mvn test

# Run specific test
mvn test -Dtest=TestClassName

# Frontend linting
cd core/core-frontend
npm run lint
npm run lint:stylelint
```

## Important Notes

1. **Maven Profile Activation**: The `standalone` profile is active by default. To build enterprise edition, explicitly activate `distributed` profile.

2. **Frontend Integration**: The `standalone` profile copies frontend `dist/` folder to `src/main/resources/static/` during build.

3. **Token Validation**: In desktop mode, `TokenFilter` detects via `ModelUtils.isDesktop()` and bypasses full JWT validation.

4. **Database Migrations**: Flyway manages schema migrations. Desktop and standalone modes have separate migration locations:
   - Desktop: `classpath:db/desktop`
   - Standalone: `classpath:db/migration`
