package nvt.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*") // Allow all origins
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // Allow all standard HTTP methods
                .allowedHeaders("*") // Allow all headers
                .exposedHeaders("Authorization", "Content-Type") // Expose specific headers if needed
                .allowCredentials(false) // Disable credentials for security when allowing all origins
                .maxAge(3600); // Cache preflight responses for 1 hour
    }
}