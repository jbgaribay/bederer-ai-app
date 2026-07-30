package com.jesusbarocio.bedererai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Gates the one costly endpoint (video upload/analysis + delete) behind a
 * single shared demo login, since this app is meant to be handed to a
 * recruiter as a live URL. Credentials come from environment variables so
 * they're never hardcoded in source control - see README for how to set
 * DEMO_USERNAME / DEMO_PASSWORD in the deployment environment.
 *
 * History (GET) stays public so a recruiter can browse past analyses without
 * logging in first; only the money-costing action requires the demo login.
 *
 * CORS is wide open in local dev (docker-compose puts frontend and backend
 * behind the same nginx origin, so it barely matters there) but should be
 * locked to the real CloudFront domain via ALLOWED_ORIGINS once deployed,
 * since frontend (CloudFront) and backend (App Runner) live on different
 * domains in the AWS deployment - see README.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        http
            .cors().configurationSource(corsConfigurationSource)
            .and()
            .csrf().disable()
            .authorizeRequests(authorize -> authorize
                .antMatchers(HttpMethod.GET, "/api/swings/**").permitAll()
                .antMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic()
            .and()
            .headers().frameOptions().disable();

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:*}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${app.demo.username:demo}") String demoUsername,
            @Value("${app.demo.password:changeme}") String demoPassword) {
        UserDetails demoUser = User.builder()
                .username(demoUsername)
                .password(passwordEncoder.encode(demoPassword))
                .roles("DEMO")
                .build();
        return new InMemoryUserDetailsManager(demoUser);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
