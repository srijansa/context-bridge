package com.secpipeline.platform.config;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/underwriting/**").hasRole("UNDERWRITER")
                        .requestMatchers("/api/claims/**").hasRole("CLAIMS_ADJUSTER")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthoritiesConverter())))
                .build();
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthoritiesConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> Stream.concat(
                        roles(jwt).stream().map(role -> new SimpleGrantedAuthority("ROLE_" + normalizeRole(role))),
                        scopes(jwt).stream().map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope)))
                .map(GrantedAuthority.class::cast)
                .toList());
        return converter;
    }

    @SuppressWarnings("unchecked")
    private Collection<String> roles(Jwt jwt) {
        Object roles = jwt.getClaims().getOrDefault("roles", jwt.getClaims().get("groups"));
        return roles instanceof Collection<?> values
                ? values.stream()
                        .map(String::valueOf)
                        .map(this::normalizeRole)
                        .filter(role -> !role.isBlank())
                        .toList()
                : List.of();
    }

    private Collection<String> scopes(Jwt jwt) {
        String scope = jwt.getClaimAsString("scope");
        return scope == null || scope.isBlank()
                ? List.of()
                : Stream.of(scope.split(" "))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList();
    }

    private String normalizeRole(String role) {
        String normalized = role.trim();
        normalized = normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
        return normalized.toUpperCase(Locale.ROOT);
    }
}
