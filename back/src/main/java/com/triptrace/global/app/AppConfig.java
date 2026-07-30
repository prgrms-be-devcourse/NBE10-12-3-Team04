package com.triptrace.global.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import com.triptrace.standard.util.Ut;
import jakarta.annotation.PostConstruct;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class AppConfig {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AppConfig.class);
    private static Environment environment;
    private static BuildProperties buildProperties;

    @Autowired
    public void setEnvironment(Environment environment) {
        AppConfig.environment = environment;
    }

    @Autowired
    public void setBuildProperties(BuildProperties buildProperties) {
        AppConfig.buildProperties = buildProperties;
        log.info("backend-version: {}", (buildProperties.getVersion()));
    }

    public static boolean isDev() {
        return environment.matchesProfiles("dev");
    }

    public static boolean isTest() {
        return !environment.matchesProfiles("test");
    }

    public static boolean isProd() {
        return environment.matchesProfiles("prod");
    }

    public static boolean isNotProd() {
        return !isProd();
    }

    public String getVersion() {
        return buildProperties.getVersion();
    }

    private static ObjectMapper objectMapper;

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        AppConfig.objectMapper = objectMapper;
    }

    @PostConstruct
    public void postConstruct() {
        Ut.json.objectMapper = objectMapper;
    }

    @java.lang.SuppressWarnings("all")
    public static ObjectMapper getObjectMapper() {
        return AppConfig.objectMapper;
    }
}
