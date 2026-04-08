package com.logistics.dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DatabaseConfig {

    private final Environment env;

    public DatabaseConfig(Environment env) {
        this.env = env;
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        // Try multiple approaches to get database URL
        String jdbcUrl = getJdbcUrl();
        String username = getUsername();
        String password = getPassword();

        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("org.postgresql.Driver");

        // Set connection pool properties
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(5);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);

        return dataSource;
    }

    private String getJdbcUrl() {
        // Approach 1: Use SPRING_DATASOURCE_URL if set (should be full JDBC URL)
        String springDatasourceUrl = env.getProperty("SPRING_DATASOURCE_URL");
        if (springDatasourceUrl != null && !springDatasourceUrl.isEmpty()) {
            return springDatasourceUrl;
        }

        // Approach 2: Use DATABASE_URL (Render's connectionString) and convert to JDBC format
        String databaseUrl = env.getProperty("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            return convertToJdbcUrl(databaseUrl);
        }

        // Approach 3: Build from individual components (host, port, database)
        String host = env.getProperty("SPRING_DATASOURCE_HOST");
        String port = env.getProperty("SPRING_DATASOURCE_PORT");
        String database = env.getProperty("SPRING_DATASOURCE_DATABASE");

        if (host != null && port != null && database != null) {
            return String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
        }

        // Approach 4: Fallback to property from application-prod.properties
        return env.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/logistics");
    }

    private String getUsername() {
        // Try multiple sources for username
        String username = env.getProperty("SPRING_DATASOURCE_USERNAME");
        if (username != null && !username.isEmpty()) {
            return username;
        }

        // Try to extract from DATABASE_URL
        String databaseUrl = env.getProperty("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            try {
                URI uri = new URI(databaseUrl.replace("postgresql://", "http://"));
                String userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    return userInfo.split(":")[0];
                }
            } catch (URISyntaxException e) {
                // Ignore, fall through
            }
        }

        return env.getProperty("spring.datasource.username", "reader");
    }

    private String getPassword() {
        // Try multiple sources for password
        String password = env.getProperty("SPRING_DATASOURCE_PASSWORD");
        if (password != null && !password.isEmpty()) {
            return password;
        }

        // Try to extract from DATABASE_URL
        String databaseUrl = env.getProperty("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            try {
                URI uri = new URI(databaseUrl.replace("postgresql://", "http://"));
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    return userInfo.split(":")[1];
                }
            } catch (URISyntaxException e) {
                // Ignore, fall through
            }
        }

        return env.getProperty("spring.datasource.password", "readonly");
    }

    private String convertToJdbcUrl(String databaseUrl) {
        // Convert postgresql://host:port/db to jdbc:postgresql://host:port/db
        // Also handle postgresql://user:pass@host:port/db format

        if (databaseUrl.startsWith("jdbc:")) {
            return databaseUrl; // Already JDBC format
        }

        if (databaseUrl.startsWith("postgresql://")) {
            return "jdbc:" + databaseUrl;
        }

        // If it doesn't start with postgresql://, assume it's already a JDBC URL or needs jdbc: prefix
        if (!databaseUrl.startsWith("jdbc:postgresql://") && databaseUrl.contains("postgresql://")) {
            return "jdbc:" + databaseUrl;
        }

        // Default: prepend jdbc:postgresql:// if not present
        if (!databaseUrl.startsWith("jdbc:")) {
            return "jdbc:postgresql://" + databaseUrl;
        }

        return databaseUrl;
    }
}