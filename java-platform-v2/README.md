# Java Platform v2.0 - Enterprise Edition

A highly scalable, production-ready Java platform for collaborative learning, real-time code compilation, and communication.

## 🚀 Features

- **Real-time Collaboration**: WebSocket-based chat and code editing
- **Java Code Compiler**: In-sandbox Java code compilation and execution with timeout/memory limits
- **AI Assistant**: Intelligent error analysis, code suggestions, and Java documentation
- **Video Calling**: WebRTC-based video communication between users
- **User Management**: Session management, authentication, and user presence
- **Scalable Architecture**: Spring Boot 3.2, MySQL, Redis, MongoDB
- **Docker Deployment**: Complete containerization with docker-compose

## 📋 System Requirements

- Java 21 LTS (OpenJDK recommended)
- Maven 3.8+
- Docker & Docker Compose (for containerized deployment)
- MySQL 8.0+
- Redis 7.0+
- MongoDB 6.0+ (optional, for document storage)

## 🛠️ Technologies

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| ORM | Spring Data JPA (Hibernate) |
| Real-time | WebSockets (Java WebSocket API) |
| Database | MySQL 8.0 |
| Cache | Redis 7.0 |
| Documents | MongoDB 6.0 |
| Authentication | JWT Tokens |
| Container | Docker |

## 📁 Project Structure

```
java-platform-v2/
├── backend/                          # Spring Boot backend
│   ├── src/main/java/com/javaplatform/
│   │   ├── api/                     # REST Controllers (6)
│   │   │   ├── AuthenticationController.java
│   │   │   ├── CompilerController.java
│   │   │   ├── ChatController.java
│   │   │   ├── AIAssistantController.java
│   │   │   ├── VideoController.java (ready)
│   │   │   └── HealthController.java
│   │   ├── config/                  # Configuration Classes (5+)
│   │   │   ├── WebSocketConfig.java
│   │   │   ├── CorsConfig.java
│   │   │   ├── CacheConfig.java
│   │   │   ├── AsyncConfig.java
│   │   │   └── JpaConfig.java
│   │   ├── core/                    # Core Services
│   │   │   ├── CompilerService.java
│   │   │   ├── SandboxExecutor.java
│   │   │   ├── ErrorAnalyzer.java
│   │   │   └── CompileResult.java
│   │   ├── dto/                     # Data Transfer Objects (2+)
│   │   │   ├── LoginRequest.java
│   │   │   └── CompileRequest.java
│   │   ├── model/                   # JPA Entities (3)
│   │   │   ├── User.java
│   │   │   ├── Message.java
│   │   │   └── CodeExecution.java
│   │   ├── repository/              # Data Access (3)
│   │   │   ├── UserRepository.java
│   │   │   ├── MessageRepository.java
│   │   │   └── CodeExecutionRepository.java
│   │   ├── realtime/                # WebSocket Handlers
│   │   │   └── ChatWebSocketHandler.java
│   │   ├── service/                 # Business Logic Services (6)
│   │   │   ├── UserService.java
│   │   │   ├── MessageService.java
│   │   │   ├── CodeCompilationService.java
│   │   │   ├── ChatService.java
│   │   │   ├── AIAssistantService.java
│   │   │   └── VideoService.java
│   │   ├── util/                    # Utilities
│   │   │   ├── JwtTokenProvider.java
│   │   │   └── ApiResponse.java
│   │   └── JavaPlatformApplication.java  # Main entry point
│   ├── src/main/resources/
│   │   └── application.yml          # Configuration
│   └── pom.xml                      # Maven dependencies
├── pom.xml                          # Parent POM
├── Dockerfile                       # Docker build
├── docker-compose.yml               # Docker Compose services
├── schema.sql                       # Database schema
└── README.md                        # This file
```

## 🚀 Quick Start

### Option 1: Local Development

1. **Set up databases**:
   ```bash
   # Start MySQL
   mysql -u root -p
   CREATE DATABASE javaplatform;
   
   # Start Redis
   redis-server
   ```

2. **Run the application**:
   ```bash
   cd java-platform-v2/backend
   mvn spring-boot:run
   ```

3. **Access the API**:
   ```
   http://localhost:8080/api/v1/health
   ```

### Option 2: Docker Deployment (Recommended)

```bash
cd java-platform-v2
docker-compose up -d
```

This starts:
- MySQL on port 3306
- Redis on port 6379
- MongoDB on port 27017
- Backend API on port 8080

## 📚 API Documentation

### Authentication Endpoints

#### Register User
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "john_doe"
}

Response:
{
  "success": true,
  "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "sessionId": "uuid..."
}
```

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "john_doe"
}
```

#### Validate Token
```http
GET /api/v1/auth/validate
Authorization: Bearer <token>
```

### Compiler Endpoints

#### Compile & Run Code
```http
POST /api/v1/compile/run
Authorization: Bearer <token>
Content-Type: application/json

{
  "sourceCode": "public class Hello {\n  public static void main(String[] args) {\n    System.out.println(\"Hello, World!\");\n  }\n}",
  "className": "Hello",
  "input": ""
}

Response:
{
  "success": true,
  "compiled": true,
  "output": "Hello, World!\n",
  "errors": [],
  "executionTime": 250
}
```

#### Get Execution History
```http
GET /api/v1/compile/history
Authorization: Bearer <token>
```

### Chat Endpoints

#### Get Chat History
```http
GET /api/v1/chat/history/{userId}
Authorization: Bearer <token>
```

#### Get Latest Messages
```http
GET /api/v1/chat/latest?limit=50
Authorization: Bearer <token>
```

#### Send Message
```http
POST /api/v1/chat/send
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "Hello everyone!"
}
```

### AI Assistant Endpoints

#### Analyze Error
```http
POST /api/v1/ai/analyze-error
Authorization: Bearer <token>
Content-Type: application/json

{
  "error": "cannot find symbol: variable count"
}

Response:
{
  "success": true,
  "error": "cannot find symbol: variable count",
  "explanation": "💡 The variable or method you're using hasn't been defined...",
  "suggestions": ["• Check if the variable/method is spelled correctly", ...],
  "category": "COMPILATION_ERROR"
}
```

#### Get Code Suggestions
```http
POST /api/v1/ai/suggest-improvements
Authorization: Bearer <token>
Content-Type: application/json

{
  "sourceCode": "..."
}
```

#### Explain Concept
```http
GET /api/v1/ai/explain/{concept}
Authorization: Bearer <token>
```

Supported concepts: polymorphism, encapsulation, inheritance, abstraction, interface, exception, generics, lambda, stream, recursion

### WebSocket Endpoints

#### Chat WebSocket
```
ws://localhost:8080/ws/chat
```

Message format:
- Register user: `USER|john_doe`
- Send message: `CHAT|Hello everyone!`
- Receive online users: `USERS|user1,user2,user3`
- Receive messages: `MSG|john_doe: Hello everyone!`

## 🔒 Security Features

- **JWT Authentication**: Token-based authentication for all API endpoints
- **CORS Configuration**: Cross-origin requests properly configured
- **Input Validation**: All user inputs validated before processing
- **Sandbox Execution**: Java code executed in isolated environment with:
  - 10-second timeout limit
  - 256MB memory limit
  - Blacklisted dangerous operations (System.exit, Runtime.exec, File.delete, etc.)

## ⚙️ Configuration

Edit `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/javaplatform
    username: root
    password: password
  redis:
    host: localhost
    port: 6379
  jpa:
    hibernate:
      ddl-auto: update

server:
  port: 8080

jwt:
  secret: your-secret-key
  expiration: 86400000  # 24 hours in milliseconds
```

## 📊 Database Schema

### Users Table
- `id` (PK): User identifier
- `username`: Unique username
- `session_id`: WebSocket session ID
- `online`: Boolean flag for user status
- `created_at`, `last_active`: Timestamps

### Messages Table
- `id` (PK): Message identifier
- `user_id` (FK): Reference to users
- `content`: Message text (LONGTEXT)
- `status`: SENT, DELIVERED, READ
- Indexed on `(user_id, created_at DESC)` for fast retrieval

### CodeExecutions Table
- `id` (PK): Execution identifier
- `user_id` (FK): Reference to users
- `code`: Source code (LONGTEXT)
- `output`, `errors`: Execution results
- `execution_time`: Duration in milliseconds
- `success`: Boolean flag

## 🧪 Testing

Run tests:
```bash
cd backend
mvn test
```

Test coverage target: 80%+

## 📈 Performance Optimization

- **Database Indexing**: All frequently-queried columns indexed
- **Caching**: Redis integration for session and query result caching
- **Connection Pooling**: HikariCP with 20-connection pool
- **Async Processing**: Message persistence and code execution handled asynchronously
- **Thread Pool**: Fixed 10-thread pool for compiler operations

## 🐳 Docker Deployment

Build and run:

```bash
# Build backend
docker build -t javaplatform:2.0 .

# Run with docker-compose
docker-compose up -d

# View logs
docker-compose logs -f backend

# Stop services
docker-compose down
```

## 📦 Dependencies

- **Spring Boot 3.2.0**: Core framework
- **Spring Data JPA**: ORM layer
- **Spring WebSocket**: Real-time communication
- **Spring Data Redis**: Cache management
- **MySQL Connector**: Database connectivity
- **jjwt 0.11.5**: JWT token generation/validation
- **Lombok**: Code generation
- **Jackson**: JSON processing

## 🔄 CI/CD Pipeline (Ready)

GitHub Actions workflow template included. Deploy to:
- AWS EC2
- Docker Hub
- Azure Container Registry
- Kubernetes clusters

## 📝 Development Roadmap

### Phase 1: ✅ Core Foundation
- ✅ Data models (User, Message, CodeExecution)
- ✅ REST APIs (Auth, Compiler, Chat, AI)
- ✅ WebSocket real-time communication
- ✅ Java sandboxed code execution
- ✅ Error analysis & suggestions

### Phase 2: 🔄 Advanced Features (Next)
- [ ] Video call integration (WebRTC)
- [ ] Collaborative code editing (OT/CRDT)
- [ ] User permissions & roles
- [ ] Advanced analytics
- [ ] Mobile app

### Phase 3: 📊 Enterprise Features
- [ ] Single Sign-On (OAuth2, SAML)
- [ ] Advanced monitoring & logging
- [ ] Serverless functions support
- [ ] AI-powered code review
- [ ] Machine learning insights

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing`)
5. Open Pull Request

## 📄 License

MIT License - See LICENSE file for details

## 👥 Support

- **Issues**: GitHub Issues
- **Documentation**: See docs/ folder
- **Email**: support@javaplatform.io
- **Slack**: #javaplatform community

---

**Version**: 2.0.0  
**Last Updated**: 2024  
**Status**: Production Ready ✅
