package com.api.expo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.api.expo.services.CustomUserDetailsService;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class ConfigurationSecurityApplication {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public JwtRequestFilter jwtRequestFilter() {
        return new JwtRequestFilter();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Utiliser allowedOriginPatterns au lieu de allowedOrigins
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtRequestFilter jwtRequestFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Activer CORS
            .csrf(csrf -> csrf.disable())
            .userDetailsService(customUserDetailsService)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v3/api-docs").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                .requestMatchers("/api/register").permitAll()
                .requestMatchers("/api/login").permitAll()
                .requestMatchers("/api/logout").permitAll()
                .requestMatchers("/api/verify-email").permitAll()
                .requestMatchers("/api/password/reset-request").permitAll()
                .requestMatchers("/api/password/reset").permitAll()
                .requestMatchers("/api/verification/resend").permitAll()
                .requestMatchers("/api/").permitAll()
                .requestMatchers("/api/files/**").permitAll()
                .requestMatchers("/api/skills/all_skills").permitAll()
                .requestMatchers("/api/contact").permitAll()
                .requestMatchers("/ws/**").permitAll() // Assure-toi d'autoriser les endpoints WebSocket
                .requestMatchers("/api/auth/login").permitAll() // Assure-toi d'autoriser les endpoints WebSocket/verify-email/{token}
                .requestMatchers("/api/auth/register").permitAll() // Assure-toi d'autoriser les endpoints WebSocket
                .requestMatchers("/api/auth/verify-email").permitAll() // Assure-toi d'autoriser les endpoints WebSocket
                .requestMatchers("/api/auth/**").permitAll() // Assure-toi d'autoriser les endpoints WebSocket
                                // Autorisez l'accès public aux ressources statiques
                .requestMatchers("/api/files/images/**").permitAll()
                .requestMatchers("/api/files/profile-pictures/**").permitAll()
                .requestMatchers("/api/files/attachments/**").permitAll()
                .requestMatchers("/api/files/voice-notes/**").permitAll()
                .requestMatchers("/api/files/videos/**").permitAll()





                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            );

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}