package com.clement.dexwin.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@Testcontainers
@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    protected static final PostgreSQLContainer<?> postgres = TestContainerManager.getPostgres();

    @Autowired
    protected Environment environment;

    @BeforeAll
    static void logContainerDetails() {
        log.info("PostgreSQL JDBC URL: {}", postgres.getJdbcUrl());
        log.info("PostgreSQL Username: {}", postgres.getUsername());
        log.info("PostgreSQL Database: {}", postgres.getDatabaseName());
    }

    @Test
    void verifySetup() {
        assertThat(postgres.isCreated()).isTrue();
        assertThat(postgres.isRunning()).isTrue();

        log.info("Spring application name: {}", environment.getProperty("spring.application.name"));
        log.info("Datasource URL: {}", environment.getProperty("spring.datasource.url"));
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database properties
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // JPA/Hibernate properties for tests
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");

        // Hikari connection pool properties
        registry.add("spring.datasource.hikari.minimum-idle", () -> "2");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "10");
        registry.add("spring.datasource.hikari.max-lifetime", () -> "30000");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "20000");
        registry.add("spring.datasource.hikari.idle-timeout", () -> "10000");

        // Application properties
        registry.add("spring.application.name", () -> "dexwin-app");
        registry.add("spring.lifecycle.timeout-per-shutdown-phase", () -> "15s");

        // RSA keys for JWT (using classpath resources)
        registry.add("rsa.public-key", () -> "classpath:certs/public.pem");
        registry.add("rsa.private-key", () -> "classpath:certs/private.pem");
    }
}