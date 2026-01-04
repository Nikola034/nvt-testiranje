package nvt.backend.config;

import nvt.backend.filter.JwtAuthenticationFilter;
import nvt.backend.services.auth.UserDetailsServiceImp;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsServiceImp userDetailsServiceImp;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomLogoutHandler logoutHandler;

    public SecurityConfig(UserDetailsServiceImp userDetailsServiceImp,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomLogoutHandler logoutHandler) {
        this.userDetailsServiceImp = userDetailsServiceImp;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.logoutHandler = logoutHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authz -> authz
                        // Public authentication endpoints
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh_token",
                                "/api/v1/auth/register", "/api/v1/auth/activate")
                        .permitAll()

                        // WebSocket endpoint
                        .requestMatchers("/ws/**")
                        .permitAll()

                        .requestMatchers("/api/v1/registration-requests/pending",
                                "/api/v1/registration-requests/pending/paged",
                                "/api/v1/registration-requests/pending/count",
                                "/api/v1/registration-requests/all",
                                "/api/v1/registration-requests/all/paged")
                        .hasAnyAuthority("MANAGER", "ADMIN")

                        .requestMatchers("/api/v1/vehicles",
                                "/api/v1/vehicles/paged",
                                "/api/v1/vehicles/search",
                                "/api/v1/vehicles/search/paged",
                                "/api/v1/vehicles/brands",
                                "/api/v1/vehicles/brands/*/models")
                        .hasAnyAuthority("MANAGER", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/v1/vehicles/{id}")
                        .hasAnyAuthority("MANAGER", "ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/v1/registration-requests/*/process")
                        .hasAnyAuthority("MANAGER", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/v1/registration-requests/{id}")
                        .hasAnyAuthority("MANAGER", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/registration-requests")
                        .hasAnyAuthority("CUSTOMER", "MANAGER", "ADMIN")

                        .anyRequest()
                        .authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .addLogoutHandler(logoutHandler)
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpStatus.OK.value());
                        })
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}