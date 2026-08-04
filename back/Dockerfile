#공식 오픈 jdk
FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon
COPY src ./src
# CD가 전달한 앱 버전을 JAR의 build-info.properties에 기록한다.
# TODO: 캐싱 추후 고려
ARG APP_VERSION=0.0.1-SNAPSHOT
RUN ./gradlew -PappVersion=${APP_VERSION} bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre-noble AS runner

WORKDIR /app

# JVM·로그·JPA가 컨테이너 기본값에 의존하지 않도록 서비스 시간대를 명시한다.
ENV TZ=Asia/Seoul
ENV JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Seoul

RUN addgroup --system spring && adduser --system --ingroup spring spring
# 컨테이너 내부의 쓰기 루트는 환경과 무관하게 고정한다.
# 업로드 하위 경로는 애플리케이션 설정이 이 루트 아래에 생성한다.
RUN mkdir -p /app/logs /app/images && chown -R spring:spring /app/logs /app/images

COPY --from=builder  --chown=spring:spring /app/build/libs/*.jar app.jar

USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
