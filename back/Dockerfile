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

ARG LOG_DIR=$LOG_DIR
ARG IMAGE_UPLOAD=$IMAGE_UPLOAD

RUN addgroup --system spring && adduser --system --ingroup spring spring
RUN mkdir -p ${LOG_DIR} ${IMAGE_UPLOAD} && chown -R spring:spring ${LOG_DIR} ${IMAGE_UPLOAD}

COPY --from=builder  --chown=spring:spring /app/build/libs/*.jar app.jar

USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
