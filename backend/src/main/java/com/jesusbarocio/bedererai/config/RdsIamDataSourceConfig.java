package com.jesusbarocio.bedererai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsUtilities;

import javax.sql.DataSource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Only active when app.datasource.iam-auth=true (set on the ECS task
 * definition). Local dev and docker-compose keep using a plain Postgres
 * container with a static password - see the "prod" profile block in
 * application.yml, which Spring Boot's normal DataSourceAutoConfiguration
 * still handles when this property is unset/false.
 *
 * RDS IAM auth tokens are valid for ~15 minutes. Rather than generating a
 * fresh token on every connection checkout (unnecessary overhead, and it
 * doesn't actually integrate cleanly with how HikariCP manages a pool of
 * already-open physical connections), this takes the approach AWS's own
 * documentation recommends: refresh the pool's password via
 * HikariConfigMXBean on a fixed schedule, and set maxLifetime below the
 * token's TTL so every pooled connection gets closed and reopened - with
 * whatever the current password is - well before its token could expire.
 */
@Configuration
@ConditionalOnProperty(name = "app.datasource.iam-auth", havingValue = "true")
public class RdsIamDataSourceConfig implements DisposableBean {

    // Tokens last 15 minutes; refresh with a safety margin.
    private static final int TOKEN_REFRESH_MINUTES = 10;
    // Must be shorter than the refresh interval above, so a connection can
    // never outlive the token that was valid when it was opened.
    private static final int MAX_CONNECTION_LIFETIME_MINUTES = 9;

    @Value("${app.datasource.host}")
    private String dbHost;

    @Value("${app.datasource.port:5432}")
    private int dbPort;

    @Value("${app.datasource.name}")
    private String dbName;

    @Value("${app.datasource.username}")
    private String dbUsername;

    @Value("${app.datasource.region:us-east-2}")
    private String region;

    private ScheduledExecutorService tokenRefreshScheduler;

    @Bean
    public DataSource dataSource() {
        RdsUtilities rdsUtilities = RdsUtilities.builder()
                .region(Region.of(region))
                .build();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format(
                "jdbc:postgresql://%s:%d/%s?ssl=true&sslmode=require", dbHost, dbPort, dbName));
        config.setUsername(dbUsername);
        config.setPassword(generateAuthToken(rdsUtilities));
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(30_000);
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(MAX_CONNECTION_LIFETIME_MINUTES));

        HikariDataSource dataSource = new HikariDataSource(config);

        tokenRefreshScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "rds-iam-token-refresh");
            thread.setDaemon(true);
            return thread;
        });
        tokenRefreshScheduler.scheduleAtFixedRate(
                () -> dataSource.getHikariConfigMXBean().setPassword(generateAuthToken(rdsUtilities)),
                TOKEN_REFRESH_MINUTES, TOKEN_REFRESH_MINUTES, TimeUnit.MINUTES);

        return dataSource;
    }

    private String generateAuthToken(RdsUtilities rdsUtilities) {
        return rdsUtilities.generateAuthenticationToken(builder -> builder
                .hostname(dbHost)
                .port(dbPort)
                .username(dbUsername)
                .build());
    }

    @Override
    public void destroy() {
        if (tokenRefreshScheduler != null) {
            tokenRefreshScheduler.shutdownNow();
        }
    }
}
