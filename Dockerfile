# 1) 构建前端
FROM node:20-alpine AS web
WORKDIR /app
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
RUN npx vite build --outDir dist --emptyOutDir

# 2) 构建后端，前端产物直接放进 static
FROM maven:3.9-eclipse-temurin-17 AS api
WORKDIR /app
COPY pom.xml ./
COPY src ./src
COPY --from=web /app/dist ./src/main/resources/static
RUN mvn -B -q -DskipTests package

# 3) 运行
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=api /app/target/youkong-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
