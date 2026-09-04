# ── 빌드 스테이지 ──
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew
# 소스 복사 전에 의존성만 먼저 받아둬서, 소스만 바뀔 때 재빌드 속도를 높인다.
RUN ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar --no-daemon

# ── 실행 스테이지 ──
FROM eclipse-temurin:17-jre
WORKDIR /app
# 컨테이너 기본 타임존이 UTC라 LocalDateTime.now() 등이 9시간씩 밀려서 나오는 걸 방지.
# 이미지 안에 박아두면 docker-compose.yml 없이 이미지만 pull해서 돌려도(Docker Hub 배포) 항상 적용됨.
ENV TZ=Asia/Seoul
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
