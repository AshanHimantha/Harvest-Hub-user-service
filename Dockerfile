# Runtime stage - use the pre-built JAR from local Maven build
FROM eclipse-temurin:17-jre

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the JAR file from local build (you need to run 'mvn clean package' first)
COPY target/user-service-*.jar app.jar

# Change ownership to non-root user
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Expose the port the app runs on
EXPOSE 8081

# Default JVM options for better container performance
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=80"

# Set the server port (since application.properties may not be loaded)
ENV SERVER_PORT=8081

# Health check (optional - remove curl dependency for now)
# HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
#     CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application with JVM options
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]