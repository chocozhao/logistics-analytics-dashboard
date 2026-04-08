package com.logistics.dashboard.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import java.util.HashMap;
import java.util.Map;

/**
 * EnvironmentPostProcessor that ensures database URLs are in proper JDBC format.
 *
 * This processor runs early in the Spring Boot startup process, before any
 * DataSource initialization occurs. It checks for common environment variables
 * that might contain PostgreSQL connection strings without the "jdbc:" prefix
 * (like Render.com's DATABASE_URL and SPRING_DATASOURCE_URL) and ensures they
 * are converted to proper JDBC format.
 *
 * Without this processor, Spring Boot's DataSourceAutoConfiguration might
 * initialize a DataSource with an invalid JDBC URL, causing startup failures.
 */
public class DatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DATABASE_URL_KEY = "DATABASE_URL";
    private static final String SPRING_DATASOURCE_URL_KEY = "SPRING_DATASOURCE_URL";
    private static final String SPRING_DATASOURCE_URL_PROPERTY = "spring.datasource.url";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                      SpringApplication application) {

        // Debug logging
        System.out.println("=== DatabaseEnvironmentPostProcessor ===");
        System.out.println("Processing environment variables for database URLs");

        // Process DATABASE_URL if present (Render's primary format)
        processDatabaseUrl(environment, DATABASE_URL_KEY);

        // Process SPRING_DATASOURCE_URL if present (Render's alternative)
        processDatabaseUrl(environment, SPRING_DATASOURCE_URL_KEY);

        // Also process spring.datasource.url property
        processDatabaseUrl(environment, SPRING_DATASOURCE_URL_PROPERTY);

        System.out.println("=== End DatabaseEnvironmentPostProcessor ===");
    }

    private void processDatabaseUrl(ConfigurableEnvironment environment, String urlKey) {
        String url = environment.getProperty(urlKey);
        if (url == null || url.isEmpty()) {
            System.out.println(urlKey + " not set");
            return;
        }

        System.out.println("Found " + urlKey + ": " + maskPassword(url));

        try {
            // Parse the connection string
            ParsedConnection parsed = parseConnectionString(url);

            // Build properties map
            Map<String, Object> properties = new HashMap<>();

            // Set the JDBC URL (without username/password in URL)
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s",
                parsed.host, parsed.port, parsed.database);
            System.out.println("Setting spring.datasource.url to: " + jdbcUrl);
            properties.put("spring.datasource.url", jdbcUrl);

            // Set username and password as separate properties
            System.out.println("Setting spring.datasource.username to: " + parsed.username);
            properties.put("spring.datasource.username", parsed.username);
            properties.put("spring.datasource.password", parsed.password);

            // Also set uppercase versions for compatibility
            properties.put("SPRING_DATASOURCE_URL", jdbcUrl);
            properties.put("SPRING_DATASOURCE_USERNAME", parsed.username);
            properties.put("SPRING_DATASOURCE_PASSWORD", parsed.password);

            // Add properties to environment
            addProperties(environment, properties);

        } catch (Exception e) {
            System.out.println("Error parsing " + urlKey + ": " + e.getMessage());
            // Fallback: just convert to JDBC format
            String convertedUrl = ensureJdbcPrefix(url);
            if (!convertedUrl.equals(url)) {
                System.out.println("Fallback conversion for " + urlKey + " to: " + maskPassword(convertedUrl));
                addProperty(environment, urlKey, convertedUrl);
                if (urlKey.equals(SPRING_DATASOURCE_URL_KEY) || urlKey.equals(DATABASE_URL_KEY)) {
                    addProperty(environment, SPRING_DATASOURCE_URL_PROPERTY, convertedUrl);
                }
            }
        }
    }

    private ParsedConnection parseConnectionString(String url) throws URISyntaxException {
        ParsedConnection result = new ParsedConnection();

        // Ensure URL is in URI-parsable format
        String uriString = url;
        if (url.startsWith("postgresql://")) {
            uriString = "http://" + url.substring("postgresql://".length());
        } else if (url.startsWith("jdbc:postgresql://")) {
            uriString = "http://" + url.substring("jdbc:postgresql://".length());
        }

        URI uri = new URI(uriString);

        // Extract username and password
        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            String[] parts = userInfo.split(":");
            result.username = parts[0];
            result.password = parts.length > 1 ? parts[1] : "";
        } else {
            result.username = "";
            result.password = "";
        }

        // Extract host and port
        result.host = uri.getHost();
        result.port = uri.getPort() > 0 ? uri.getPort() : 5432; // Default PostgreSQL port

        // Extract database name
        String path = uri.getPath();
        if (path != null && path.startsWith("/")) {
            result.database = path.substring(1);
        } else {
            result.database = "";
        }

        return result;
    }

    private static class ParsedConnection {
        String host;
        int port;
        String database;
        String username;
        String password;
    }

    private String ensureJdbcPrefix(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        // If already starts with "jdbc:", return as-is
        if (url.startsWith("jdbc:")) {
            return url;
        }

        // If starts with "postgresql://", prepend "jdbc:"
        if (url.startsWith("postgresql://")) {
            return "jdbc:" + url;
        }

        // For any other URL that doesn't start with "jdbc:", prepend it
        return "jdbc:" + url;
    }

    private void addProperty(ConfigurableEnvironment environment, String key, String value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        addProperties(environment, map);
    }

    private void addProperties(ConfigurableEnvironment environment, Map<String, Object> properties) {
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new MapPropertySource("databaseConfig", properties));
    }

    private String maskPassword(String url) {
        // Simple password masking for logs
        return url.replaceAll(":([^@]+)@", ":***@");
    }
}