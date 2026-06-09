# Deployment Guide

## DigitalOcean App Platform (Recommended)

### Prerequisites
- DigitalOcean account
- GitHub repository connected
- `doctl` CLI installed (optional)

### Steps

1. **Build the application**
   ```bash
   mvn clean package
   ```

2. **Create a Docker image**
   ```bash
   docker build -t job-processing-scheduler:latest .
   ```

3. **Push to DigitalOcean Registry**
   ```bash
   chmod +x deploy.sh
   ./deploy.sh
   ```

4. **Deploy via DigitalOcean Dashboard**
   - Go to [DigitalOcean Apps](https://cloud.digitalocean.com/apps)
   - Click "Create Apps"
   - Connect your GitHub repository
   - Use the `.do/app.yaml` configuration
   - Click "Deploy"

---

## DigitalOcean Kubernetes (DOKS)

### Prerequisites
- DigitalOcean Kubernetes cluster created
- `kubectl` configured to access your cluster
- Docker image pushed to DigitalOcean Registry

### Deploy

```bash
kubectl apply -f k8s-deployment.yaml
```

Verify deployment:

```bash
kubectl get pods
kubectl get services
```

Access the API:

```bash
kubectl port-forward service/job-scheduler-service 8080:80
curl http://localhost:8080/api/jobs
```

---

## Docker Compose (Development)

```bash
docker-compose up --build
```

Access at `http://localhost:8080/api/jobs`

---

## Environment Variables

- `JAVA_OPTS`: JVM options (default: `-Xmx512m -Xms256m`)
- `PORT`: Server port (default: `8080`)

---

## Health Checks

The application exposes:
- `GET /api/jobs` — returns 405 (endpoint requires POST)
- `GET /api/jobs/{jobId}` — returns job status
- `POST /api/jobs` — submits a job

---

## Monitoring

Check logs in DigitalOcean Dashboard or via:

```bash
kubectl logs deployment/job-processing-scheduler
```

---

## Scaling

To scale horizontally in Kubernetes:

```bash
kubectl scale deployment job-processing-scheduler --replicas=3
```
