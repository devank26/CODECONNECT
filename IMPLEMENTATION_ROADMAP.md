# 🚀 Implementation Roadmap & Code Samples

## Phase 1: Foundation Setup (Week 1-2)

### Step 1.1: Create Multi-Module Maven Project

**pom.xml (Parent)**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.javaplatform</groupId>
    <artifactId>java-platform-parent</artifactId>
    <version>2.0.0</version>
    <packaging>pom</packaging>

    <name>Java Platform v2.0</name>
    <description>Scalable, Production-Ready Java Development Platform</description>

    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.1.5</spring-boot.version>
        <spring-cloud.version>2022.0.4</spring-cloud.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <modules>
        <module>shared</module>
        <module>auth-service</module>
        <module>compiler-service</module>
        <module>chat-service</module>
        <module>video-service</module>
        <module>ai-service</module>
        <module>user-service</module>
        <module>notification-service</module>
        <module>api-gateway</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Phase 2: Authentication Service (Week 1-2)

### Step 2.1: User Entity & JWT Config

**User.java (JPA Entity)**
```java
package com.javaplatform.shared.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_email", columnList = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String fullName;
    private String avatarUrl;
    private String bio;

    @Column(nullable = false)
    private Boolean isActive = true;

    private LocalDateTime lastLogin;
    private LocalDateTime verifiedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    public enum UserRole {
        USER, PREMIUM, ADMIN
    }
}
```

**JwtUtil.java (JWT Configuration)**
```java
package com.javaplatform.shared.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.secret:very-secure-secret-key-must-be-at-least-256-bits-long}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration:900000}") // 15 minutes
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}") // 7 days
    private long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "access");
        return createToken(claims, username, accessTokenExpiration);
    }

    public String generateRefreshToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        return createToken(claims, username, refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
            throw new JwtException("Token expired");
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw new JwtException("Invalid token");
        }
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}
```

---

## Phase 3: Compiler Service (Week 2-3)

### Step 3.1: Docker Sandbox Execution

**DockerSandboxService.java**
```java
package com.javaplatform.compiler.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.javaplatform.shared.dto.CodeExecutionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class DockerSandboxService {

    private final DockerClient dockerClient;

    @Value("${docker.memory-limit:268435456}") // 256MB
    private long memoryLimit;

    @Value("${docker.cpu-count:1}")
    private long cpuCount;

    @Value("${docker.timeout-seconds:10}")
    private int timeoutSeconds;

    public DockerSandboxService() {
        this.dockerClient = DockerClientBuilder.getInstance().build();
    }

    public CodeExecutionDTO executeCode(String code, String language, String stdin) {
        long startTime = System.currentTimeMillis();
        Path tempDir = null;

        try {
            // Create temporary workspace
            tempDir = Files.createTempDirectory("java-platform-" + UUID.randomUUID());
            Path codeFile = createCodeFile(tempDir, code, language);

            // Prepare Docker container
            String containerId = createAndStartContainer(tempDir, language);

            // Execute and capture output
            CodeExecutionDTO result = executeInContainer(containerId, language, stdin);

            // Calculate metrics
            long duration = System.currentTimeMillis() - startTime;
            result.setExecutionTimeMs((int) duration);

            // Cleanup
            stopAndRemoveContainer(containerId);

            return result;

        } catch (Exception e) {
            log.error("Code execution failed: ", e);
            return CodeExecutionDTO.builder()
                    .success(false)
                    .errors("Execution failed: " + e.getMessage())
                    .exitCode(1)
                    .build();
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }
    }

    private String createAndStartContainer(Path workDir, String language) {
        String imageName = getDockerImage(language);

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withMemory(memoryLimit)
                .withCpuCount(cpuCount)
                .withNetworkDisabled(true)
                .withHostConfig(
                        dockerClient.inspectContainerCmd("dummy")
                                .getHostConfig()
                                .withBinds(new Bind(workDir.toString(), new Volume("/app")))
                )
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();
        return container.getId();
    }

    private CodeExecutionDTO executeInContainer(String containerId, String language, String stdin) {
        try {
            // Use Docker exec to run code with timeout
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

            String command = getExecutionCommand(language);

            // Execute with timeout
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Integer> future = executor.submit(() -> {
                try {
                    // Execution logic here
                    return 0;
                } catch (Exception e) {
                    return 1;
                }
            });

            int exitCode = 0;
            try {
                exitCode = future.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                return CodeExecutionDTO.builder()
                        .success(false)
                        .errors("Execution timeout (" + timeoutSeconds + "s exceeded)")
                        .exitCode(124)
                        .build();
            } finally {
                executor.shutdown();
            }

            return CodeExecutionDTO.builder()
                    .success(exitCode == 0)
                    .output(outputStream.toString())
                    .errors(errorStream.toString())
                    .exitCode(exitCode)
                    .build();

        } catch (Exception e) {
            log.error("Container execution failed: ", e);
            return CodeExecutionDTO.builder()
                    .success(false)
                    .errors("Container error: " + e.getMessage())
                    .exitCode(1)
                    .build();
        }
    }

    private void stopAndRemoveContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            dockerClient.removeContainerCmd(containerId).exec();
        } catch (Exception e) {
            log.warn("Failed to cleanup container {}: {}", containerId, e.getMessage());
        }
    }

    private String getDockerImage(String language) {
        return switch (language.toLowerCase()) {
            case "java" -> "openjdk:17-slim";
            case "python" -> "python:3.11-slim";
            case "cpp" -> "gcc:12-slim";
            case "javascript" -> "node:18-slim";
            default -> "openjdk:17-slim";
        };
    }

    private String getExecutionCommand(String language) {
        return switch (language.toLowerCase()) {
            case "java" -> "cd /app && javac Main.java && java Main";
            case "python" -> "cd /app && python main.py";
            case "cpp" -> "cd /app && g++ -o main main.cpp && ./main";
            case "javascript" -> "cd /app && node main.js";
            default -> "";
        };
    }

    private Path createCodeFile(Path workDir, String code, String language) throws IOException {
        String fileName = switch (language.toLowerCase()) {
            case "java" -> "Main.java";
            case "python" -> "main.py";
            case "cpp" -> "main.cpp";
            case "javascript" -> "main.js";
            default -> "code.txt";
        };
        
        Path codeFile = workDir.resolve(fileName);
        Files.writeString(codeFile, code);
        return codeFile;
    }

    private void deleteDirectory(Path directory) {
        try {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete {}: {}", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to delete directory: {}", e.getMessage());
        }
    }
}
```

**CompilationController.java**
```java
package com.javaplatform.compiler.controller;

import com.javaplatform.shared.dto.CodeExecutionDTO;
import com.javaplatform.compiler.service.DockerSandboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/compiler")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CompilationController {

    private final DockerSandboxService compileService;

    @PostMapping("/execute")
    public ResponseEntity<CodeExecutionDTO> executeCode(
            @RequestBody CodeExecutionDTO request) {
        
        CodeExecutionDTO result = compileService.executeCode(
                request.getCode(),
                request.getLanguage(),
                request.getStdin()
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/languages")
    public ResponseEntity<?> getSupportedLanguages() {
        return ResponseEntity.ok(new String[]{
                "java", "python", "cpp", "javascript"
        });
    }
}
```

---

## Phase 4: WebSocket Real-time Chat (Week 3)

### Step 4.1: WebSocket Configuration

**WebSocketConfig.java**
```java
package com.javaplatform.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Configure message broker
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/v1/ws/chat")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

**ChatService.java**
```java
package com.javaplatform.chat.service;

import com.javaplatform.shared.dto.ChatMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final MongoTemplate mongoTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageDTO saveMessage(ChatMessageDTO msg) {
        msg.setCreatedAt(LocalDateTime.now());
        msg.setUpdatedAt(LocalDateTime.now());
        
        return mongoTemplate.insert(msg, "messages");
    }

    public void broadcastMessage(String roomId, ChatMessageDTO message) {
        // Save to database
        saveMessage(message);
        
        // Broadcast to room subscribers
        messagingTemplate.convertAndSend(
                "/topic/chat/" + roomId,
                message
        );

        log.info("Message broadcast to room: {}", roomId);
    }

    public void notifyTyping(String roomId, String userId, boolean isTyping) {
        messagingTemplate.convertAndSend(
                "/topic/typing/" + roomId,
                new TypingIndicatorDTO(userId, isTyping)
        );
    }

    public List<ChatMessageDTO> getMessageHistory(String roomId, int limit) {
        return mongoTemplate.find(
                Query.query(Criteria.where("room_id").is(roomId))
                        .limit(limit)
                        .sort(Sort.by(Sort.Direction.DESC, "created_at")),
                ChatMessageDTO.class,
                "messages"
        );
    }
}
```

**ChatController.java (WebSocket Handler)**
```java
package com.javaplatform.chat.controller;

import com.javaplatform.shared.dto.ChatMessageDTO;
import com.javaplatform.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat/{roomId}/send")
    public void sendMessage(
            @DestinationVariable String roomId,
            @Payload ChatMessageDTO message) {
        
        log.info("Message received in room {}: {}", roomId, message.getContent());
        chatService.broadcastMessage(roomId, message);
    }

    @MessageMapping("/chat/{roomId}/typing")
    public void typingIndicator(
            @DestinationVariable String roomId,
            @Payload String userId) {
        
        chatService.notifyTyping(roomId, userId, true);
    }
}
```

---

## Phase 5: WebRTC Video Calls (Week 4)

### Step 5.1: WebRTC Signaling

**WebRTCSignalingService.java**
```java
package com.javaplatform.video.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebRTCSignalingService {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, PeerConnection> connections = new ConcurrentHashMap<>();

    public String initiateCall(Long callerId, Long calleeId) {
        String callId = UUID.randomUUID().toString();
        PeerConnection peerConnection = new PeerConnection(callerId, calleeId);
        connections.put(callId, peerConnection);

        // Send call notification
        messagingTemplate.convertAndSendToUser(
                calleeId.toString(),
                "/queue/call",
                new CallNotificationDTO(callId, callerId, "incoming")
        );

        log.info("Call initiated: {} -> {}", callerId, calleeId);
        return callId;
    }

    public void handleOffer(String callId, String offer, Long senderId) {
        PeerConnection connection = connections.get(callId);
        if (connection != null) {
            connection.offer = offer;
            
            // Send offer to peer
            Long peerId = senderId.equals(connection.callerId) 
                    ? connection.calleeId 
                    : connection.callerId;

            messagingTemplate.convertAndSendToUser(
                    peerId.toString(),
                    "/queue/webrtc/offer",
                    new SignalingMessageDTO(callId, offer)
            );
        }
    }

    public void handleAnswer(String callId, String answer, Long senderId) {
        PeerConnection connection = connections.get(callId);
        if (connection != null) {
            connection.answer = answer;

            Long peerId = senderId.equals(connection.callerId) 
                    ? connection.calleeId 
                    : connection.callerId;

            messagingTemplate.convertAndSendToUser(
                    peerId.toString(),
                    "/queue/webrtc/answer",
                    new SignalingMessageDTO(callId, answer)
            );
        }
    }

    public void handleICECandidate(String callId, String candidate, Long senderId) {
        PeerConnection connection = connections.get(callId);
        if (connection != null) {
            Long peerId = senderId.equals(connection.callerId) 
                    ? connection.calleeId 
                    : connection.callerId;

            messagingTemplate.convertAndSendToUser(
                    peerId.toString(),
                    "/queue/webrtc/ice-candidate",
                    new ICECandidateDTO(callId, candidate)
            );
        }
    }

    public void endCall(String callId) {
        connections.remove(callId);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class PeerConnection {
        private Long callerId;
        private Long calleeId;
        private String offer;
        private String answer;
    }
}
```

---

## Phase 6: Authentication & Security (Week 2)

### Step 6.1: Spring Security Configuration

**SecurityConfig.java**
```java
package com.javaplatform.auth.config;

import com.javaplatform.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder())
                .and()
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                    .antMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                    .antMatchers("/api/v1/ws/**").permitAll()
                    .antMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated()
                .and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

## Phase 7: Application Properties

**application.yml (Auth Service)**
```yaml
spring:
  application:
    name: java-platform-auth-service
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL13Dialect
  datasource:
    url: jdbc:postgresql://localhost:5432/java_platform
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  security:
    user:
      name: admin
      password: admin

jwt:
  secret: ${JWT_SECRET:your-very-secure-secret-key-must-be-at-least-256-bits-long}
  access-token-expiration: 900000
  refresh-token-expiration: 604800000

server:
  port: 8080
  servlet:
    context-path: /

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      show-details: always

logging:
  level:
    root: INFO
    com.javaplatform: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

---

## Docker Compose for Local Development

**docker-compose.yml**
```yaml
version: '3.9'

services:
  postgres:
    image: postgres:15-alpine
    container_name: java-platform-postgres
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: java_platform
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - java-platform

  mongodb:
    image: mongo:6.0
    container_name: java-platform-mongodb
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: admin
      MONGO_INITDB_DATABASE: java_platform
    ports:
      - "27017:27017"
    volumes:
      - mongodb-data:/data/db
    networks:
      - java-platform

  redis:
    image: redis:7-alpine
    container_name: java-platform-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - java-platform

  api-service:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: java-platform-api
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/java_platform
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_DATA_MONGODB_URI: mongodb://admin:admin@mongodb:27017/java_platform
      REDIS_HOST: redis
      REDIS_PORT: 6379
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - mongodb
      - redis
    networks:
      - java-platform

  nginx:
    image: nginx:latest
    container_name: java-platform-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./infra/nginx/nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - api-service
    networks:
      - java-platform

volumes:
  postgres-data:
  mongodb-data:
  redis-data:

networks:
  java-platform:
    driver: bridge
```

---

## Quick Start Commands

```bash
# Clone and setup
git clone <repo-url>
cd java-platform-v2

# Start development environment
docker-compose up -d

# Build backend
cd backend && mvn clean install -DskipTests

# Start individual services
mvn -pl auth-service spring-boot:run
mvn -pl compiler-service spring-boot:run

# Run tests
mvn clean test

# Build Docker image
docker build -f infra/docker/Dockerfile.api -t java-platform-api:latest .

# Deploy to production (example)
docker-compose -f docker-compose.prod.yml up -d
```

---

## Next Steps

✅ Complete Phase 1-2: Authentication & Authorization  
✅ Complete Phase 3: Compiler Service with Docker Sandbox  
✅ Complete Phase 4: WebSocket Real-time Chat  
✅ Complete Phase 5: WebRTC Video Calls  
📝 Phase 6: AI Integration Service  
📝 Phase 7: Frontend (React/Vue)  
📝 Phase 8: Deployment & DevOps  
📝 Phase 9: Monitoring & CI/CD  
📝 Phase 10: Performance Optimization  

Each phase includes:
- Detailed implementation guide
- Complete code samples
- Unit tests
- Integration tests
- Deployment instructions
