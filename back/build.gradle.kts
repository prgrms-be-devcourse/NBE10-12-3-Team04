plugins {
    java

    kotlin("jvm") version "2.3.20" //java kotlin 함께 컴파일
    kotlin("plugin.spring") version "2.3.20" // final 제약을 spring 프록시와 호환
    kotlin("plugin.jpa") version "2.3.20" // jpa entity용 no-arg생성자와 open 처리

    id("com.diffplug.spotless") version "8.8.0"
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com"
version = providers.gradleProperty("appVersion").orElse("0.0.3-SNAPSHOT").get()
description = "triptrace"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val jjwtVersion = "0.13.0"
val extractorVersion = "2.19.0"

dependencies {
    // kotlin
    implementation(kotlin("reflect")) // spring kotlin 지원에 필요
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin") // kotlin data class JSON 역직렬화
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // H2 Console
    implementation("org.springframework.boot:spring-boot-h2console")

    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")

    // Actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Micrometer가 등록한 JVM·HTTP·DB 등의 메트릭을 /actuator/prometheus 형식으로 제공한다.
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Database
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")


    //Image MetaData
    implementation("com.drewnoakes:metadata-extractor:$extractorVersion")

    // Dev
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // JJWT
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // kotlin Test
    testImplementation(kotlin("test"))
    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

springBoot {
    buildInfo()
}

spotless {
    java {
        target("src/*/java/**/*.java")
        googleJavaFormat()
    }
    kotlin {
        target("src/*/kotlin/**/*.kt")
        ktlint()
    }
}
