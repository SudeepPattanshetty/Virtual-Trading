# ---- 1) build React ----
FROM node:22 AS web
WORKDIR /web
COPY frontend/ ./
# remove any Windows node_modules/dist that came from your PC, then install fresh for Linux
RUN rm -rf node_modules dist && npm install && npm run build

# ---- 2) build Spring Boot (React is bundled in as static files) ----
FROM maven:3.9-eclipse-temurin-21 AS api
WORKDIR /app
COPY backend/pom.xml .
RUN mvn -q dependency:go-offline
COPY backend/src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -q package -DskipTests

# ---- 3) small runtime image ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=api /app/target/vtrade-backend-0.1.0.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx300m"
EXPOSE 8080
CMD ["java","-jar","app.jar"]
