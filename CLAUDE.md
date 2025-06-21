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

### Backend Java Code Style
- **NO Lombok**: Use standard Java getters/setters (explicit policy)
- **Code Formatting**: Use Spotless plugin with Eclipse formatter (4 spaces, 120 char line limit)
- **Package Structure**: Follow DDD layering - `domain`, `application`, `infrastructure`, `interfaces`
- **Indentation**: 4 spaces (no tabs), continuation indent 2 spaces
- **Line Length**: Maximum 120 characters
- **Comments**: Use Javadoc `/** */` for public methods, concise descriptions

### Backend Java Rules
- **MyBatis-Plus Queries**: Use lambda queries with `Wrappers.<Entity>lambdaQuery()`
- **Entity Updates**: Use `MyBatisPlusExtRepository.checkedUpdate()` for permission-aware updates
- **Permissions**: Use `entity.setAdmin()` for admin operations, check `entity.needCheckUserId()`
- **JSON Fields**: Use custom converters in `infrastructure/converter/` for JSON database fields
- **Exceptions**: Use `BusinessException` for business logic errors
- **Repository Naming Conventions**: 
  - `getXxx()`: Must return value or throw exception
  - `findXxx()`: Can return null
  - `existsXxx()`: Returns boolean
  - `checkXxxExists()`: Throws exception if not found
- **Entity Operations**: Use `Operator.ADMIN` for admin operations, check `operator.needCheckUserId()`

### Controller Layer Standards
- **Annotation Usage**: `@RestController`, `@RequestMapping` for base path
- **Validation**: Add `@Validated` annotation to `@RequestBody` parameters
- **Required Imports**: 
  ```java
  import org.springframework.validation.annotation.Validated;
  import org.xhy.infrastructure.auth.UserContext;
  import org.xhy.interfaces.api.common.Result;
  ```
- **User Context**: Always use `UserContext.getCurrentUserId()` for current user ID
- **Method Structure**: Keep controllers thin, delegate to application services
- **Return Types**: Always use `Result<T>` wrapper for API responses
- **Error Handling**: Let application layer handle business logic validation

#### Controller Method Format:
```java
/** Method description
 * 
 * @param request request object description
 * @return response description */
@PostMapping("/endpoint")
public Result<ResponseType> methodName(@RequestBody @Validated RequestType request) {
    String userId = UserContext.getCurrentUserId();
    ResponseType result = appService.methodName(request, userId);
    return Result.success(result);
}
```

### Application Service Layer
- **Constructor Injection**: Use final fields with constructor injection
- **Method Comments**: Use multiline Javadoc for public methods
- **Transaction Management**: Use `@Transactional` for database operations
- **Event Publishing**: Use `ApplicationEventPublisher` for domain events
- **Assembler Usage**: Use dedicated assemblers for DTO/Entity conversion

#### Application Service Format:
```java
@Service
public class ExampleAppService {
    
    private final ExampleDomainService exampleDomainService;
    
    public ExampleAppService(ExampleDomainService exampleDomainService) {
        this.exampleDomainService = exampleDomainService;
    }
    
    /**
     * Method description
     * 
     * @param request request object
     * @param userId current user ID
     * @return response object
     */
    public ResponseDTO methodName(RequestDTO request, String userId) {
        // Implementation
    }
}
```

### Domain Service Layer
- **Business Logic**: Core business rules and entity operations
- **Repository Access**: Direct repository access for data operations
- **Validation**: Entity state and business rule validation
- **Event Publishing**: Publish domain events for side effects

### Repository Layer
- **Interface**: Extend `MyBatisPlusExtRepository<Entity>`
- **Custom Queries**: Use `@Select`, `@Update`, `@Delete` for complex SQL
- **Permission Checks**: Use `checkedUpdate()`, `checkedDelete()` methods
- **Naming**: Repository interface in domain layer, mapper XML in infrastructure

### Frontend TypeScript Standards
- **File Organization**: Pages in `app/`, components in `components/`, services in `lib/`
- **Component Library**: Use Shadcn/ui components from `@/components/ui/`
- **State Management**: React Context with useReducer for complex state
- **Type Definitions**: Create interfaces for all data structures
- **API Services**: Centralized API calls in service classes with static methods

### Frontend Component Patterns
- **"use client"**: Add directive for client components in Next.js App Router
- **Props Interface**: Define TypeScript interfaces for component props
- **State Hooks**: Use `useState`, `useEffect` for component state
- **Error Handling**: Use toast notifications via `useToast` hook
- **Loading States**: Implement loading spinners for async operations

#### Frontend Component Format:
```typescript
"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { toast } from "@/hooks/use-toast";

interface ComponentProps {
  prop1: string;
  prop2: number;
}

export default function ComponentName({ prop1, prop2 }: ComponentProps) {
  const [loading, setLoading] = useState(false);
  
  useEffect(() => {
    // Side effects
  }, []);
  
  return (
    <div className="space-y-4">
      {/* Component content */}
    </div>
  );
}
```

### Frontend Service Layer
- **API Services**: Static class methods for API calls
- **Error Handling**: Consistent error handling with try-catch
- **Type Safety**: Return typed responses with interfaces
- **HTTP Client**: Use centralized `httpClient` from `lib/http-client`

#### API Service Format:
```typescript
export class ExampleApiService {
  static async getItems(): Promise<ItemResponse[]> {
    return httpClient.get('/api/items');
  }
  
  static async createItem(data: CreateItemRequest): Promise<ItemResponse> {
    return httpClient.post('/api/items', data);
  }
}
```

### Styling Guidelines
- **Tailwind CSS**: Use utility-first classes
- **Class Merging**: Use `cn()` utility from `lib/utils` for conditional classes
- **Component Variants**: Use `class-variance-authority` for component variants
- **Responsive Design**: Mobile-first approach with Tailwind breakpoints

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

## Testing Guidelines

### Backend Testing
- **Unit Tests**: Test domain services and business logic in isolation
- **Integration Tests**: Test repositories and database interactions
- **Test Location**: Place tests in `src/test/java` with same package structure
- **Test Naming**: Use descriptive method names with `should_` prefix
- **Mock Strategy**: Mock external dependencies, use real objects for domain logic

### Frontend Testing
- **Component Testing**: Test UI components in isolation
- **Integration Testing**: Test API service integration
- **Type Safety**: Ensure TypeScript types are tested
- **Mock Data**: Use mock data for development and testing

## Error Handling & Logging

### Backend Error Handling
- **Business Exceptions**: Use `BusinessException` for business rule violations
- **Validation Errors**: Use `@Validated` annotations for input validation
- **Global Handler**: `GlobalExceptionHandler` catches and formats exceptions
- **Logging**: Use SLF4J with structured logging patterns

### Frontend Error Handling
- **API Errors**: Handle HTTP errors gracefully with toast notifications
- **Form Validation**: Use react-hook-form with zod schemas
- **Error Boundaries**: Implement error boundaries for component failures
- **User Feedback**: Always provide clear error messages to users

### Logging Standards
```java
// Backend logging pattern
private static final Logger logger = LoggerFactory.getLogger(ClassName.class);

// Usage
logger.info("User {} performed action {}", userId, action);
logger.error("Failed to process request", exception);
```

## Security Best Practices

### Authentication & Authorization
- **JWT Tokens**: Use JWT for stateless authentication
- **User Context**: Always use `UserContext.getCurrentUserId()`
- **Permission Checks**: Validate user permissions at service layer
- **Admin Operations**: Use `entity.setAdmin()` for admin-only operations

### Data Protection
- **Input Validation**: Validate all inputs at API layer
- **SQL Injection**: Use MyBatis-Plus parameterized queries
- **Sensitive Data**: Never log passwords, tokens, or sensitive information
- **API Keys**: Store API keys encrypted, mask in responses

### Frontend Security
- **Environment Variables**: Use `NEXT_PUBLIC_` prefix for client-side variables
- **API Calls**: Always handle authentication tokens securely
- **Input Sanitization**: Sanitize user inputs before rendering
- **HTTPS**: Ensure all production traffic uses HTTPS

## Performance Guidelines

### Backend Performance
- **Database Queries**: Use lazy loading and pagination for large datasets
- **Caching**: Implement caching for frequently accessed data
- **Transaction Management**: Keep transactions short and focused
- **Async Processing**: Use `@Async` for long-running operations

### Frontend Performance
- **Code Splitting**: Use dynamic imports for large components
- **State Management**: Minimize re-renders with proper state design
- **API Optimization**: Implement loading states and error handling
- **Bundle Size**: Monitor and optimize bundle size

## Development Workflow

### Code Quality Checklist
- [ ] Run `mvnw spotless:apply` before committing Java code
- [ ] Run `npm run lint` before committing TypeScript code
- [ ] Write unit tests for new business logic
- [ ] Update API documentation for interface changes
- [ ] Validate database migrations work correctly
- [ ] Test both success and error scenarios

### Git Workflow
- **Branch Naming**: Use `feature/description`, `fix/description`, `hotfix/description`
- **Commit Messages**: Use conventional commits format
- **Pull Requests**: Include description, testing notes, and breaking changes
- **Code Review**: Review for business logic, security, and performance

### Debugging Tips
- **Backend Debugging**: Use IDE breakpoints with Spring Boot DevTools
- **Database Debugging**: Check SQL logs and query performance
- **Frontend Debugging**: Use React DevTools and browser console
- **API Debugging**: Use network tab to inspect HTTP requests/responses

## API Standards & Data Flow Specifications

### Backend API Response Standards

#### Unified Response Format
All API responses MUST use the `Result<T>` wrapper format:

```java
// Standard response structure
public class Result<T> {
    private Integer code;        // HTTP status code (200, 400, 401, 403, 404, 500)
    private String message;      // Response message
    private T data;             // Response data payload
    private Long timestamp;     // Response timestamp
}

// Success responses
Result.success()                    // No data
Result.success(data)                // With data
Result.success(message, data)       // Custom message with data

// Error responses
Result.error(code, message)         // Custom error
Result.badRequest(message)          // 400 error
Result.unauthorized(message)        // 401 error
Result.forbidden(message)           // 403 error
Result.notFound(message)           // 404 error
Result.serverError(message)         // 500 error
```

#### Request Parameter Validation
- **Required Fields**: Use `@NotBlank`, `@NotNull` annotations
- **Request Bodies**: Always add `@Validated` to `@RequestBody` parameters
- **Path Variables**: Validate in service layer, not controller
- **Query Parameters**: Use dedicated request DTOs for complex queries

```java
// Example request DTO
public class CreateAgentRequest {
    @NotBlank(message = "助理名称不可为空")
    private String name;
    
    private String description;
    private String avatar;
    private String systemPrompt;
    private List<String> toolIds;
    // ... getters and setters
}
```

#### Pagination Standards
- **Default Page Size**: 15 items per page
- **Page Parameters**: `page` (1-based), `pageSize` (max 100)
- **Response Format**: Use consistent pagination wrapper

```java
// Pagination request
public class Page {
    private Integer page = 1;
    private Integer pageSize = 15;
}

// Pagination response (when applicable)
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [...],
        "total": 150,
        "size": 15,
        "current": 1,
        "pages": 10
    }
}
```

#### Admin Operation Patterns
- **Admin Context**: Use `Operator.ADMIN` for administrative operations
- **Permission Checks**: Always validate `operator.needCheckUserId()`
- **Admin Endpoints**: Prefix with `/admin/` for administrative APIs
- **User Context**: Regular operations use `UserContext.getCurrentUserId()`

```java
// Admin operation example
@PostMapping("/admin/agents/versions/{versionId}/status")
public Result<AgentVersionDTO> updateVersionStatus(
    @PathVariable String versionId, 
    @RequestParam Integer status) {
    
    // Admin operations don't need userId check
    PublishStatus publishStatus = PublishStatus.fromCode(status);
    ReviewAgentVersionRequest request = new ReviewAgentVersionRequest();
    request.setStatus(publishStatus);
    
    return Result.success(agentAppService.reviewAgentVersion(versionId, request));
}

// Regular user operation
@PostMapping("/agents")
public Result<AgentDTO> createAgent(@RequestBody @Validated CreateAgentRequest request) {
    String userId = UserContext.getCurrentUserId(); // Always get current user
    AgentDTO agent = agentAppService.createAgent(request, userId);
    return Result.success(agent);
}
```

### Frontend Data Flow Standards

#### HTTP Client Configuration
- **Base URL**: Configured via `API_CONFIG.BASE_URL`
- **Authentication**: Automatic Bearer token injection
- **Timeout**: Default 30 seconds, configurable per request
- **Error Handling**: Automatic 401 redirect to login

```typescript
// HTTP client usage
const response = await httpClient.get<ApiResponse<Agent[]>>(
  '/agents',
  { params: { name: searchQuery } }
);

// With request options
const response = await httpClient.post<ApiResponse<Agent>>(
  '/agents',
  requestData,
  {},
  { showToast: true } // Auto-show success/error toast
);
```

#### API Response Handling
- **Type Safety**: All responses use `ApiResponse<T>` interface
- **Error Handling**: Consistent error structure with code/message
- **Toast Integration**: Optional automatic toast notifications

```typescript
// Standard API response interface
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

// Service method pattern
export async function getUserAgents(params: GetAgentsParams): Promise<ApiResponse<Agent[]>> {
  try {
    const response = await httpClient.get<ApiResponse<Agent[]>>(
      '/agents/user',
      { params }
    );
    return response;
  } catch (error) {
    // Return formatted error response
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    };
  }
}
```

#### State Management Patterns
- **Loading States**: Always implement loading indicators
- **Error States**: Handle and display error messages
- **Success States**: Provide user feedback for successful operations
- **Optimistic Updates**: Update UI immediately, rollback on error

```typescript
// Component state management pattern
export default function StudioPage() {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Data fetching with error handling
  useEffect(() => {
    async function fetchAgents() {
      try {
        setLoading(true);
        setError(null);

        const response = await getUserAgentsWithToast({ name: searchQuery });

        if (response.code === 200) {
          setAgents(response.data);
        } else {
          setError(response.message);
        }
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : "未知错误";
        setError(errorMessage);
      } finally {
        setLoading(false);
      }
    }

    fetchAgents();
  }, [searchQuery]);

  // Optimistic update example
  const handleToggleStatus = async (agent: Agent) => {
    // Update UI immediately
    setAgents(agents.map(a => 
      a.id === agent.id 
        ? { ...a, enabled: !a.enabled } 
        : a
    ));

    try {
      const response = await toggleAgentStatusWithToast(agent.id);
      
      if (response.code !== 200) {
        // Rollback on error
        setAgents(agents.map(a => 
          a.id === agent.id 
            ? { ...a, enabled: agent.enabled } 
            : a
        ));
      }
    } catch (error) {
      // Rollback on error
      setAgents(agents.map(a => 
        a.id === agent.id 
          ? { ...a, enabled: agent.enabled } 
          : a
      ));
    }
  };
}
```

#### Toast Notification Standards
- **Success Operations**: Show success toast for create/update/delete
- **Error Handling**: Always show error toasts for failed operations
- **Toast Wrapper**: Use `withToast()` utility for automatic handling

```typescript
// Manual toast usage
toast({
  title: "操作成功",
  description: "助理已创建",
  variant: "default",
});

toast({
  title: "操作失败",
  description: error.message,
  variant: "destructive",
});

// Automatic toast with withToast wrapper
export const createAgentWithToast = withToast(createAgent, {
  showSuccessToast: true,
  showErrorToast: true,
  successTitle: "创建助理成功",
  errorTitle: "创建助理失败"
});
```

#### Form Validation & Submission
- **Client-side Validation**: Validate before submission
- **Server-side Validation**: Handle server validation errors
- **Loading States**: Disable forms during submission
- **Error Display**: Show field-specific errors

```typescript
// Form submission pattern
const handleSubmit = async (formData: CreateAgentRequest) => {
  // Client-side validation
  if (!formData.name.trim()) {
    toast({
      title: "请输入助理名称",
      variant: "destructive",
    });
    return;
  }

  setIsSubmitting(true);

  try {
    const response = await createAgentWithToast(formData);
    
    if (response.code === 200) {
      router.push('/studio');
    } else {
      // Server validation errors handled by withToast
    }
  } catch (error) {
    // Network errors handled by withToast
  } finally {
    setIsSubmitting(false);
  }
};
```

### Repository & Database Patterns

#### MyBatis-Plus Repository Extensions
- **Permission-aware Updates**: Use `checkedUpdate()` for secure operations
- **Transaction Safety**: Throw exceptions on zero affected rows
- **Admin Operations**: Use `Operator` enum for permission checks

```java
// Safe update with permission check
public interface AgentRepository extends MyBatisPlusExtRepository<AgentEntity> {
    // Inherited methods: checkedUpdate(), checkedDelete(), checkInsert()
}

// Usage in domain service
LambdaUpdateWrapper<AgentEntity> updateWrapper = Wrappers.<AgentEntity>lambdaUpdate()
    .eq(AgentEntity::getId, agentId)
    .eq(entity.needCheckUserId(), AgentEntity::getUserId, userId);

repository.checkedUpdate(updatedEntity, updateWrapper);
```

#### Query Patterns
- **Lambda Queries**: Always use type-safe lambda expressions
- **Permission Filtering**: Include user ID in queries for data isolation
- **Pagination**: Use MyBatis-Plus `Page` for large datasets

```java
// Standard query pattern
LambdaQueryWrapper<AgentEntity> wrapper = Wrappers.<AgentEntity>lambdaQuery()
    .eq(AgentEntity::getUserId, userId)
    .like(StringUtils.hasText(searchName), AgentEntity::getName, searchName)
    .eq(AgentEntity::getEnabled, true)
    .orderByDesc(AgentEntity::getUpdatedAt);

List<AgentEntity> agents = repository.selectList(wrapper);

// Paginated query
Page<AgentEntity> page = new Page<>(pageNum, pageSize);
Page<AgentEntity> result = repository.selectPage(page, wrapper);
```

## Common Patterns

### API Response Pattern
```java
// Success response
return Result.success(data);

// Error response  
return Result.error("Error message");

// Validation error
if (validation.hasErrors()) {
    throw new ParamValidationException(validation.getErrors());
}
```

### React State Pattern
```typescript
// Complex state with useReducer
const [state, dispatch] = useReducer(reducer, initialState);

// Simple state with useState
const [loading, setLoading] = useState(false);

// Effect cleanup pattern
useEffect(() => {
  const controller = new AbortController();
  
  fetchData(controller.signal);
  
  return () => controller.abort();
}, [dependency]);
```

### MyBatis-Plus Query Pattern
```java
// Lambda query
LambdaQueryWrapper<Entity> wrapper = Wrappers.<Entity>lambdaQuery()
    .eq(Entity::getUserId, userId)
    .eq(Entity::getStatus, true)
    .orderByDesc(Entity::getCreatedAt);

// Update with permission check
LambdaUpdateWrapper<Entity> updateWrapper = Wrappers.<Entity>lambdaUpdate()
    .eq(Entity::getId, id)
    .eq(entity.needCheckUserId(), Entity::getUserId, userId);
repository.checkedUpdate(entity, updateWrapper);
```

## Troubleshooting

### Common Backend Issues
- **Database Connection**: Check PostgreSQL connection settings
- **Port Conflicts**: Ensure ports 8080, 5432 are available
- **Maven Dependencies**: Clear `.m2/repository` if build fails
- **Hot Reload**: Restart application if DevTools stops working

### Common Frontend Issues
- **npm install**: Use `--legacy-peer-deps` flag for compatibility
- **Build Errors**: Clear `.next` folder and node_modules
- **Type Errors**: Check TypeScript configuration and imports
- **API Calls**: Verify backend service is running

### Docker Issues
- **Container Build**: Clear Docker build cache if images fail
- **Volume Mounts**: Ensure correct file permissions
- **Network Issues**: Check docker-compose network configuration
- **Database Init**: Verify SQL scripts execute correctly

## Important Notes
- Always run code formatting before commits (`mvnw spotless:apply` for Java)
- Use `--legacy-peer-deps` flag when installing npm dependencies
- Development mode includes file watching with automatic container restarts
- Follow REST conventions for API design
- All API responses use unified `Result<T>` wrapper format
- Never commit secrets, API keys, or sensitive configuration
- Test thoroughly in Docker environment before production deployment

## Key Reminders
- NO Lombok - Use standard Java getters/setters
- Use MyBatis-Plus lambda queries exclusively
- All controllers must use `@Validated` for request validation
- Frontend components require TypeScript interfaces
- Always handle loading and error states in UI
- Run Spotless formatting before every commit
- Check both success and failure scenarios during testing