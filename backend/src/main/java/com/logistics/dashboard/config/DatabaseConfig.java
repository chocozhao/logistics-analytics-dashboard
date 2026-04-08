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
        // Debug: print all relevant environment variables
        System.out.println("===== DATABASE CONFIG DEBUG START =====");
        System.out.println("SPRING_PROFILES_ACTIVE: " + env.getProperty("SPRING_PROFILES_ACTIVE"));
        System.out.println("Active profiles (spring.profiles.active): " + env.getProperty("spring.profiles.active"));
        System.out.println("DATABASE_URL: " + (env.getProperty("DATABASE_URL") != null ? "SET" : "NOT SET"));
        if (env.getProperty("DATABASE_URL") != null) {
            System.out.println("DATABASE_URL value: " + env.getProperty("DATABASE_URL").replaceAll(":([^@]+)@", ":***@"));
        }
        System.out.println("SPRING_DATASOURCE_HOST: " + env.getProperty("SPRING_DATASOURCE_HOST"));
        System.out.println("SPRING_DATASOURCE_PORT: " + env.getProperty("SPRING_DATASOURCE_PORT"));
        System.out.println("SPRING_DATASOURCE_DATABASE: " + env.getProperty("SPRING_DATASOURCE_DATABASE"));
        System.out.println("SPRING_DATASOURCE_USERNAME: " + env.getProperty("SPRING_DATASOURCE_USERNAME"));
        System.out.println("SPRING_DATASOURCE_PASSWORD: " + (env.getProperty("SPRING_DATASOURCE_PASSWORD") != null ? "SET" : "NOT SET"));
        System.out.println("spring.datasource.url: " + env.getProperty("spring.datasource.url"));
        System.out.println("spring.datasource.username: " + env.getProperty("spring.datasource.username"));
        System.out.println("spring.datasource.password: " + (env.getProperty("spring.datasource.password") != null ? "SET" : "NOT SET"));
        System.out.println("===== DATABASE CONFIG DEBUG END =====");

        // Approach 1: Use DATABASE_URL (Render's connectionString) as primary source
        String databaseUrl = env.getProperty("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            System.out.println("===== USING DATABASE_URL ENVIRONMENT VARIABLE =====");
            System.out.println("Original DATABASE_URL: " + databaseUrl.replaceAll(":([^@]+)@", ":***@"));
            String jdbcUrl = convertToJdbcUrl(databaseUrl);
            System.out.println("Converted to JDBC URL: " + jdbcUrl);
            System.out.println("===== END DATABASE_URL PROCESSING =====");
            return jdbcUrl;
        }

        // Approach 2: Build from individual components (host, port, database)
        String host = env.getProperty("SPRING_DATASOURCE_HOST");
        String port = env.getProperty("SPRING_DATASOURCE_PORT");
        String database = env.getProperty("SPRING_DATASOURCE_DATABASE");

        if (host != null && port != null && database != null) {
            System.out.println("===== BUILDING JDBC URL FROM INDIVIDUAL COMPONENTS =====");
            String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
            System.out.println("Built JDBC URL: " + jdbcUrl);
            System.out.println("===== END COMPONENTS BUILDING =====");
            return jdbcUrl;
        }

        // Approach 3: Fallback to property from application-prod.properties
        System.out.println("===== USING FALLBACK FROM APPLICATION PROPERTIES =====");
        String fallbackUrl = env.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/logistics");
        System.out.println("Fallback URL from properties: " + fallbackUrl);

        // Ensure fallback is in JDBC format
        if (fallbackUrl != null && !fallbackUrl.isEmpty() && !fallbackUrl.startsWith("jdbc:")) {
            System.out.println("Converting fallback URL to JDBC format");
            String jdbcUrl = convertToJdbcUrl(fallbackUrl);
            System.out.println("Converted fallback URL: " + jdbcUrl);
            System.out.println("===== END FALLBACK PROCESSING =====");
            return jdbcUrl;
        }

        // Final safety check: ensure URL is JDBC format
        if (fallbackUrl != null && !fallbackUrl.startsWith("jdbc:")) {
            System.out.println("WARNING: Fallback URL is not JDBC format, forcing conversion");
            String jdbcUrl = convertToJdbcUrl(fallbackUrl);
            System.out.println("Forced conversion result: " + jdbcUrl);
            System.out.println("===== END SAFETY CHECK =====");
            return jdbcUrl;
        }

        System.out.println("Using final fallback URL: " + fallbackUrl);
        System.out.println("===== END ALL PROCESSING =====");
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