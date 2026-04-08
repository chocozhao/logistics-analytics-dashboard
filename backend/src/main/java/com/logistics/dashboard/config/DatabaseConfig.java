package com.logistics.dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        // Try multiple approaches to get database URL
        String jdbcUrl = getJdbcUrl();
        String username = getUsername();
        String password = getPassword();

        // Log the configuration (using System.out since logging might not be initialized yet)
        System.out.println("=== DatabaseConfig ===");
        System.out.println("JDBC URL: " + jdbcUrl.replace(password, "***")); // Hide password in logs
        System.out.println("Username: " + username);
        System.out.println("Password: ***");
        System.out.println("======================");

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
            System.out.println("Using SPRING_DATASOURCE_URL for JDBC URL");
            return springDatasourceUrl;
        }

        // Approach 2: Use DATABASE_URL (Render's connectionString) and convert to JDBC format
        String databaseUrl = env.getProperty("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            System.out.println("Using DATABASE_URL for JDBC URL: " + databaseUrl.replaceAll(":([^@]+)@", ":***@")); // Hide password
            return convertToJdbcUrl(databaseUrl);
        }

        // Approach 3: Build from individual components (host, port, database)
        String host = env.getProperty("SPRING_DATASOURCE_HOST");
        String port = env.getProperty("SPRING_DATASOURCE_PORT");
        String database = env.getProperty("SPRING_DATASOURCE_DATABASE");

        if (host != null && port != null && database != null) {
            System.out.println("Building JDBC URL from individual components");
            return String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
        }

        // Approach 4: Fallback to property from application-prod.properties
        System.out.println("Using fallback JDBC URL from application properties");
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
        // Convert Render's connection string to JDBC URL
        // Render format: postgresql://username:password@host:port/database
        // or: postgresql://username:password@host/database (no port, default 5432)
        // We need: jdbc:postgresql://host:port/database (without username:password in URL)

        if (databaseUrl == null || databaseUrl.isEmpty()) {
            return databaseUrl;
        }

        // If already JDBC format, return as-is
        if (databaseUrl.startsWith("jdbc:")) {
            return databaseUrl;
        }

        try {
            // Parse the PostgreSQL URL
            // Replace postgresql:// with http:// for URI parsing
            String httpUrl = databaseUrl.replace("postgresql://", "http://");
            URI uri = new URI(httpUrl);

            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();

            // Remove leading slash from path if present
            if (path != null && path.startsWith("/")) {
                path = path.substring(1);
            }

            // Build JDBC URL without username/password
            String result;
            if (port > 0) {
                result = String.format("jdbc:postgresql://%s:%d/%s", host, port, path != null ? path : "");
            } else {
                // Default PostgreSQL port is 5432
                result = String.format("jdbc:postgresql://%s:5432/%s", host, path != null ? path : "");
            }
            System.out.println("Converted URL via URI parsing: " + result);
            return result;
        } catch (URISyntaxException e) {
            // If parsing fails, try simple string manipulation
            System.out.println("WARNING: URI parsing failed for database URL, using string manipulation: " + e.getMessage());

            if (databaseUrl.startsWith("postgresql://")) {
                // Remove the postgresql:// prefix
                String withoutPrefix = databaseUrl.substring("postgresql://".length());

                // Find @ symbol separating credentials from host
                int atIndex = withoutPrefix.indexOf('@');
                if (atIndex > 0) {
                    // Extract host/database part after @
                    String hostAndDb = withoutPrefix.substring(atIndex + 1);

                    // Check if hostAndDb contains port
                    if (hostAndDb.contains(":")) {
                        // Already has port
                        String result = "jdbc:postgresql://" + hostAndDb;
                        System.out.println("Converted URL via string manipulation (has port): " + result);
                        return result;
                    } else {
                        // Add default port
                        // Find / separating host from database
                        int slashIndex = hostAndDb.indexOf('/');
                        if (slashIndex > 0) {
                            String host = hostAndDb.substring(0, slashIndex);
                            String db = hostAndDb.substring(slashIndex + 1);
                            String result = String.format("jdbc:postgresql://%s:5432/%s", host, db);
                            System.out.println("Converted URL via string manipulation (added port): " + result);
                            return result;
                        } else {
                            String result = "jdbc:postgresql://" + hostAndDb + ":5432/";
                            System.out.println("Converted URL via string manipulation (no db): " + result);
                            return result;
                        }
                    }
                } else {
                    // No @ found, assume it's already host/database format
                    return "jdbc:postgresql://" + withoutPrefix;
                }
            }

            // Default fallback
            return "jdbc:postgresql://" + databaseUrl;
        }
    }
}