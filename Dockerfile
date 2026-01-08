# syntax=docker/dockerfile:1.6

# =========================
# Build stage
# =========================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1) Önce sadece pom.xml kopyala (cache için önemli)
COPY pom.xml .

# 2) Dependency'leri önceden indir (go-offline) + Maven cache kullan
#    (BuildKit ile --mount çalışır, yoksa bu satırı normal RUN gibi çalıştırır)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -e -DskipTests \
    -Dmaven.repo.local=/root/.m2/repository \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=60 \
    -Dmaven.wagon.http.retryHandler.count=5 \
    dependency:go-offline

# 3) Kaynakları kopyala
COPY src ./src

# 4) Paketle (cache yine devrede)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests \
    -Dmaven.repo.local=/root/.m2/repository \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=60 \
    -Dmaven.wagon.http.retryHandler.count=5 \
    package

# =========================
# Run stage
# =========================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Jar kopyala
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
