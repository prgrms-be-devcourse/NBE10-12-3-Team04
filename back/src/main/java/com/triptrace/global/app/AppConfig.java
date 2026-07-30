package com.triptrace.global.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.triptrace.standard.util.Ut;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;


@Slf4j
@Configuration
public class AppConfig {
    private static Environment environment;
    private static BuildProperties buildProperties;

    @Autowired
    public void setEnvironment(Environment environment) {
        AppConfig.environment = environment;
    }
    @Autowired
    public void setBuildProperties(BuildProperties buildProperties) {
        AppConfig.buildProperties = buildProperties;
        log.info("backend-version: {}",(buildProperties.getVersion()));
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

    public String getVersion(){
        return buildProperties.getVersion();
    }

    @Getter
    private static ObjectMapper objectMapper;

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        AppConfig.objectMapper = objectMapper;
    }

    @PostConstruct
    public void postConstruct() {
        Ut.json.objectMapper = objectMapper;
    }
}
