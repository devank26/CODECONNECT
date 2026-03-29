# Quick Start Guide - Java Platform v2.0

## Prerequisites

- Java 21+ (OpenJDK or Oracle JDK)
- Maven 3.8+
- Docker & Docker Compose
- Git (optional)

## 5-Minute Startup

### Step 1: Navigate to Project Directory
```bash
cd "c:\Users\devan\OneDrive\Desktop\java pbl\java-platform-v2"
```

### Step 2: Start Services with Docker (Recommended)
```bash
docker-compose up -d
```

Wait 30-40 seconds for all services to be healthy:
- MySQL: port 3306
- Redis: port 6379
- MongoDB: port 27017
- Backend: port 8080

### Step 3: Verify Services Are Running
```bash
# Check all services
docker-compose ps

# Check backend logs
docker-compose logs -f backend
```

### Step 4: Test the API
```bash
# Health check
curl http://localhost:8080/api/v1/health

# Register new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser"}'
```

### Step 5: Open Web Dashboard
```bash
# On Windows
start "c:\Users\devan\OneDrive\Desktop\java pbl\java-platform-v2\frontend\index.html"

# Or open in browser manually
```

## API Testing Examples

### Register User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "john_doe"}'
```

Response:
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "username": "john_doe",
  "sessionId": "uuid..."
}
```

### Compile and Run Java Code
```bash
# Save this as compile-request.json
{
  "sourceCode": "public class Hello {\n  public static void main(String[] args) {\n    System.out.println(\"Hello, World!\");\n  }\n}",
  "className": "Hello",
  "input": ""
}

# Execute
curl -X POST http://localhost:8080/api/v1/compile/run \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -H "Content-Type: application/json" \
  -d @compile-request.json
```

Response:
```json
{
  "success": true,
  "compiled": true,
  "output": "Hello, World!\n",
  "errors": [],
  "executionTime": 250
}
```

### Analyze Error
```bash
curl -X POST http://localhost:8080/api/v1/ai/analyze-error \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"error": "cannot find symbol: variable count"}'
```

Response:
```json
{
  "success": true,
  "error": "cannot find symbol: variable count",
  "explanation": "💡 The variable or method you're using hasn't been defined. Check spelling and scope.",
  "suggestions": [
    "• Check if the variable/method is spelled correctly",
    "• Ensure the variable is declared before use",
    "• Import the required class if using external libraries"
  ],
  "category": "COMPILATION_ERROR"
}
```

## Manual Setup (Without Docker)

### Step 1: Set Up Databases

**MySQL:**
```bash
mysql -u root -p
CREATE DATABASE javaplatform;
SOURCE schema.sql;
```

**Redis:**
```bash
# Start Redis
redis-server

# Or run as service
```

### Step 2: Update Configuration
Edit `backend/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/javaplatform
    username: root
    password: <YOUR_PASSWORD>
  redis:
    host: localhost
    port: 6379
```

### Step 3: Build Project
```bash
cd java-platform-v2
mvn clean install -DskipTests
```

### Step 4: Run Application
```bash
cd backend
mvn spring-boot:run
```

## Common Tasks

### View API Documentation
Open [README.md](./README.md)

### View All Database Tables
```bash
docker-compose exec mysql mysql -u root -ppassword javaplatform
SHOW TABLES;
DESCRIBE users;
```

### View Backend Logs
```bash
docker-compose logs -f backend
```

### Stop All Services
```bash
docker-compose down
```

### Rebuild Backend Image
```bash
docker-compose up --build backend
```

### Clear All Data
```bash
docker-compose down -v
docker-compose up -d
```

## Testing Endpoints

### Using Postman

1. **Import Collection**:
   - Create new request
   - Save 5 requests for: register, login, compile, chat, health

2. **Register User**:
   ```
   POST http://localhost:8080/api/v1/auth/register
   Body (JSON): {"username": "testuser"}
   ```

3. **Copy token** from response and use in Authorization header:
   ```
   Authorization: Bearer <token_from_response>
   ```

4. **Test other endpoints** with the token

### Using curl

Create `test.sh`:
```bash
#!/bin/bash

# Register
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "'$(date +%s)'"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

echo "Token: $TOKEN"

# Health check
curl -s -X GET http://localhost:8080/api/v1/health | json_pp

# Compile code
curl -s -X POST http://localhost:8080/api/v1/compile/run \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sourceCode":"public class Test{public static void main(String[]a){System.out.println(\"OK\");}}","className":"Test","input":""}' | json_pp
```

## Troubleshooting

### "Port 3306 already in use"
```bash
# Find and stop the process
netstat -ano | findstr 3306
taskkill /PID <PID> /F

# Or use different port in docker-compose.yml
```

### "Cannot connect to MySQL"
```bash
# Check if MySQL container is running
docker-compose ps

# Check logs
docker-compose logs mysql

# Restart MySQL
docker-compose restart mysql
```

### "Redis connection refused"
```bash
# Verify Redis is running
docker-compose logs redis

# Restart Redis
docker-compose restart redis
```

### "Compilation timeout"
- Check system resources
- Java code may have infinite loop
- Check logs for details

### "Out of memory"
- Increase Docker memory limits
- Check for memory leaks in code
- Review JVM settings

## Performance Tuning

### Increase Thread Pool
Edit `config/AsyncConfig.java`:
```java
executor.setCorePoolSize(10);
executor.setMaxPoolSize(50);
```

### Increase Cache TTL
Edit `application.yml`:
```yaml
spring:
  cache:
    redis:
      time-to-live: 3600000  # 1 hour
```

### Increase Database Connections
Edit `application.yml`:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
```

## Security Notes

### Change JWT Secret
Edit `application.yml`:
```yaml
jwt:
  secret: your-very-long-secret-key-min-32-chars
  expiration: 86400000
```

### Enable HTTPS (Production)
1. Generate certificate
2. Configure in `application.yml`
3. Update docker-compose for TLS

### Database Passwords
Change in `docker-compose.yml`:
```yaml
MYSQL_ROOT_PASSWORD: your_strong_password
MONGO_INITDB_ROOT_PASSWORD: your_strong_password
```

## Monitoring

### Check Container Health
```bash
docker-compose ps
```

### View Metrics
```bash
curl http://localhost:8080/api/v1/health
```

### View Performance
- Check Backend logs for timing info
- Monitor MySQL slow queries
- Use Redis CLI for cache stats

## Next Steps

1. ✅ Services running
2. ✅ API responding
3. 📝 Write integration tests
4. 📊 Load testing (JMeter)
5. 🔐 Security audit
6. 📈 Performance tuning
7. 🚀 Deploy to production

## Support

- Check logs: `docker-compose logs -f backend`
- Read API docs: [README.md](./README.md)
- View schema: [schema.sql](./schema.sql)
- Test UI: Open frontend/index.html

**You're ready to go! 🚀**
