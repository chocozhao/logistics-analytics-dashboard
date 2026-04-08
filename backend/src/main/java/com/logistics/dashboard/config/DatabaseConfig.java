package com.logistics.dashboard.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
    @ConditionalOnMissingBean(DataSource.class)
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        // Log the configuration (using System.out since logging might not be initialized yet)
        System.out.println("=== DatabaseConfig - Creating DataSource ===");

        // Let Spring Boot auto-configure the basic properties
        // We just need to set Hikari-specific properties
        dataSource.setDriverClassName("org.postgresql.Driver");

        // Set connection pool properties (can also be set in application-prod.properties)
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(5);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);

        // Log final configuration
        System.out.println("DataSource configured with:");
        System.out.println("Driver: " + dataSource.getDriverClassName());
        System.out.println("Max Pool Size: " + dataSource.getMaximumPoolSize());
        System.out.println("Min Idle: " + dataSource.getMinimumIdle());
        System.out.println("Connection Timeout: " + dataSource.getConnectionTimeout());
        System.out.println("======================");

        return dataSource;
    }

    private String getJdbcUrl() {
        // Debug: print all relevant environment variables
        System.out.println("===== DATABASE CONFIG DEBUG START =====");
        System.out.println("SPRING_PROFILES_ACTIVE: " + env.getProperty("SPRING_PROFILES_ACTIVE"));
        System.out.println("Active profiles (spring.profiles.active): " + env.getProperty("spring.profiles.active"));

        // Check all possible database URL sources
        System.out.println("DATABASE_URL: " + (env.getProperty("DATABASE_URL") != null ? "SET" : "NOT SET"));
        if (env.getProperty("DATABASE_URL") != null) {
            System.out.println("DATABASE_URL value: " + env.getProperty("DATABASE_URL").replaceAll(":([^@]+)@", ":***@"));
        }

        System.out.println("SPRING_DATASOURCE_URL: " + (env.getProperty("SPRING_DATASOURCE_URL") != null ? "SET" : "NOT SET"));
        if (env.getProperty("SPRING_DATASOURCE_URL") != null) {
            System.out.println("SPRING_DATASOURCE_URL value: " + env.getProperty("SPRING_DATASOURCE_URL").replaceAll(":([^@]+)@", ":***@"));
        }

        System.out.println("spring.datasource.url: " + (env.getProperty("spring.datasource.url") != null ? "SET" : "NOT SET"));
        if (env.getProperty("spring.datasource.url") != null) {
            System.out.println("spring.datasource.url value: " + env.getProperty("spring.datasource.url").replaceAll(":([^@]+)@", ":***@"));
        }

        System.out.println("SPRING_DATASOURCE_HOST: " + env.getProperty("SPRING_DATASOURCE_HOST"));
        System.out.println("SPRING_DATASOURCE_PORT: " + env.getProperty("SPRING_DATASOURCE_PORT"));
        System.out.println("SPRING_DATASOURCE_DATABASE: " + env.getProperty("SPRING_DATASOURCE_DATABASE"));
        System.out.println("===== DATABASE CONFIG DEBUG END =====");

        // Priority 1: DATABASE_URL from Render (highest priority)
        String databaseUrl = env.getProperty("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            System.out.println("===== USING DATABASE_URL ENVIRONMENT VARIABLE =====");
            return convertToJdbcUrl(databaseUrl);
        }

        // Priority 2: SPRING_DATASOURCE_URL from Render
        databaseUrl = env.getProperty("SPRING_DATASOURCE_URL");
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            System.out.println("===== USING SPRING_DATASOURCE_URL ENVIRONMENT VARIABLE =====");
            return convertToJdbcUrl(databaseUrl);
        }

        // Priority 3: spring.datasource.url from application properties
        databaseUrl = env.getProperty("spring.datasource.url");
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            System.out.println("===== USING SPRING.DATASOURCE.URL PROPERTY =====");
            return convertToJdbcUrl(databaseUrl);
        }

        // Fallback: use individual components
        String host = env.getProperty("SPRING_DATASOURCE_HOST");
        String port = env.getProperty("SPRING_DATASOURCE_PORT", "5432");
        String database = env.getProperty("SPRING_DATASOURCE_DATABASE");

        if (host != null && database != null) {
            System.out.println("===== BUILDING JDBC URL FROM INDIVIDUAL COMPONENTS =====");
            String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
            System.out.println("Built JDBC URL: " + jdbcUrl);
            System.out.println("===== END COMPONENTS BUILDING =====");
            return jdbcUrl;
        }

        // Final fallback: from application properties
        System.out.println("===== USING FALLBACK FROM APPLICATION PROPERTIES =====");
        String fallbackUrl = env.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/logistics");
        System.out.println("Fallback URL: " + fallbackUrl);
        System.out.println("===== END FALLBACK PROCESSING =====");
        return fallbackUrl;
    }

    private String getUsername() {
        // Try to extract from DATABASE_URL first (Render's format)
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

        // Try SPRING_DATASOURCE_USERNAME as fallback
        String username = env.getProperty("SPRING_DATASOURCE_USERNAME");
        if (username != null && !username.isEmpty()) {
            return username;
        }

        return env.getProperty("spring.datasource.username", "reader");
    }

    private String getPassword() {
        // Try to extract from DATABASE_URL first (Render's format)
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

        // Try SPRING_DATASOURCE_PASSWORD as fallback
        String password = env.getProperty("SPRING_DATASOURCE_PASSWORD");
        if (password != null && !password.isEmpty()) {
            return password;
        }

        return env.getProperty("spring.datasource.password", "readonly");
    }

    private String convertToJdbcUrl(String databaseUrl) {
        // Convert Render's connection string to JDBC URL
        // Render format: postgresql://username:password@host:port/database?sslmode=require
        // or: postgresql://username:password@host/database (no port, default 5432)
        // We need: jdbc:postgresql://host:port/database?sslmode=require

        System.out.println("===== CONVERTING URL TO JDBC FORMAT =====");
        System.out.println("Original URL: " + databaseUrl.replaceAll(":([^@]+)@", ":***@")); // Hide password

        if (databaseUrl == null || databaseUrl.isEmpty()) {
            System.out.println("URL is null or empty, returning as-is");
            System.out.println("===== END CONVERSION =====");
            return databaseUrl;
        }

        // If already JDBC format, return as-is
        if (databaseUrl.startsWith("jdbc:")) {
            System.out.println("URL is already JDBC format, returning as-is");
            System.out.println("===== END CONVERSION =====");
            return databaseUrl;
        }

        // Render PostgreSQL URL format: postgresql://username:password@host:port/database?sslmode=require
        try {
            if (databaseUrl.startsWith("postgresql://")) {
                // Remove postgresql:// prefix
                String withoutPrefix = databaseUrl.substring("postgresql://".length());

                // Find @ symbol separating credentials from host
                int atIndex = withoutPrefix.indexOf('@');
                if (atIndex <= 0) {
                    // No @ found, try to parse as direct host/database format
                    System.out.println("No @ found in URL, trying direct parsing");
                    return parseHostAndDatabase(withoutPrefix);
                }

                // Extract host/database part after @
                String hostAndDb = withoutPrefix.substring(atIndex + 1);
                System.out.println("Host and database part: " + hostAndDb);

                return parseHostAndDatabase(hostAndDb);
            }
        } catch (Exception e) {
            System.out.println("Error parsing database URL: " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback: just prepend jdbc: prefix
        System.out.println("Using fallback conversion - prepending jdbc: prefix");
        String result = "jdbc:" + databaseUrl;
        System.out.println("Converted URL via fallback: " + result);
        System.out.println("===== END CONVERSION =====");
        return result;
    }

    private String parseHostAndDatabase(String hostAndDb) {
        // hostAndDb format: host:port/database?query or host/database?query
        // Remove query parameters for parsing, then re-add them
        String queryParams = "";
        int questionMarkIndex = hostAndDb.indexOf('?');
        if (questionMarkIndex > 0) {
            queryParams = hostAndDb.substring(questionMarkIndex);
            hostAndDb = hostAndDb.substring(0, questionMarkIndex);
        }

        System.out.println("Host and DB after removing query params: " + hostAndDb);

        // Find slash separating host/port from database
        int slashIndex = hostAndDb.indexOf('/');
        if (slashIndex <= 0) {
            // No database name specified
            System.out.println("No database name found in URL");
            String hostPort = hostAndDb;
            String database = "";

            // Check if hostPort has port
            int colonIndex = hostPort.indexOf(':');
            String host;
            int port = 5432;

            if (colonIndex > 0) {
                host = hostPort.substring(0, colonIndex);
                try {
                    port = Integer.parseInt(hostPort.substring(colonIndex + 1));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port number, using default 5432");
                }
            } else {
                host = hostPort;
            }

            String result = String.format("jdbc:postgresql://%s:%d/%s%s", host, port, database, queryParams);
            System.out.println("Converted URL (no db name): " + result);
            return result;
        }

        // Extract host/port and database
        String hostPort = hostAndDb.substring(0, slashIndex);
        String database = hostAndDb.substring(slashIndex + 1);

        System.out.println("Host/port: " + hostPort + ", Database: " + database);

        // Parse host and port
        String host;
        int port = 5432; // Default PostgreSQL port

        int colonIndex = hostPort.indexOf(':');
        if (colonIndex > 0) {
            host = hostPort.substring(0, colonIndex);
            try {
                port = Integer.parseInt(hostPort.substring(colonIndex + 1));
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number, using default 5432");
            }
        } else {
            host = hostPort;
        }

        String result = String.format("jdbc:postgresql://%s:%d/%s%s", host, port, database, queryParams);
        System.out.println("Converted URL: " + result);
        return result;
    }
}