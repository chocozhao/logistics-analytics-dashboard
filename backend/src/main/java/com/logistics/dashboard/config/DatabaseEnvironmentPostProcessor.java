package com.logistics.dashboard.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

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

        // Check and convert DATABASE_URL if present
        String databaseUrl = environment.getProperty(DATABASE_URL_KEY);
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            System.out.println("Found DATABASE_URL: " + maskPassword(databaseUrl));
            String convertedUrl = convertToJdbcUrl(databaseUrl);
            if (!convertedUrl.equals(databaseUrl)) {
                System.out.println("Converted DATABASE_URL to: " + maskPassword(convertedUrl));
                addProperty(environment, DATABASE_URL_KEY, convertedUrl);
            }
        } else {
            System.out.println("DATABASE_URL not set");
        }

        // Check and convert SPRING_DATASOURCE_URL if present
        String springDatasourceUrl = environment.getProperty(SPRING_DATASOURCE_URL_KEY);
        if (springDatasourceUrl != null && !springDatasourceUrl.isEmpty()) {
            System.out.println("Found SPRING_DATASOURCE_URL: " + maskPassword(springDatasourceUrl));
            String convertedUrl = convertToJdbcUrl(springDatasourceUrl);
            if (!convertedUrl.equals(springDatasourceUrl)) {
                System.out.println("Converted SPRING_DATASOURCE_URL to: " + maskPassword(convertedUrl));
                addProperty(environment, SPRING_DATASOURCE_URL_KEY, convertedUrl);
                // Also update spring.datasource.url property since Spring Boot will read this
                addProperty(environment, SPRING_DATASOURCE_URL_PROPERTY, convertedUrl);
            }
        } else {
            System.out.println("SPRING_DATASOURCE_URL not set");
        }

        // Ensure spring.datasource.url is also in JDBC format
        String springDatasourceUrlProperty = environment.getProperty(SPRING_DATASOURCE_URL_PROPERTY);
        if (springDatasourceUrlProperty != null && !springDatasourceUrlProperty.isEmpty()) {
            System.out.println("Found spring.datasource.url property: " + maskPassword(springDatasourceUrlProperty));
            String convertedUrl = convertToJdbcUrl(springDatasourceUrlProperty);
            if (!convertedUrl.equals(springDatasourceUrlProperty)) {
                System.out.println("Converted spring.datasource.url to: " + maskPassword(convertedUrl));
                addProperty(environment, SPRING_DATASOURCE_URL_PROPERTY, convertedUrl);
            }
        }

        System.out.println("=== End DatabaseEnvironmentPostProcessor ===");
    }

    private String convertToJdbcUrl(String url) {
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
        // This handles edge cases
        return "jdbc:" + url;
    }

    private void addProperty(ConfigurableEnvironment environment, String key, String value) {
        MutablePropertySources propertySources = environment.getPropertySources();

        // Create a new property source with our converted value
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);

        // Add it with highest priority so our converted value takes precedence
        propertySources.addFirst(new MapPropertySource("databaseUrlFix", map));
    }

    private String maskPassword(String url) {
        // Simple password masking for logs
        return url.replaceAll(":([^@]+)@", ":***@");
    }
}