package com.urlshortener.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void stripsEmbeddedCredentialsAndChannelBinding() {
        var normalized = DatabaseUrlEnvironmentPostProcessor.normalize(
                "jdbc:postgresql://neondb_owner:secret@ep-example-pooler.aws.neon.tech/neondb"
                        + "?sslmode=require&channel_binding=require");

        assertThat(normalized).isNotNull();
        assertThat(normalized.jdbcUrl())
                .isEqualTo("jdbc:postgresql://ep-example-pooler.aws.neon.tech/neondb?sslmode=require");
        assertThat(normalized.username()).isEqualTo("neondb_owner");
        assertThat(normalized.password()).isEqualTo("secret");
    }

    @Test
    void convertsLibpqStyleUrl() {
        var normalized = DatabaseUrlEnvironmentPostProcessor.normalize(
                "postgresql://neondb_owner:secret@ep-example-pooler.aws.neon.tech/neondb?sslmode=require");

        assertThat(normalized).isNotNull();
        assertThat(normalized.jdbcUrl())
                .isEqualTo("jdbc:postgresql://ep-example-pooler.aws.neon.tech/neondb?sslmode=require");
        assertThat(normalized.username()).isEqualTo("neondb_owner");
        assertThat(normalized.password()).isEqualTo("secret");
    }

    @Test
    void leavesCleanJdbcUrlAlone() {
        var normalized = DatabaseUrlEnvironmentPostProcessor.normalize(
                "jdbc:postgresql://localhost:5434/urlshortener");

        assertThat(normalized).isNotNull();
        assertThat(normalized.jdbcUrl())
                .isEqualTo("jdbc:postgresql://localhost:5434/urlshortener");
        assertThat(normalized.username()).isNull();
        assertThat(normalized.password()).isNull();
    }
}
