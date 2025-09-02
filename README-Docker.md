# Docker Setup for Harvest Hub User Service

This document provides instructions for running the Harvest Hub User Service using Docker.

## Prerequisites

- Docker installed on your system
- Docker Compose (optional, for development with database)

## Building the Docker Image

### Option 1: Build from Source

1. First, build the application using Maven:
   ```bash
   mvn clean package -DskipTests
   ```

2. Build the Docker image:
   ```bash
   docker build -t harvest-hub-user-service:latest .
   ```

### Option 2: Using Docker Compose (Recommended for Development)

The `docker-compose.yml` file includes both the user service and a PostgreSQL database for easy development setup.

1. **Configure Environment Variables:**
   
   Before running, update the environment variables in `docker-compose.yml` with your AWS Cognito configuration:
   
   ```yaml
   environment:
     # Update these with your actual values
     - SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=https://cognito-idp.your-region.amazonaws.com/your-user-pool-id/.well-known/jwks.json
     - AWS_COGNITO_USERPOOLID=your-user-pool-id
     - AWS_REGION=your-region
   ```

2. **Start the services:**
   ```bash
   docker-compose up --build
   ```

   This will:
   - Build the user service image
   - Start a PostgreSQL database container
   - Start the user service container
   - Create a network for communication between services

## Running the Docker Container Standalone

If you want to run just the user service container (assuming you have a separate database):

```bash
docker run -d \
  --name harvest-hub-user-service \
  -p 8081:8081 \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://your-db-host:5432/user_db" \
  -e SPRING_DATASOURCE_USERNAME="your-username" \
  -e SPRING_DATASOURCE_PASSWORD="your-password" \
  -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI="https://cognito-idp.your-region.amazonaws.com/your-user-pool-id/.well-known/jwks.json" \
  -e AWS_COGNITO_USERPOOLID="your-user-pool-id" \
  -e AWS_REGION="your-region" \
  harvest-hub-user-service:latest
```

## Environment Variables

The following environment variables can be configured:

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL database URL | Required |
| `SPRING_DATASOURCE_USERNAME` | Database username | Required |
| `SPRING_DATASOURCE_PASSWORD` | Database password | Required |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` | AWS Cognito JWK Set URI | Required |
| `AWS_COGNITO_USERPOOLID` | AWS Cognito User Pool ID | Required |
| `AWS_REGION` | AWS Region | Required |
| `JAVA_OPTS` | JVM options | `-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=80` |

## Accessing the Application

Once the container is running, the application will be available at:
- **API Base URL:** `http://localhost:8081`
- **Health Check:** `http://localhost:8081/actuator/health` (if Spring Boot Actuator is enabled)

## Logs

To view the application logs:

```bash
# For docker-compose
docker-compose logs -f user-service

# For standalone container
docker logs -f harvest-hub-user-service
```

## Development Tips

1. **Hot Reload:** For development, you may want to mount your source code as a volume and rebuild when changes are made.

2. **Database Persistence:** The docker-compose setup includes a named volume `postgres_data` to persist database data between container restarts.

3. **Network:** All services in the docker-compose setup are connected via the `harvest-hub-network` bridge network.

## Stopping the Services

```bash
# Stop docker-compose services
docker-compose down

# Stop standalone container
docker stop harvest-hub-user-service
docker rm harvest-hub-user-service
```

## Troubleshooting

1. **Port Conflicts:** If port 8081 is already in use, change the port mapping in docker-compose.yml or the docker run command.

2. **Database Connection Issues:** Ensure the database is running and accessible, and that the connection string is correct.

3. **AWS Cognito Issues:** Verify that your AWS credentials are configured correctly and that the Cognito User Pool settings are accurate.

## Security Notes

- The container runs as a non-root user (`appuser`) for security.
- Sensitive configuration like database passwords and AWS credentials should be managed through environment variables or secrets management systems.
- For production deployments, consider using Docker secrets or external configuration management.