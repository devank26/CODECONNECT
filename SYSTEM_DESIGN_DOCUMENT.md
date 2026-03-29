# 🏗️ Java Platform v2.0 - Complete System Design Document

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Technology Stack](#technology-stack)
3. [System Components](#system-components)
4. [Database Schema](#database-schema)
5. [API Specifications](#api-specifications)
6. [Security Architecture](#security-architecture)
7. [Implementation Roadmap](#implementation-roadmap)

---

## Architecture Overview

### HighLevel System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     CLIENT LAYER                             │
├─────────────────────────────────────────────────────────────┤
│  Frontend (React/Vue/Angular SPA or enhanced JavaFX desktop) │
│  - Code Editor (Monaco / CodeMirror)                         │
│  - Chat UI                                                   │
│  - Video Call UI (WebRTC)                                    │
│  - AI Assistant Chat                                         │
└────────────────┬────────────────────────────────────────────┘
                 │ HTTP/HTTPS + WebSocket + WebRTC
        ┌────────▼────────────────────────────────────┐
        │     API GATEWAY / LOAD BALANCER              │
        │  (nginx / AWS API Gateway)                   │
        └────────┬────────────────────────────────────┘
                 │
    ┌────────────┼────────────────┐
    │            │                 │
    ▼            ▼                 ▼
┌──────────┐ ┌──────────┐ ┌──────────────┐
│ Auth     │ │Compiler  │ │ Real-time    │
│Service   │ │Service   │ │Services      │
│(JWT)     │ │(Docker)  │ │(WebSocket)   │
└──────────┘ └──────────┘ └──────────────┘
    │            │                 │
    └────────────┼─────────────────┘
                 │
        ┌────────▼────────────────┐
        │   SPRING BOOT            │
        │   REST API               │
        │   (Microservices)        │
        └────────┬─────────────────┘
                 │
    ┌────────────┼──────────────────┐
    │            │                   │
    ▼            ▼                   ▼
┌──────────┐ ┌──────────┐ ┌──────────────┐
│PostgreSQL│ │MongoDB   │ │Redis Cache   │
│(Users,   │ │(Messages,│ │(Sessions,    │
│Sessions) │ │Code)     │ │Real-time)    │
└──────────┘ └──────────┘ └──────────────┘
```

### Layered Architecture

```
┌─────────────────────────────────────────┐
│         PRESENTATION LAYER               │
│  (Controllers & REST Endpoints)          │
├─────────────────────────────────────────┤
│         BUSINESS LOGIC LAYER             │
│  (Services: Auth, Compiler, Chat, AI)   │
├─────────────────────────────────────────┤
│         DATA ACCESS LAYER                │
│  (Repositories & DAO)                    │
├─────────────────────────────────────────┤
│         DATABASE & EXTERNAL SERVICES     │
└─────────────────────────────────────────┘
```

---

## Technology Stack

### Backend
- **Framework:** Spring Boot 3.x with Spring Cloud
- **Language:** Java 17+
- **Build Tool:** Maven
- **API:** REST API (Spring MVC)
- **Real-time:** WebSocket (Spring WebSocket + SockJS)
- **Code Execution:** Docker Containers + Security Manager
- **Authentication:** Spring Security + JWT
- **API Documentation:** Springdoc OpenAPI (Swagger)

### Frontend - Option 1 (Web)
- **Framework:** React 18 / Vue 3 / Angular 16+
- **Code Editor:** Monaco Editor / CodeMirror
- **WebRTC:** mediasoup-client / simple-peer
- **State Management:** Redux / Pinia / NgRx
- **UI Component Library:** Material-UI / Ant Design / Vuetify
- **Real-time Communication:** Socket.io / SockJS

### Frontend - Option 2 (Desktop)
- **Framework:** JavaFX 21+ (Enhanced)
- **Code Editor:** RichTextFX / FXML-based syntax highlighting
- **WebRTC:** Kurento / TwilioVideo Java SDK
- **Async:** Project Reactor / Vert.x

### Databases
- **PostgreSQL:** Users, Analytics, Sessions
- **MongoDB:** Chat messages, Code snippets, Code history
- **Redis:** Real-time sessions, Caching, WebSocket state

### DevOps & Infrastructure
- **Containerization:** Docker & Docker Compose
- **Orchestration:** Kubernetes (optional)
- **CI/CD:** GitHub Actions / Jenkins
- **Deployment:** AWS EC2 / Render / Railway / DigitalOcean
- **Monitoring:** Prometheus + Grafana
- **Logging:** ELK Stack (Elasticsearch, Logstash, Kibana)

### Security
- **HTTPS/TLS:** Let's Encrypt / AWS ACM
- **Code Execution Isolation:** Docker + seccomp / AppArmor
- **Secrets Management:** AWS Secrets Manager / HashiCorp Vault
- **Rate Limiting:** Redis + Bucket4j
- **Input Validation:** Spring Validation / Hibernate Validator

---

## System Components

### 1. Authentication Service
- User registration with email verification
- Login/Logout with JWT tokens
- Refresh token mechanism
- OAuth2 integration (Google, GitHub)
- Role-based access control (RBAC)

### 2. Compiler Service
- Docker-based isolated execution
- Support for Java, Python, C++, JavaScript
- Resource limits (CPU, Memory, Time)
- Syntax highlighting and validation
- Execution metrics (Time, Memory, Output)

### 3. Real-time Chat Service
- WebSocket-based messaging
- Message persistence (MongoDB)
- Typing indicators
- Online/Offline status
- Message read receipts
- File sharing support

### 4. Video Call Service
- WebRTC peer-to-peer connection
- TURN servers for NAT traversal
- Screen sharing capability
- Call history and recordings
- Network quality adaptation

### 5. AI Assistant Service
- Integration with OpenAI GPT-4 / Gemini API
- Code explanation and optimization
- Error debugging
- Context-aware responses
- Chat history with code snippets

### 6. User Management Service
- Profile management
- User settings and preferences
- Achievements/Badges system
- User statistics and analytics

### 7. Code Storage & Management Service
- Code snippet management
- Version control for code
- Collaborative features
- Code sharing with permissions

---

## Database Schema

### PostgreSQL Schema

```sql
-- Users Table
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    avatar_url TEXT,
    bio TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP,
    verified_at TIMESTAMP
);

-- Sessions Table
CREATE TABLE sessions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    token TEXT UNIQUE NOT NULL,
    refresh_token TEXT UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Code Snippets Table (PostgreSQL)
CREATE TABLE code_snippets (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    title VARCHAR(255),
    description TEXT,
    language VARCHAR(50),
    code_content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Code Execution History Table
CREATE TABLE execution_history (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    snippet_id INTEGER,
    language VARCHAR(50),
    execution_time_ms INTEGER,
    memory_used_kb INTEGER,
    exit_code INTEGER,
    output TEXT,
    error_output TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(snippet_id) REFERENCES code_snippets(id) ON DELETE SET NULL
);

-- Calls Table
CREATE TABLE calls (
    id SERIAL PRIMARY KEY,
    initiator_id INTEGER NOT NULL,
    receiver_id INTEGER NOT NULL,
    call_type VARCHAR(50), -- 'video', 'screen_share'
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    duration_seconds INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(initiator_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(receiver_id) REFERENCES users(id) ON DELETE CASCADE
);

-- User Achievements Table
CREATE TABLE achievements (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    badge_name VARCHAR(255),
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- INDEX for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_code_snippets_user_id ON code_snippets(user_id);
CREATE INDEX idx_execution_history_user_id ON execution_history(user_id);
CREATE INDEX idx_calls_users ON calls(initiator_id, receiver_id);
```

### MongoDB Schema (Collections)

```javascript
// Messages Collection
db.createCollection("messages", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["sender_id", "room_id", "content", "created_at"],
            properties: {
                _id: { bsonType: "objectId" },
                sender_id: { bsonType: "int" },
                receiver_id: { bsonType: "int" },
                room_id: { bsonType: "string" },
                content: { bsonType: "string" },
                message_type: { enum: ["text", "file", "code"] },
                file_url: { bsonType: "string" },
                is_edited: { bsonType: "bool" },
                edited_at: { bsonType: "date" },
                reactions: [{ bsonType: "string" }],
                created_at: { bsonType: "date" },
                updated_at: { bsonType: "date" }
            }
        }
    }
});

// Code Snippets Collection
db.createCollection("code_history", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            properties: {
                _id: { bsonType: "objectId" },
                user_id: { bsonType: "int" },
                code: { bsonType: "string" },
                language: { bsonType: "string" },
                version: { bsonType: "int" },
                changes: { bsonType: "array" },
                created_at: { bsonType: "date" }
            }
        }
    }
});

// Create indexes
db.messages.createIndex({ room_id: 1, created_at: -1 });
db.messages.createIndex({ sender_id: 1 });
db.code_history.createIndex({ user_id: 1, created_at: -1 });
```

---

## API Specifications

### Authentication APIs

```
POST   /api/v1/auth/register          - Register new user
POST   /api/v1/auth/login             - Login user
POST   /api/v1/auth/logout            - Logout user
POST   /api/v1/auth/refresh           - Refresh JWT token
GET    /api/v1/auth/me                - Get current user info
POST   /api/v1/auth/verify-email      - Verify email

Request: POST /api/v1/auth/register
{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePassword123!",
    "full_name": "John Doe"
}

Response:
{
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
    "expires_in": 3600
}
```

### Compiler APIs

```
POST   /api/v1/compiler/compile       - Compile and run code
GET    /api/v1/compiler/languages     - Get supported languages
POST   /api/v1/compiler/execute       - Execute code in sandbox

Request: POST /api/v1/compiler/compile
{
    "code": "public class Main { ... }",
    "language": "java",
    "stdin": "optional input",
    "timeout_seconds": 10
}

Response:
{
    "success": true,
    "output": "Hello, World!",
    "errors": "",
    "execution_time_ms": 245,
    "memory_used_kb": 1024,
    "exit_code": 0
}
```

### Chat APIs

```
WebSocket: /api/v1/ws/chat/{roomId}

GET    /api/v1/chat/messages/{roomId}  - Get message history
POST   /api/v1/chat/rooms              - Create chat room
GET    /api/v1/chat/rooms              - List user's rooms
POST   /api/v1/chat/rooms/{id}/leave   - Leave room

WebSocket Events:
- new_message: { user, content, timestamp }
- user_typing: { user }
- user_online: { user }
- user_offline: { user }
```

### Video APIs

```
POST   /api/v1/video/call/initiate     - Start video call
POST   /api/v1/video/call/answer       - Answer video call
POST   /api/v1/video/call/end          - End video call
GET    /api/v1/video/call/history      - Get call history
GET    /api/v1/video/turn/config       - Get TURN server config

WebSocket: /api/v1/ws/video/{callId}

WebSocket Events:
- call_ring: { caller, caller_name }
- call_accepted: { }
- call_rejected: { reason }
- call_ended: { duration }
- peer_connected: { peer_info }
- network_quality: { quality: "high|medium|low" }
```

### AI Assistant APIs

```
POST   /api/v1/ai/chat                 - Send message to AI
GET    /api/v1/ai/chat/history         - Get chat history
POST   /api/v1/ai/explain-code         - Explain code snippet
POST   /api/v1/ai/optimize-code        - Suggest optimizations

Request: POST /api/v1/ai/chat
{
    "message": "How do I fix NullPointerException?",
    "code_context": "String name = null; System.out.println(name.length());",
    "conversation_id": "uuid"
}

Response:
{
    "response": "A NullPointerException occurs when you try to...",
    "suggestions": ["Add null check", "Use Optional"],
    "code_example": "if (name != null) { ... }"
}
```

---

## Security Architecture

### Authentication & Authorization

```
┌─────────────────┐
│ User Login      │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────┐
│ Generate JWT Token (access)     │
│ + Refresh Token (long-lived)    │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│ Store in Redis for validation   │
│ TTL: access(15 min), refresh(7d)│
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│ Attach JWT to requests          │
│ Authorization: Bearer <token>   │
└─────────────────────────────────┘
```

### Code Execution Sandbox

```
User Code → Docker Container
    ↓
    ├─ Resource Limits
    │  ├─ CPU: 1 core max
    │  ├─ Memory: 256MB max
    │  └─ Timeout: 10s max
    ├─ Security Constraints
    │  ├─ No network access
    │  ├─ No file system access
    │  └─ Isolated JVM
    └─ Output Capture
       └─ stdout/stderr → Result
```

### Input Validation Strategy

```
1. Request arrives → API Gateway
2. Validate schema (Bean Validation)
3. Sanitize input (XSS prevention)
4. Rate limiting check
5. Process request
6. Return sanitized response
```

---

## Implementation Roadmap

### Phase 1: Foundation (Weeks 1-2)
- [ ] Set up Spring Boot project structure
- [ ] Configure PostgreSQL & MongoDB
- [ ] Implement authentication (JWT)
- [ ] Set up basic REST API scaffold

### Phase 2: Core Features (Weeks 3-5)
- [ ] Implement Compiler Service (Docker)
- [ ] Build Chat Service (WebSocket)
- [ ] Create AI Integration
- [ ] Develop User Management

### Phase 3: Advanced Features (Weeks 6-7)
- [ ] WebRTC Video Call Integration
- [ ] Code Sharing & Collaboration
- [ ] Real-time Notifications
- [ ] Performance Optimization

### Phase 4: DevOps & Deployment (Week 8)
- [ ] Docker containerization
- [ ] Kubernetes setup (optional)
- [ ] CI/CD pipeline
- [ ] Cloud deployment

---

## Deployment Architecture

### Docker Compose Structure
```yaml
version: '3.9'
services:
  api:
    image: java-platform-api:latest
    ports: ["8080:8080"]
  postgres:
    image: postgres:15
  mongodb:
    image: mongo:6.0
  redis:
    image: redis:7.0
  nginx:
    image: nginx:latest
    ports: ["80:80", "443:443"]
  docker-executor:
    image: openjdk:17
    privileged: true
```

### Cloud Deployment (AWS/Render/Railway)
- API: Spring Boot on EC2 / Render / Railway
- Database: AWS RDS / MongoDB Atlas
- Cache: AWS ElastiCache (Redis)
- Storage: AWS S3
- CDN: CloudFront
- Domain: Route 53

---

*End of System Design Document*
