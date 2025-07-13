# ── STAGE 1: Build with Maven + JDK 21 ─────────────────────────────
FROM maven:3.9.8-eclipse-temurin-21 AS builder

WORKDIR /workspace
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ── STAGE 2: Runtime with a slim JRE ────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# copy artifact
COPY --from=builder /workspace/target/expenzo-backend-0.0.1-SNAPSHOT.jar app.jar

# expose the custom port
EXPOSE 6969

# run, passing server.port override
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=6969"]
