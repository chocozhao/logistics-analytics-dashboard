package com.logistics.dashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * CORS allowed origins. Can be configured via:
     * - application.properties: cors.allowed-origins
     * - Environment variable: CORS_ALLOWED_ORIGINS
     * - Default: http://localhost:3000,http://localhost:5173 (development)
     * In production, set to frontend URL (e.g., https://logistics-frontend.onrender.com)
     */
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        System.out.println("CORS配置 - 允许的来源: " + String.join(", ", allowedOrigins));
        System.out.println("CORS配置 - 当前环境: " + (Arrays.asList(allowedOrigins).contains("https://*.onrender.com") ? "生产环境" : "开发环境"));

        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins) // 使用allowedOriginPatterns以支持通配符
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}