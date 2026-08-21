# Best practices demonstrated:
# - Multi-stage build: builder stage compiles; runtime stage has no JDK, only JRE
# - Use a specific base image tag (never "latest") for reproducible builds
# - Layered JAR extraction: Spring Boot Maven plugin splits the app into layers
#   (dependencies, snapshot-dependencies, spring-boot-loader, application).
#   Layers that change rarely (deps) are cached separately from app code — faster rebuilds.
# - Run as a non-root user to reduce the blast radius of a container escape
# - EXPOSE documents the port but does not publish it; use -p at runtime

# ---- Stage 1: Build ----
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Download dependencies first — this layer is cached unless pom.xml changes
RUN ./mvnw dependency:go-offline -q

COPY src/ src/
RUN ./mvnw package -DskipTests -q

# Extract layered JAR
RUN java -Djarmode=tools -jar target/*.jar extract --layers --destination extracted

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# Non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy each layer separately so Docker caches them independently
COPY --from=builder /app/extracted/dependencies/           ./
COPY --from=builder /app/extracted/spring-boot-loader/    ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/           ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
