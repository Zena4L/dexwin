package com.clement.dexwin.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import static org.assertj.core.api.Assertions.assertThat;

//@Testcontainers
class PostgresContainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = TestContainerManager.getPostgres();

    @Test
    void connectionEstablished() {
        assertThat(postgres.isCreated()).isTrue();
        assertThat(postgres.isRunning()).isTrue();
    }
}