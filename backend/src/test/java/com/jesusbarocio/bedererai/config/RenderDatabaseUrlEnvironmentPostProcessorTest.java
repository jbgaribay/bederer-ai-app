package com.jesusbarocio.bedererai.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class RenderDatabaseUrlEnvironmentPostProcessorTest {

    private final RenderDatabaseUrlEnvironmentPostProcessor processor =
            new RenderDatabaseUrlEnvironmentPostProcessor();

    @Test
    void rewritesRenderConnectionStringWithoutExplicitPort() {
        // This is exactly the shape of Render's fromDatabase connectionString -
        // no ":5432" in it, which is what broke production: URI.getPort()
        // returns -1 for a URL with no explicit port, and that -1 was being
        // written straight into the JDBC URL instead of defaulting to 5432.
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DATABASE_URL", "postgres://bedererai:secret@dpg-example-a/bedererai");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://dpg-example-a:5432/bedererai");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("bedererai");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("secret");
    }

    @Test
    void respectsExplicitPortWhenPresent() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DATABASE_URL", "postgres://user:pw@example.com:6543/mydb");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://example.com:6543/mydb");
    }

    @Test
    void leavesAlreadyJdbcUrlsUntouched() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DATABASE_URL", "jdbc:postgresql://localhost:5432/bedererai");

        processor.postProcessEnvironment(environment, new SpringApplication());

        // Should not have added an override property source at all - the
        // property resolves straight from what was already set.
        assertThat(environment.getProperty("spring.datasource.url")).isNull();
    }

    @Test
    void doesNothingWhenDatabaseUrlIsAbsent() {
        MockEnvironment environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url")).isNull();
    }
}
