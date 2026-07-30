package com.jesusbarocio.bedererai.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Render (like Heroku, and most managed Postgres providers) exposes a
 * database's connection info as a single DATABASE_URL in the form
 * postgres://user:password@host:port/database - not the
 * jdbc:postgresql://host:port/database URL plus separate username/password
 * that Spring Boot's DataSourceAutoConfiguration expects, and not broken out
 * into separate host/port fields the way the AWS/ECS deployment's
 * DATABASE_HOST/DATABASE_PORT env vars are (Render's fromDatabase binding
 * for Postgres only exposes connectionString/user/password/database, no
 * separate host or port property - confirmed against Render's Blueprint
 * docs).
 *
 * This runs before the rest of Spring Boot starts up, and if DATABASE_URL
 * is in that postgres://... form, rewrites it into
 * spring.datasource.url/username/password so the normal, unmodified
 * DataSourceAutoConfiguration path just works. A DATABASE_URL already in
 * jdbc:postgresql://... form (docker-compose, the AWS/IAM path) is left
 * completely untouched, and this has no effect at all when
 * app.datasource.iam-auth=true, since RdsIamDataSourceConfig's DataSource
 * bean takes over before autoconfiguration would ever read these properties.
 *
 * Registered via src/main/resources/META-INF/spring.factories.
 */
public class RenderDatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null
                || !(databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            return;
        }

        URI uri = URI.create(databaseUrl);
        String userInfo = uri.getUserInfo();
        if (userInfo == null) {
            return;
        }

        String[] parts = userInfo.split(":", 2);
        String username = parts[0];
        String password = parts.length > 1 ? parts[1] : "";

        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();

        Map<String, Object> overrides = new HashMap<>();
        overrides.put("spring.datasource.url", jdbcUrl);
        overrides.put("spring.datasource.username", username);
        overrides.put("spring.datasource.password", password);

        environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrl", overrides));
    }
}
