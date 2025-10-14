# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WEEB is a modern instant communication and content management system built with Spring Boot 3.5.4 and Vue 3. It combines real-time chat, group collaboration, article publishing, and social features into a unified platform.

## Development Commands

### Backend (Spring Boot)
```bash
# Development (auto-creates database)
mvn spring-boot:run

# Production build
mvn clean package -DskipTests
java -jar target/weeb-*.jar --spring.profiles.active=prod

# Run tests
mvn test

# Specific test
mvn test -Dtest=UserServiceTest
```

### Frontend (Vue 3 + Vite)
```bash
cd Vue
npm install
npm run dev        # Development with hot reload
npm run build      # Production build
npm run preview    # Preview production build
npm run test       # Run tests
```

## Architecture

### Backend Structure
- **Controller/**: REST API layer (16 controllers)
- **service/**: Business logic layer (36+ services)
- **mapper/**: MyBatis data access layer (16 mappers)
- **model/**: Entity models (17 models)
- **vo/**: View objects (38 VOs)
- **dto/**: Data transfer objects (4 DTOs)
- **Config/**: Configuration classes (13 configs)
- **util/**: Utility classes (11 utilities)
- **annotation/**: Custom annotations (6 annotations)
- **aop/**: Aspect-oriented programming (2 aspects)

### Key Components
- **DatabaseInitializer**: Auto-creates database and tables in development
- **JwtUtil**: JWT token management with environment-based secrets
- **ValidationUtils**: Input validation and SQL injection detection
- **SecurityAuditUtils**: Security event logging and account lockout
- **WebSocketConfig**: Real-time chat with STOMP protocol

### Database Architecture
19+ interconnected tables with proper relationships:
- Core: `user`, `user_stats` (separated for performance)
- Content: `articles`, `article_comment`, `article_category`, `article_tag`
- Communication: `message`, `chat_list`, `group`, `group_member`, `contact`
- Social: `notification`, `user_follow`, `message_reaction`
- Files: `file_record`, `file_share`, `file_transfer`

## Configuration

### Development Environment
- Uses default configurations in `application.yml`
- Auto-creates database and tables
- MySQL 8.0+, Redis 7.0+, Elasticsearch 8.x required
- Debug logging enabled

### Production Environment
- Requires environment variables for all sensitive data
- Enforces security validation on startup via `EnvironmentValidator`
- Use `--spring.profiles.active=prod` to enable production mode

## Key Development Patterns

### Service Layer Pattern
All business logic resides in service classes with clear separation of concerns. Services handle:
- Business rules and validation
- Transaction management
- Integration with multiple mappers
- Exception handling and logging

### Security First Approach
- All inputs validated through `ValidationUtils`
- SQL injection protection via `SqlInjectionUtils`
- JWT-based authentication with refresh tokens
- Account lockout after failed attempts
- XSS protection and content filtering
- Audit logging for security events

### Error Handling
Global exception handling via `GlobalExceptionHandler` with consistent API responses using `Result<T>` wrapper class.

### Performance Optimization
- Redis caching with `RedisCacheManager`
- Caffeine local cache for frequently accessed data
- Connection pooling for database and Redis
- Async processing for heavy operations

## Testing

The project includes comprehensive test coverage:
- **Integration Tests**: 8 test classes covering core functionality
- **Mapper Tests**: 3 mapper-specific tests
- **Performance Tests**: 3 benchmark tests
- **Database Tests**: Schema validation and initialization

Run tests with `mvn test`. Individual test classes can be run with `mvn test -Dtest=ClassName`.

## Frontend Architecture

Vue 3 with Composition API:
- **Pinia**: State management
- **Element Plus**: UI components
- **Axios**: HTTP client with interceptors
- **Vue Router 4**: Client-side routing
- **Vite**: Build tool with HMR

The frontend follows a component-based architecture with clear separation between presentation, business logic, and data layers.