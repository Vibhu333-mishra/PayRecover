package com.payrecover.payrecoverai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS = Cross-Origin Resource Sharing.
 *
 * Your React app will run on a different "origin" (e.g. http://localhost:3000
 * or http://localhost:5173) than this Spring Boot API (http://localhost:8080).
 * Browsers block JavaScript from calling a different origin by default, for
 * security reasons. This config explicitly tells the browser: "requests from
 * these frontend origins are allowed to call my /api/** endpoints."
 *
 * Without this, every fetch() call from React would fail with a CORS error
 * in the browser console, even though the API works fine in Postman.
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(
                                "http://localhost:3000", // Create React App default
                                "http://localhost:5173"  // Vite default
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
