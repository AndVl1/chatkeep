# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Chatkeep** is a Spring Boot application written in Kotlin. It's a reactive web service that uses:
- Spring Boot 4.0.1
- Kotlin 2.2.21
- Spring Data JDBC for database access
- Spring WebClient and RestClient for HTTP communication
- Kotlin coroutines with Reactor for reactive programming

## Common Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Clean and build
./gradlew clean build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "ru.andvl.chatkeep.ChatkeepApplicationTests"

# Run tests with more detailed output
./gradlew test --info

# Run tests continuously (useful during development)
./gradlew test --continuous
```

### Other Gradle Tasks
```bash
# List all available tasks
./gradlew tasks

# Check for dependency updates
./gradlew dependencyUpdates

# Generate a bootJar
./gradlew bootJar
```

## Architecture

### Technology Stack
- **Language**: Kotlin with strict compiler options (JSR-305 strict mode)
- **Framework**: Spring Boot 4.0.1 with Spring Data JDBC
- **Reactive Programming**: Project Reactor with Kotlin coroutines support
- **HTTP Clients**: Both RestClient (blocking) and WebClient (reactive) are available
- **Java Version**: Java 17
- **JSON**: Jackson with Kotlin module support

### Package Structure
- Base package: `ru.andvl.chatkeep`
- All application code is under `src/main/kotlin/ru/andvl/chatkeep/`
- Tests are under `src/test/kotlin/ru/andvl/chatkeep/`

### Key Dependencies
The application has both blocking (Spring Data JDBC, RestClient) and reactive (WebClient, Reactor) capabilities, allowing for hybrid approaches where needed.

## Development Notes

### Kotlin Compiler Configuration
The project uses strict compiler settings:
- JSR-305 strict mode enabled
- Parameter property annotation target set by default

### Testing Framework
- JUnit 5 (Jupiter)
- Spring Boot Test with `@SpringBootTest`
- Kotlinx Coroutines Test support