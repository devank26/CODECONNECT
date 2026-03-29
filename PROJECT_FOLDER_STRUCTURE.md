# 📁 Project Folder Structure

```
java-platform-v2/
│
├── backend/                              # Spring Boot Backend
│   ├── pom.xml                          # Parent POM
│   │
│   ├── shared/                          # Shared utilities & models
│   │   ├── pom.xml
│   │   ├── src/main/java/com/javaplatform/
│   │   │   ├── dto/                     # Data Transfer Objects
│   │   │   │   ├── UserDTO.java
│   │   │   │   ├── CodeExecutionDTO.java
│   │   │   │   ├── ChatMessageDTO.java
│   │   │   │   └── VideoCallDTO.java
│   │   │   ├── entity/                  # JPA Entities
│   │   │   │   ├── User.java
│   │   │   │   ├── CodeSnippet.java
│   │   │   │   ├── ExecutionHistory.java
│   │   │   │   ├── Call.java
│   │   │   │   └── Session.java
│   │   │   ├── exception/               # Custom Exceptions
│   │   │   │   ├── InvalidToken.java
│   │   │   │   ├── CompilationException.java
│   │   │   │   ├── APIException.java
│   │   │   │   └── ValidationException.java
│   │   │   ├── constant/                # Constants & Enums
│   │   │   │   ├── ErrorCode.java
│   │   │   │   ├── ProgrammingLanguage.java
│   │   │   │   └── ResponseCode.java
│   │   │   ├── utils/                   # Utility Classes
│   │   │   │   ├── JwtUtil.java
│   │   │   │   ├── EncryptionUtil.java
│   │   │   │   ├── ValidationUtil.java
│   │   │   │   └── DateTimeUtil.java
│   │   │   └── config/                  # Shared Configuration
│   │   │       ├── SecurityConfig.java
│   │   │       ├── JacksonConfig.java
│   │   │       └── CorsConfig.java
│   │
│   ├── api-gateway/                     # API Gateway (Optional)
│   │   ├── pom.xml
│   │   ├── src/
│   │   │   └── main/java/com/javaplatform/gateway/
│   │   │       ├── controller/
│   │   │       ├── filter/
│   │   │       └── config/
│   │   └── application.yml
│   │
│   ├── auth-service/                    # Authentication Microservice
│   │   ├── pom.xml
│   │   ├── src/main/java/com/javaplatform/auth/
│   │   │   ├── controller/
│   │   │   │   └── AuthController.java
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── TokenService.java
│   │   │   │   ├── UserService.java
│   │   │   │   └── PasswordService.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── SessionRepository.java
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── CustomUserDetailsService.java
│   │   │   ├── event/
│   │   │   │   └── UserRegisteredEvent.java
│   │   │   └── config/
│   │   ├── src/test/java/
│   │   │   └── com/javaplatform/auth/
│   │   │       └── AuthServiceTest.java
│   │   └── application.yml
│   │
│   ├── compiler-service/                # Code Compilation Microservice
│   │   ├── pom.xml
│   │   ├── src/main/java/com/javaplatform/compiler/
│   │   │   ├── controller/
│   │   │   │   └── CompilationController.java
│   │   │   ├── service/
│   │   │   │   ├── CompilationService.java
│   │   │   │   ├── DockerSandboxService.java
│   │   │   │   ├── CodeValidationService.java
│   │   │   │   └── LanguageSupportService.java
│   │   │   ├── repository/
│   │   │   │   └── ExecutionHistoryRepository.java
│   │   │   ├── docker/
│   │   │   │   ├── DockerConfig.java
│   │   │   │   └── SandboxExecutor.java
│   │   │   └── config/
│   │   ├── docker/
│   │   │   ├── Dockerfile.java
│   │   │   ├── Dockerfile.python
│   │   │   └── Dockerfile.cpp
│   │   └── application.yml
│   │
│   ├── chat-service/                    # Real-time Chat Microservice
│   │   ├── pom.xml
│   │   ├── src/main/java/com/javaplatform/chat/
│   │   │   ├── controller/
│   │   │   │   ├── ChatController.java
│   │   │   │   └── WebSocketController.java
│   │   │   ├── service/
│   │   │   │   ├── ChatService.java
│   │   │   │   ├── WebSocketService.java
│   │   │   │   ├── PresenceService.java
│   │   │   │   └── TypingIndicatorService.java
│   │   │   ├── repository/
│   │   │   │   └── ChatMessageRepository.java
│   │   │   ├── event/
│   │   │   │   ├── MessagePublisher.java
│   │   │   │   └── PresenceEvent.java
│   │   │   ├── handler/
│   │   │   │   └── WebSocketEventHandler.java
│   │   │   └── config/
│   │   │       ├── WebSocketConfig.java
│   │   │       └── StompConfig.java
│   │   └── application.yml
│   │
│   ├── video-service/                   # Video Call Microservice
│   │   ├── pom.xml
│   │   ├── src/main/java/com/javaplatform/video/
│   │   │   ├── controller/
│   │   │   │   ├── CallController.java
│   │   │   │   └── WebRTCController.java
│   │   │   ├── service/
│   │   │   │   ├── CallService.java
│   │   │   │   ├── WebRTCService.java
│   │   │   │   ├── TurnServerService.java
│   │   │   │   └── ScreenShareService.java
│   │   │   ├── repository/
│   │   │   │   └── CallRepository.java
│   │   │   ├── signal/
│   │   │   │   └── SignalingHandler.java
│   │   │   └── config/
│   │   │       ├── WebSocketConfig.java
│   │   │       └── WebRTCConfig.java
│   │   └── application.yml
│   │
│   ├── ai-service/                      # AI Assistant Microservice
│   │   ├── pom.xml
│   │   ├── src/main/java/com/javaplatform/ai/
│   │   │   ├── controller/
│   │   │   │   └── AIController.java
│   │   │   ├── service/
│   │   │   │   ├── AIService.java
│   │   │   │   ├── OpenAIClient.java
│   │   │   │   ├── CodeExplainerService.java
│   │   │   │   ├── ErrorDebuggerService.java
│   │   │   │   └── ConversationService.java
│   │   │   ├── repository/
│   │   │   │   └── ConversationRepository.java
│   │   │   ├── client/
│   │   │   │   └── OpenAIApiClient.java
│   │   │   └── config/
│   │   └── application.yml
│   │
│   ├── user-service/                    # User Management Microservice
│   │   ├── pom.xml
│   │   ├── src/main/java/com/javaplatform/user/
│   │   │   ├── controller/
│   │   │   │   └── UserProfileController.java
│   │   │   ├── service/
│   │   │   │   ├── UserProfileService.java
│   │   │   │   └── AchievementService.java
│   │   │   └── repository/
│   │   │       └── UserProfileRepository.java
│   │   └── application.yml
│   │
│   ├── notification-service/            # Notification Microservice
│   │   ├── pom.xml
│   │   ├── src/main/java/com/javaplatform/notification/
│   │   │   ├── service/
│   │   │   │   ├── NotificationService.java
│   │   │   │   ├── EmailService.java
│   │   │   │   └── PushNotificationService.java
│   │   │   └── event/
│   │   │       └── NotificationEventListener.java
│   │   └── application.yml
│   │
│   └── docker-compose.yml               # Local Development Setup
│
├── frontend/                            # Frontend Application
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts (or webpack.config.js)
│   ├── public/
│   │   ├── index.html
│   │   ├── favicon.ico
│   │   └── manifest.json
│   │
│   ├── src/
│   │   ├── index.tsx (or main.ts)
│   │   ├── App.tsx
│   │   ├── types/
│   │   │   ├── User.ts
│   │   │   ├── Message.ts
│   │   │   ├── CodeExecution.ts
│   │   │   └── VideoCall.ts
│   │   │
│   │   ├── components/
│   │   │   ├── auth/
│   │   │   │   ├── LoginForm.tsx
│   │   │   │   ├── RegisterForm.tsx
│   │   │   │   └── PasswordReset.tsx
│   │   │   ├── editor/
│   │   │   │   ├── CodeEditor.tsx
│   │   │   │   ├── OutputConsole.tsx
│   │   │   │   └── ErrorPanel.tsx
│   │   │   ├── chat/
│   │   │   │   ├── ChatWindow.tsx
│   │   │   │   ├── MessageList.tsx
│   │   │   │   ├── InputBox.tsx
│   │   │   │   └── TypingIndicator.tsx
│   │   │   ├── video/
│   │   │   │   ├── VideoWindow.tsx
│   │   │   │   ├── RemoteVideo.tsx
│   │   │   │   ├── LocalVideo.tsx
│   │   │   │   └── CallControls.tsx
│   │   │   ├── ai/
│   │   │   │   ├── AIChat.tsx
│   │   │   │   └── AIResponse.tsx
│   │   │   ├── shared/
│   │   │   │   ├── Header.tsx
│   │   │   │   ├── Sidebar.tsx
│   │   │   │   ├── Modal.tsx
│   │   │   │   └── Toast.tsx
│   │   │   └── layout/
│   │   │       ├── MainLayout.tsx
│   │   │       └── DashboardLayout.tsx
│   │   │
│   │   ├── pages/
│   │   │   ├── LoginPage.tsx
│   │   │   ├── DashboardPage.tsx
│   │   │   ├── EditorPage.tsx
│   │   │   ├── ChatPage.tsx
│   │   │   ├── VideoPage.tsx
│   │   │   ├── ProfilePage.tsx
│   │   │   └── NotFoundPage.tsx
│   │   │
│   │   ├── hooks/
│   │   │   ├── useAuth.ts
│   │   │   ├── useWebSocket.ts
│   │   │   ├── useWebRTC.ts
│   │   │   └── useFetch.ts
│   │   │
│   │   ├── services/
│   │   │   ├── api.ts
│   │   │   ├── authService.ts
│   │   │   ├── chatService.ts
│   │   │   ├── videoService.ts
│   │   │   ├── compilerService.ts
│   │   │   └── AIService.ts
│   │   │
│   │   ├── store/ (Redux/Pinia)
│   │   │   ├── slices/
│   │   │   │   ├── authSlice.ts
│   │   │   │   ├── chatSlice.ts
│   │   │   │   ├── editorSlice.ts
│   │   │   │   └── userSlice.ts
│   │   │   ├── middleware/
│   │   │   │   └── authMiddleware.ts
│   │   │   └── store.ts
│   │   │
│   │   ├── styles/
│   │   │   ├── global.css
│   │   │   ├── themes.css
│   │   │   ├── variables.css
│   │   │   └── responsive.css
│   │   │
│   │   ├── utils/
│   │   │   ├── validators.ts
│   │   │   ├── formatters.ts
│   │   │   └── helpers.ts
│   │   │
│   │   └── config/
│   │       ├── apiConfig.ts
│   │       └── constants.ts
│   │
│   ├── tests/
│   │   ├── unit/
│   │   ├── integration/
│   │   └── e2e/
│   │
│   └── .env.example
│
├── infra/                               # Infrastructure & DevOps
│   ├── docker/
│   │   ├── Dockerfile.api
│   │   ├── Dockerfile.frontend
│   │   └── Dockerfile.nginx
│   │
│   ├── kubernetes/                      # K8s manifests (optional)
│   │   ├── namespace.yaml
│   │   ├── deployment-api.yaml
│   │   ├── deployment-frontend.yaml
│   │   ├── service-api.yaml
│   │   ├── ingress.yaml
│   │   └── configmap.yaml
│   │
│   ├── terraform/ (optional)            # Infrastructure as Code
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── aws/
│   │   │   ├── ec2.tf
│   │   │   ├── rds.tf
│   │   │   └── s3.tf
│   │   └── gcp/
│   │
│   ├── nginx/
│   │   └── nginx.conf
│   │
│   └── scripts/
│       ├── deploy.sh
│       ├── setup-db.sh
│       └── health-check.sh
│
├── docs/                                # Documentation
│   ├── API.md                          # API Documentation
│   ├── ARCHITECTURE.md                 # Architecture Guide
│   ├── SETUP.md                        # Setup Instructions
│   ├── DEPLOYMENT.md                   # Deployment Guide
│   ├── CONTRIBUTING.md
│   └── diagrams/
│       ├── system-architecture.png
│       ├── data-flow.png
│       └── deployment-flow.png
│
├── .github/                             # GitHub Configuration
│   ├── workflows/
│   │   ├── ci-backend.yml
│   │   ├── ci-frontend.yml
│   │   ├── deploy-staging.yml
│   │   └── deploy-production.yml
│   └── ISSUE_TEMPLATE/
│       └── bug_report.md
│
├── scripts/                             # Utility Scripts
│   ├── setup.sh
│   ├── start-dev.sh
│   ├── build.sh
│   ├── test.sh
│   └── clean.sh
│
├── README.md                            # Main README
├── docker-compose.yml                   # Local Development
├── docker-compose.prod.yml              # Production Setup
├── .gitignore
├── .env.example
└── LICENSE
```

## Key Directories Explained

### Backend Structure
- **shared/**: Common libraries, DTOs, entities (shared by all services)
- **auth-service/**: JWT authentication, user registration, token management
- **compiler-service/**: Code execution in Docker sandboxes
- **chat-service/**: WebSocket-based real-time messaging
- **video-service/**: WebRTC signaling and call management
- **ai-service/**: AI chatbot and code analysis
- **user-service/**: User profiles and achievements
- **notification-service/**: Email, push notifications

### Frontend Structure
- **components/**: Reusable React/Vue components
- **pages/**: Full-page components (routed)
- **services/**: API communication layer
- **hooks/**: React custom hooks for state management
- **store/**: Redux/Pinia state management
- **types/**: TypeScript type definitions

### Infrastructure
- **docker/**: Dockerfiles for containerization
- **kubernetes/**: K8s deployments (optional, for scalability)
- **infra/**: Terraform for AWS/cloud infrastructure
- **scripts/**: Deployment and setup automation

This structure supports:
✅ Microservices architecture
✅ Independent scaling
✅ Technology independence
✅ Parallel development
✅ Easy testing & deployment
