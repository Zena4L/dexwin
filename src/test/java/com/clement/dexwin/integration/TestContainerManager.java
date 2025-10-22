package com.clement.dexwin.integration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public final class TestContainerManager {
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName
            .parse("postgres:15-alpine");

    private static PostgreSQLContainer<?> postgres;

    public static synchronized PostgreSQLContainer<?> getPostgres() {
        if (postgres == null) {
            postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass")
                .withCommand("postgres -c max_connections=100")
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofSeconds(60));
            postgres.start();
        }
        return postgres;
    }


    public static synchronized void stopContainers() {
        if (postgres != null) {
            postgres.stop();
            postgres = null;
        }

    }
}