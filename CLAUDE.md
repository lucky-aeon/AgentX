# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Architecture

AgentX is an intelligent agent platform built with Domain-Driven Design (DDD) architecture, featuring:

**Backend (Java/Spring Boot)**:
- **Domain Layer**: Core business logic with entities, repositories, and domain services
- **Application Layer**: Business process orchestration and use cases 
- **Infrastructure Layer**: Technical implementations (persistence, configs, utilities)
- **Interface Layer**: API controllers and external interfaces

**Frontend (Next.js/TypeScript)**:
- **Pages**: Route-based page components using Next.js App Router
- **Components**: Reusable UI components with Tailwind CSS and Shadcn/ui
- **Lib**: API services, HTTP client, and utility functions
- **Types**: TypeScript type definitions
- **Contexts**: React Context for state management

**Key Technologies**:
- Backend: Spring Boot 3.2.3, MyBatis-Plus, PostgreSQL, JWT
- Frontend: Next.js 15, React 19, TypeScript, Tailwind CSS
- Infrastructure: Docker, Maven, Gradle

## Development Commands

### Backend (Java/Spring Boot)
```bash
# Navigate to backend directory
cd AgentX

# Run backend server (development)
./mvnw spring-boot:run

# Build backend
./mvnw clean compile

# Run tests
./mvnw test

# Code formatting (uses Spotless plugin)
./mvnw spotless:apply
```

### Frontend (Next.js)
```bash
# Navigate to frontend directory  
cd agentx-frontend-plus

# Install dependencies
npm install --legacy-peer-deps

# Run development server
npm run dev

# Build for production
npm run build

# Start production server
npm start

# Lint code
npm run lint
```

### Docker Development (Recommended)
```bash
# Start all services in development mode (with hot reload)
./bin/start-dev.sh     # Linux/macOS
bin\start-dev.bat      # Windows

# Start production mode
./bin/start.sh         # Linux/macOS  
bin\start.bat          # Windows

# Stop services
./bin/stop.sh          # Linux/macOS
bin\stop.bat           # Windows

# View service status
docker compose -f docker-compose.dev.yml ps

# View logs
docker compose -f docker-compose.dev.yml logs -f [service-name]
```

### Database
```bash
# Setup PostgreSQL with Docker
cd script
./setup_with_compose.sh  # Linux/macOS
setup_with_compose.bat   # Windows
```

## Code Architecture Patterns

### Backend DDD Structure
- **Entities**: Core business objects with identity (e.g., `AgentEntity`, `UserEntity`)
- **Aggregates**: Clusters of related entities (e.g., `TaskAggregate`) 
- **Domain Services**: Business logic that doesn't naturally fit in entities
- **Repositories**: Data access abstractions using MyBatis-Plus
- **Application Services**: Orchestrate use cases and coordinate domain objects
- **Assemblers**: Convert between DTOs and entities (located in `application/{domain}/assembler`)

### Frontend Architecture
- **Service Layer**: API communication through `lib/*-service.ts` files
- **Component Pattern**: Function components with TypeScript interfaces
- **State Management**: React Context API with useReducer pattern
- **Styling**: Tailwind CSS with utility-first approach
- **Type Safety**: Comprehensive TypeScript coverage

## Key Development Guidelines

### Backend Java Rules
- **NO Lombok**: Use standard Java getters/setters
- **MyBatis-Plus**: Use lambda queries with `Wrappers.<Entity>lambdaQuery()`
- **Entity Updates**: Use `MyBatisPlusExtRepository.checkedUpdate()` for permission-aware updates
- **Permissions**: Use `entity.setAdmin()` for admin operations, check `entity.needCheckUserId()`
- **JSON Fields**: Use custom converters in `infrastructure/converter/` for JSON database fields
- **Exceptions**: Use `BusinessException` for business logic errors
- **Repository Naming**: 
  - `getXxx()`: Must return value or throw exception
  - `findXxx()`: Can return null
  - `existsXxx()`: Returns boolean
  - `checkXxxExists()`: Throws exception if not found

### Frontend TypeScript Rules
- **Component Library**: Use Shadcn/ui components (in `components/ui/`)
- **API Services**: Use centralized services in `lib/` directory
- **Type Safety**: Define interfaces for all data structures
- **State Management**: Use Context API with reducers for complex state
- **Styling**: Tailwind CSS with `cn()` utility for class merging
- **Error Handling**: Use toast notifications for user feedback

### Database Conventions
- **No Foreign Keys**: DDD principle - no database-level foreign key constraints
- **Naming**: Snake_case for tables/columns, lowercase plural for table names
- **Audit Fields**: All entities inherit `created_at`, `updated_at` from `BaseEntity`

### Three-Layer Validation
1. **API Layer**: Field format validation using `@Validated`
2. **Application Layer**: Business rules, data existence, operation permissions  
3. **Domain Layer**: Entity state validation, domain rules verification

## Service Access
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080  
- **API Gateway**: http://localhost:8081
- **Database**: localhost:5432

## Default Credentials
- **Admin**: admin@agentx.ai / admin123
- **Test User**: test@agentx.ai / test123

## Important Notes
- Always run code formatting before commits (`mvnw spotless:apply` for Java)
- Use `--legacy-peer-deps` flag when installing npm dependencies
- Development mode includes file watching with automatic container restarts
- Follow REST conventions for API design
- All API responses use unified `Result<T>` wrapper format