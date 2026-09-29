# Production Deployment Guide

---

## 1. Local Deployment with Docker Compose

The fastest way to run the entire distributed cluster (Postgres, Redis, Kafka, Backend, Frontend, Prometheus, Grafana) locally is via Docker Compose:

```bash
# 1. Navigate to infrastructure folder
cd infrastructure

# 2. Start all services
docker compose up -d

# 3. Scale workers and schedulers horizontally
docker compose up -d --scale backend=3

# 4. View container statuses
docker compose ps
```

### Accessing Local Endpoints
* **Web Dashboard**: [http://localhost:3000](http://localhost:3000)
* **Backend API / Health**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
* **Swagger UI Documentation**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **Prometheus Metrics**: [http://localhost:9090](http://localhost:9090)
* **Grafana Dashboard**: [http://localhost:3001](http://localhost:3001) (Credentials: `admin` / `admin`)

---

## 2. Low-Cost / Free-Tier Cloud Deployment Guide

| Component | Recommended Cloud Provider | Tier | Setup Notes |
| :--- | :--- | :--- | :--- |
| **Frontend** | **Vercel** / **Netlify** | Free | Deploy `frontend/` directory with Build Command: `npm run build`, Output: `dist`. Set `VITE_API_URL` to backend domain. |
| **Backend API & Workers** | **Render** / **Railway** / **Fly.io** | Low-Cost / Free | Deploy via `backend/Dockerfile`. Expose port `8080`. |
| **PostgreSQL** | **Neon** / **Supabase** | Free | Serverless PostgreSQL 16. Supports connection pooling. |
| **Redis** | **Upstash Redis** | Free | Serverless Redis with standard TCP support for Redisson locks. |
| **Kafka** | **Upstash Kafka** / **Aiven** | Free | Managed Kafka cluster with SASL_SSL authentication. |

### Environment Variables for Production
```env
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://<neon-host>:5432/<db>?sslmode=require
DATABASE_USERNAME=<user>
DATABASE_PASSWORD=<password>

REDIS_HOST=<upstash-redis-host>
REDIS_PORT=6379
REDIS_PASSWORD=<upstash-password>

KAFKA_BOOTSTRAP_SERVERS=<upstash-kafka-endpoint>:9092

JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
CORS_ALLOWED_ORIGINS=https://your-dts-frontend.vercel.app
```
