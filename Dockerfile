# ─────────────────────────────────────────────────────────────
# MercadoX — Dockerfile (multi-stage build)
#
# Etapa 1 (builder): compila el proyecto y genera el JAR.
# Etapa 2 (runtime): imagen mínima que solo ejecuta el JAR.
# Resultado: imagen final ~200 MB en lugar de ~600 MB.
# ─────────────────────────────────────────────────────────────

# ── Etapa 1: compilación ──────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos de dependencias primero (aprovecha el cache de Docker)
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

# Descargar dependencias sin compilar el código fuente
# (esta capa se cachea si pom.xml no cambia)
RUN ./mvnw dependency:go-offline -B --no-transfer-progress

# Copiar el código fuente y compilar
COPY src/ src/
RUN ./mvnw package -B --no-transfer-progress -DskipTests

# Etapa 2: imagen de ejecución (con layered JAR)
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S mercadox && adduser -S mercadox -G mercadox

COPY --from=builder /app/target/*.jar app.jar

RUN java -Djarmode=layertools -jar app.jar extract --destination extracted

RUN chown -R mercadox:mercadox /app
USER mercadox

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-cp", "extracted/dependencies/*:extracted/spring-boot-loader/*:extracted/snapshot-dependencies/*:extracted/application/*", \
  "org.springframework.boot.loader.launch.JarLauncher"]
