package com.healthcare.auth.config;

import com.healthcare.auth.security.JwtAuthenticationFilter;
import com.healthcare.auth.security.RestAuthenticationEntryPoint;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration using the modern (Spring Security 6.x)
 * {@link SecurityFilterChain} bean. No {@code WebSecurityConfigurerAdapter}.
 *
 * <p>Stateless, JWT-based:
 * <ul>
 *   <li>CSRF disabled — no session, no cookies, no browser form posts.</li>
 *   <li>Public: POST /api/v1/auth/{register,login,refresh}, /actuator/health/**, /v3/api-docs/**, /swagger-ui/**.</li>
 *   <li>Protected: everything else, including /api/v1/auth/{me,logout} and /api/v1/users/**.</li>
 *   <li>{@link JwtAuthenticationFilter} runs before {@code UsernamePasswordAuthenticationFilter}.</li>
 *   <li>{@link RestAuthenticationEntryPoint} emits the standard error envelope on 401.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final RestAuthenticationEntryPoint authEntryPoint;
    private final AuthProperties props;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          RestAuthenticationEntryPoint authEntryPoint,
                          AuthProperties props) {
        this.jwtFilter = jwtFilter;
        this.authEntryPoint = authEntryPoint;
        this.props = props;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(authEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        // Health and OpenAPI
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService uds,
                                                              org.springframework.security.crypto.password.PasswordEncoder encoder) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(encoder);
        return p;
    }

    /**
     * CORS configuration. Origins come from configuration (env var in
     * production, sensible default in local). Credentials are explicitly
     * allowed because the auth flow may include a refresh-token cookie in
     * a future phase; we do not allow wildcard origins.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = props.getCors().getAllowedOrigins();
        if (origins != null && !origins.isEmpty()) {
            cfg.setAllowedOrigins(origins);
        } else {
            cfg.setAllowedOrigins(List.of("http://localhost:4200"));
        }
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type",
                "X-Correlation-Id", "X-Requested-With", "Accept", "Origin"));
        cfg.setExposedHeaders(List.of("X-Correlation-Id", "Location"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
