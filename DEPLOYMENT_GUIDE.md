# 📦 Deployment Guide & Production Setup

## Table of Contents
1. [Local Development Setup](#local-development-setup)
2. [Docker Deployment](#docker-deployment)
3. [Cloud Deployment (AWS/Render/Railway)](#cloud-deployment)
4. [CI/CD Pipeline](#cicd-pipeline)
5. [Monitoring & Logging](#monitoring--logging)
6. [Security Hardening](#security-hardening)

---

## Local Development Setup

### Prerequisites
- Java 17+ JDK
- Docker & Docker Compose
- Maven 3.8+
- Node.js 18+ (for frontend)
- Git

### Installation Steps

```bash
# 1. Clone repository
git clone https://github.com/yourusername/java-platform-v2.git
cd java-platform-v2

# 2. Create environment file
cp .env.example .env
# Edit .env with your configuration

# 3. Start all services with Docker Compose
docker-compose up -d

# 4. Wait for services to be ready
docker-compose logs -f api-service

# 5. Initialize database schema
docker-compose exec postgres psql -U postgres -d java_platform -f init.sql

# 6. Start frontend development server (in separate terminal)
cd frontend
npm install
npm run dev

# Application URLs:
# - API: http://localhost:8080
# - Frontend: http://localhost:3000  
# - pgAdmin: http://localhost:5050
# - MongoDB Express: http://localhost:8081
```

### Common Local Commands

```bash
# View logs
docker-compose logs -f api-service

# Stop all services
docker-compose down

# Reset everything (clean slate)
docker-compose down -v

# Rebuild services after code changes
docker-compose up -d --build

# Access database directly
docker-compose exec postgres psql -U postgres -d java_platform

# Run tests
cd backend && mvn clean test

# Build production JAR
cd backend && mvn clean package -DskipTests
```

---

## Docker Deployment

### Production Dockerfile

**Dockerfile (Multi-stage)**
```dockerfile
# Stage 1: Build
FROM maven:3.8-openjdk-17 AS builder
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:resolve
COPY backend/ .
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:17-slim
WORKDIR /app

# Install security updates
RUN apt-get update && apt-get upgrade -y \
    && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*

# Copy JAR from builder
COPY --from=builder /app/target/java-platform-api-2.0.0.jar app.jar

# Create non-root user for security
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run application with security options
ENTRYPOINT ["java", \
    "-Dspring.profiles.active=production", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-Dcom.sun.management.jmxremote.port=9010", \
    "-Dcom.sun.management.jmxremote.authenticate=false", \
    "-Dcom.sun.management.jmxremote.ssl=false", \
    "-jar", "app.jar"]
```

### Production Docker Compose

**docker-compose.prod.yml**
```yaml
version: '3.9'

services:
  postgres:
    image: postgres:15-alpine
    container_name: java-platform-postgres-prod
    restart: always
    environment:
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_DB: java_platform_prod
    ports:
      - "127.0.0.1:5432:5432"  # Expose only to localhost
    volumes:
      - /data/postgres:/var/lib/postgresql/data
    networks:
      - java-platform-prod
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER}"]
      interval: 10s
      timeout: 5s
      retries: 5

  mongodb:
    image: mongo:6.0
    container_name: java-platform-mongodb-prod
    restart: always
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_USER}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD}
      MONGO_INITDB_DATABASE: java_platform_prod
    ports:
      - "127.0.0.1:27017:27017"  # Expose only to localhost
    volumes:
      - /data/mongodb:/data/db
    networks:
      - java-platform-prod
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh localhost:27017/test --quiet
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: java-platform-redis-prod
    restart: always
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes
    ports:
      - "127.0.0.1:6379:6379"  # Expose only to localhost
    volumes:
      - /data/redis:/data
    networks:
      - java-platform-prod
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  api-service:
    image: java-platform-api:${API_VERSION}
    container_name: java-platform-api-prod
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: production
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/java_platform_prod
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_DATA_MONGODB_URI: mongodb://${MONGO_USER}:${MONGO_PASSWORD}@mongodb:27017/java_platform_prod
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      GEMINI_API_KEY: ${GEMINI_API_KEY}
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      mongodb:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - java-platform-prod
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health/liveness"]
      interval: 30s
      timeout: 10s
      retries: 3

  nginx:
    image: nginx:latest
    container_name: java-platform-nginx-prod
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /etc/nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro
      - /var/www/certbot:/var/www/certbot:ro
    depends_on:
      - api-service
    networks:
      - java-platform-prod
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost/health"]
      interval: 30s
      timeout: 10s
      retries: 3

networks:
  java-platform-prod:
    driver: bridge

volumes:
  postgres-data:
  mongodb-data:
  redis-data:
```

---

## Cloud Deployment

### AWS EC2 Deployment

**1. Create EC2 Instance**
```bash
# Launch Ubuntu 22.04 LTS instance
# - Instance Type: t3.medium (minimum)
# - Storage: 30GB EBS
# - Security Group: Allow 80, 443, 8080

# SSH into instance
ssh -i your-key.pem ubuntu@your-instance-ip

# Update system
sudo apt-get update && sudo apt-get upgrade -y

# Install Docker
sudo apt-get install -y docker.io docker-compose
sudo usermod -aG docker ubuntu

# Clone repository
git clone <repo-url>
cd java-platform-v2

# Create .env file
nano .env
# (Add your production configuration)

# Build Docker image
docker build -f infra/docker/Dockerfile.api -t java-platform-api:latest .

# Push to Docker Hub
docker tag java-platform-api:latest yourhub/java-platform-api:latest
docker push yourhub/java-platform-api:latest

# Start services
docker-compose -f docker-compose.prod.yml up -d
```

### Render Deployment

**render.yaml**
```yaml
services:
  - type: web
    name: java-platform-api
    runtime: docker
    dockerfilePath: ./Dockerfile
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: DATABASE_URL
        fromDatabase:
          name: java-platform-db
          property: connectionString
    healthCheckPath: /actuator/health
    
  - type: web
    name: java-platform-frontend
    runtime: static
    buildCommand: cd frontend && npm install && npm run build
    staticPublishPath: frontend/dist
    envVars:
      - key: VITE_API_URL
        value: "https://java-platform-api.onrender.com"

  - type: pserv
    name: java-platform-db
    runtime: postgres
    plan: free
    ipAllowList: []
```

### Railway Deployment

**railway.json**
```json
{
  "id": "java-platform",
  "name": "Java Platform v2.0 - Production",
  "description": "Scalable Java development platform",
  "services": [
    {
      "type": "postgres",
      "name": "database",
      "version": "15"
    },
    {
      "type": "redis",
      "name": "cache",
      "version": "7"
    },
    {
      "type": "docker",
      "name": "api-service",
      "dockerfile": "./Dockerfile",
      "healthcheck": "/actuator/health"
    }
  ],
  "env": {
    "SPRING_PROFILES_ACTIVE": "production"
  }
}
```

---

## CI/CD Pipeline

### GitHub Actions Workflow

**.github/workflows/deploy-production.yml**
```yaml
name: Deploy to Production

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: java-platform-api

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      
      mongodb:
        image: mongo:6.0
        options: >-
          --health-cmd mongosh --eval 'db.runCommand({ ping: 1 })'
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    
    - name: Build with Maven
      run: cd backend && mvn clean package -DskipTests
    
    - name: Run Tests
      run: cd backend && mvn test
    
    - name: Code Coverage
      uses: codecov/codecov-action@v3
    
    - name: SonarQube Scan
      uses: SonarSource/sonarcloud-github-action@master
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

  build-docker-image:
    needs: build-and-test
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v2
    
    - name: Log in to Container Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    
    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v4
      with:
        images: ${{ env.REGISTRY }}/${{ github.repository }}/${{ env.IMAGE_NAME }}
        tags: |
          type=ref,event=branch
          type=semver,pattern={{version}}
          type=semver,pattern={{major}}.{{minor}}
    
    - name: Build and push Docker image
      uses: docker/build-push-action@v4
      with:
        context: ./backend
        file: ./infra/docker/Dockerfile.api
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}

  deploy:
    needs: build-docker-image
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Deploy to AWS
      env:
        AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
        AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
      run: |
        # Install AWS CLI
        curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
        unzip awscliv2.zip
        sudo ./aws/install
        
        # Deploy to ECS, EC2, or Lambda
        # Example for EC2:
        aws ssm send-command \
          --document-name "AWS-RunShellScript" \
          --parameters 'commands=["cd /opt/java-platform && docker-compose pull && docker-compose up -d"]' \
          --instance-ids ${{ secrets.AWS_INSTANCE_ID }}
        
        # Update CloudFront cache
        aws cloudfront create-invalidation \
          --distribution-id ${{ secrets.CLOUDFRONT_DIST_ID }} \
          --paths "/*"

  notify:
    needs: [build-and-test, build-docker-image, deploy]
    runs-on: ubuntu-latest
    if: always()
    
    steps:
    - name: Send Slack Notification
      uses: slackapi/slack-github-action@v1.24.0
      with:
        text: |
          Deployment Status: ${{ job.status }}
          Repository: ${{ github.repository }}
          Branch: ${{ github.ref }}
          Commit: ${{ github.sha }}
      env:
        SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

---

## Monitoring & Logging

### Prometheus Metrics

**prometheus.yml**
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spring-boot-api'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

### ELK Stack (Elasticsearch, Logstash, Kibana)

**logback-spring.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE_JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/spring-boot.json</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/spring-boot-%d{yyyy-MM-dd}.%i.json</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE_JSON"/>
    </root>
</configuration>
```

---

## Security Hardening

### SSL/TLS Certificate Setup

```bash
# Using Let's Encrypt with Certbot
sudo apt-get install -y certbot python3-certbot-nginx

# Issue certificate
sudo certbot certonly --standalone \
  -d yourdomain.com \
  -d api.yourdomain.com \
  --email admin@yourdomain.com \
  --agree-tos \
  --non-interactive

# Auto-renewal
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer
```

### Nginx Configuration with SSL

**nginx.conf (Production)**
```nginx
upstream api_backend {
    server api-service:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name api.yourdomain.com;
    
    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;
    
    # SSL Certificates
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    
    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;
    
    # Rate Limiting
    limit_req_zone $binary_remote_addr zone=general:10m rate=10r/s;
    limit_req_zone $binary_remote_addr zone=auth:10m rate=5r/m;
    
    limit_req zone=general burst=20 nodelay;
    
    # Proxy Configuration
    location / {
        limit_req zone=auth;
        
        proxy_pass http://api_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # WebSocket Support
    location /api/v1/ws/ {
        proxy_pass http://api_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 86400;
    }
    
    # Health Check
    location /health {
        proxy_pass http://api_backend/actuator/health;
        access_log off;
    }
}
```

---

## Monitoring Checklist

- ✅ Application Health Checks
- ✅ Database Connectivity
- ✅ Redis Cache Health
- ✅ Disk Space & Memory Usage
- ✅ API Response Times
- ✅ Error Rates & Exception Tracking
- ✅ Security Audit Logs
- ✅ User Activity Logs
- ✅ Backup Status
- ✅ SSL Certificate Expiration

---

## Rollback Procedure

```bash
# Tag current version as backup
docker tag java-platform-api:latest java-platform-api:pre-rollback-$(date +%Y%m%d-%H%M%S)

# Pull previous stable version
docker pull yourhub/java-platform-api:v2.0.0-stable

# Update docker-compose
sed -i 's/java-platform-api:latest/java-platform-api:v2.0.0-stable/' docker-compose.prod.yml

# Restart services
docker-compose -f docker-compose.prod.yml up -d

# Verify
docker-compose logs -f api-service
```

---

## Post-Deployment Checklist

- [ ] Database migrations completed
- [ ] Health checks passing
- [ ] API endpoints responding
- [ ] WebSocket connections working
- [ ] SSL certificate valid
- [ ] Logs aggregating
- [ ] Monitoring alerts configured
- [ ] Backup jobs running
- [ ] Security scan completed
- [ ] Performance baseline established

*End of Deployment Guide*
