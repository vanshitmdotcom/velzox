# 📡 API Monitor - Developer-First API Monitoring SaaS

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring_Boot-3.2-green?style=for-the-badge&logo=spring-boot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/React-18-blue?style=for-the-badge&logo=react" alt="React 18">
  <img src="https://img.shields.io/badge/PostgreSQL-15-blue?style=for-the-badge&logo=postgresql" alt="PostgreSQL">
</p>

A focused, affordable, and reliable API monitoring tool that:
- ✅ Periodically hits APIs (GET/POST)
- ✅ Supports authentication (Bearer Token, API Key, Basic Auth)
- ✅ Measures availability (uptime), latency, and status correctness
- ✅ Sends real-time alerts when something breaks

**Not** a full observability platform. **Not** Datadog. Just a focused, affordable tool.

---

## 🎯 Target Users

- Backend engineers
- Small startups
- SaaS founders
- Freelancers managing production APIs

---

## ✨ Features

### Core MVP Features

| Feature | Description |
|---------|-------------|
| **Endpoint Monitoring** | Monitor any HTTP endpoint with custom headers, body, and auth |
| **Multi-Method Support** | GET, POST, PUT, DELETE, PATCH, HEAD |
| **Authentication** | Bearer Token, API Key, Basic Auth (encrypted at rest) |
| **Latency Tracking** | Measure response times, set thresholds for alerts |
| **Status Verification** | Verify expected HTTP status codes |
| **Flexible Intervals** | Check every 30s (PRO), 1m (Starter), or 5m (Free) |
| **Email Alerts** | Instant notifications when endpoints fail |
| **Incident Tracking** | Group related failures, prevent alert spam |
| **Dashboard** | Real-time status overview with uptime percentages |

### Alert Types

| Type | Trigger |
|------|---------|
| 🔴 Endpoint Down | Wrong status code or error |
| ⏱️ Timeout | Request took too long |
| 🔐 Auth Failure | 401 Unauthorized |
| 🔒 SSL Error | Certificate issues |
| 🐢 Latency Breach | Response time exceeded threshold |
| ✅ Recovery | Endpoint came back up |

---

## 💰 Pricing

| Plan | Price | Endpoints | Check Interval | History | Slack/Webhook |
|------|-------|-----------|----------------|---------|---------------|
| **Free** | ₹0/mo | 2 | 5 minutes | 24 hours | ❌ |
| **Starter** | ₹499/mo | 10 | 1 minute | 7 days | ❌ |
| **Pro** | ₹999/mo | 50 | 30 seconds | 30 days | ✅ |

**2×–8× cheaper than Pingdom/Datadog** by design.

---

## 🏗️ Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   React SPA     │────▶│  Spring Boot    │────▶│   PostgreSQL    │
│   (Frontend)    │     │   (Backend)     │     │   (Database)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │
                               ▼
                        ┌─────────────────┐
                        │  Monitored APIs │
                        │  (Your Services)│
                        └─────────────────┘
```

### Tech Stack

| Component | Technology |
|-----------|------------|
| Backend | Java 17, Spring Boot 3.2, Spring Security, Spring WebFlux |
| Frontend | React 18, TypeScript, Tailwind CSS, React Query |
| Database | PostgreSQL 15 (H2 for development) |
| HTTP Client | WebClient (async, non-blocking) |
| Authentication | JWT (HMAC-SHA256) |
| Encryption | AES-256-GCM for credentials |

---

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Node.js 18+
- Docker & Docker Compose (optional)

### Option 1: Docker Compose (Recommended)

```bash
# Clone and start
cd api-monitor
docker-compose up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

Access:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- H2 Console: http://localhost:8080/api/h2-console (dev only)

### Option 2: Local Development

**Backend:**
```bash
cd backend

# Run with Maven
./mvnw spring-boot:run

# Or build and run JAR
./mvnw clean package
java -jar target/api-monitor-1.0.0.jar
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

### Option 3: Development Mode (H2 Database)

The application uses H2 in-memory database by default for development. No additional setup required!

---

## 📁 Project Structure

```
api-monitor/
├── backend/                    # Spring Boot application
│   ├── src/main/java/com/velzox/apimonitor/
│   │   ├── config/            # Configuration classes
│   │   ├── controller/        # REST controllers
│   │   ├── dto/               # Data transfer objects
│   │   ├── entity/            # JPA entities
│   │   ├── exception/         # Custom exceptions
│   │   ├── repository/        # JPA repositories
│   │   ├── scheduler/         # Monitoring scheduler
│   │   ├── security/          # JWT & security
│   │   └── service/           # Business logic
│   ├── src/main/resources/
│   │   └── application.yml    # Configuration
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                   # React application
│   ├── src/
│   │   ├── components/        # Reusable components
│   │   ├── context/           # React contexts
│   │   ├── pages/             # Page components
│   │   ├── services/          # API services
│   │   ├── types/             # TypeScript types
│   │   └── App.tsx            # Main app
│   ├── Dockerfile
│   └── package.json
│
├── docker-compose.yml          # Docker orchestration
└── README.md
```

---

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection URL | H2 in-memory |
| `DATABASE_USERNAME` | Database username | sa |
| `DATABASE_PASSWORD` | Database password | (empty) |
| `JWT_SECRET` | JWT signing key (min 32 chars) | (dev key) |
| `ENCRYPTION_SECRET` | Credential encryption key | (dev key) |
| `MAIL_HOST` | SMTP server | smtp.gmail.com |
| `MAIL_PORT` | SMTP port | 587 |
| `MAIL_USERNAME` | SMTP username | (empty) |
| `MAIL_PASSWORD` | SMTP password | (empty) |
| `CORS_ALLOWED_ORIGINS` | Frontend URLs | localhost:3000,5173 |

### Production Configuration

For production, override these settings via environment variables:

```bash
export DATABASE_URL=jdbc:postgresql://your-db-host:5432/apimonitor
export JWT_SECRET=your-very-long-and-secure-jwt-secret-key-here
export ENCRYPTION_SECRET=your-32-character-encryption-key!
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
```

---

## 📡 API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login and get JWT |
| GET | `/api/v1/auth/me` | Get current user |

### Projects

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/projects` | List all projects |
| POST | `/api/v1/projects` | Create project |
| PUT | `/api/v1/projects/{id}` | Update project |
| DELETE | `/api/v1/projects/{id}` | Delete project |

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/endpoints?projectId={id}` | List endpoints |
| POST | `/api/v1/endpoints` | Create endpoint |
| PUT | `/api/v1/endpoints/{id}` | Update endpoint |
| DELETE | `/api/v1/endpoints/{id}` | Delete endpoint |
| PATCH | `/api/v1/endpoints/{id}/toggle` | Enable/disable |

### Credentials

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/credentials?projectId={id}` | List credentials |
| POST | `/api/v1/credentials` | Create credential |
| PUT | `/api/v1/credentials/{id}` | Update credential |
| DELETE | `/api/v1/credentials/{id}` | Delete credential |

### Dashboard

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/dashboard` | Get dashboard overview |

### Alerts

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/alerts` | List alerts |
| POST | `/api/v1/alerts/{id}/acknowledge` | Acknowledge alert |

---

## 🔐 Security Features

1. **JWT Authentication** - Stateless, scalable auth
2. **BCrypt Password Hashing** - Industry standard
3. **AES-256-GCM Encryption** - Credentials encrypted at rest
4. **Credential Masking** - Values never exposed in API responses
5. **CORS Protection** - Configurable allowed origins
6. **Input Validation** - All inputs validated

### Important Security Notes

- Change default JWT and encryption secrets in production
- Credentials are stored encrypted, never in plain text
- Response bodies from monitored APIs are NOT stored
- Use service tokens, not user login credentials

---

## 🛠️ Development

### Running Tests

```bash
cd backend
./mvnw test
```

### Building for Production

```bash
# Backend
cd backend
./mvnw clean package -Pproduction

# Frontend
cd frontend
npm run build
```

### Database Migrations

The application uses Hibernate's auto-update feature. For production, consider using Flyway or Liquibase for controlled migrations.

---

## 📊 Monitoring Load Math

| Scenario | Monthly Requests |
|----------|------------------|
| 10 endpoints × 1-min checks | ~432K requests |
| 50 endpoints × 30-sec checks | ~4.3M requests |

This is very manageable with async HTTP calls and proper rate limiting.

---

## 💻 Infrastructure Cost

**Early Stage (Month 1-6):**
- VPS (2 vCPU, 4GB RAM): ₹1,500–₹2,000
- Email service: ₹300
- Domain + misc: ₹200
- **Total: ₹2,500–₹3,000/month**

**Break-even:** ~6 Starter users

---

## 🗺️ Roadmap

### v1.1
- [ ] Slack/Webhook alerts (PRO)
- [ ] Multi-region probes
- [ ] Response time charts

### v1.2
- [ ] Team collaboration
- [ ] Custom alert rules
- [ ] Status page integration

### v2.0
- [ ] Synthetic monitoring
- [ ] API response validation
- [ ] SSL certificate monitoring

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is proprietary software by Velzox Tech.

---

## 📞 Support

- 📧 Email: support@velzox.com
- 📖 Documentation: Coming soon
- 💬 Discord: Coming soon

---

<p align="center">
  Made with ❤️ by <strong>Velzox Tech</strong>
</p>
