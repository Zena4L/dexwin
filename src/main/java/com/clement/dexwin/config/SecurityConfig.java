package com.clement.dexwin.config;

import com.clement.dexwin.domain.models.users.Roles;
import com.clement.dexwin.domain.models.users.User;
import com.clement.dexwin.domain.repository.UserRepository;
import com.clement.dexwin.domain.security.JwtAuthenticationEntryPoint;
import com.clement.dexwin.exceptions.NotFoundException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Collection;
import java.util.List;

import static org.springframework.security.oauth2.core.authorization.OAuth2AuthorizationManagers.hasScope;

@Component
@EnableWebSecurity
@Slf4j
@AllArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final RsaProperties rsa;
    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.exceptionHandling(ex ->
                ex.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()));
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(
                cors -> cors.configurationSource(
                        request -> {
                            CorsConfiguration configuration = new CorsConfiguration();
                            configuration.setAllowedOrigins(List.of("*"));
                            configuration.setAllowedHeaders(List.of("*"));
                            configuration.setAllowCredentials(true);
                            configuration.setExposedHeaders(List.of("If-Match"));
                            configuration.setAllowedMethods(
                                    List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
                            return configuration;
                        }
                )
        );
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/error")
                .permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/signup", "/api/v1/login").permitAll()
                .requestMatchers("/api/v1/users/**").access(hasScope(Roles.ADMIN.name()))
                .anyRequest().authenticated()
        );
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.oauth2ResourceServer(oauth ->
                oauth.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(this.jwtAuthConverter())
                )
        );
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(rsa.publicKey()).build();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(rsa.publicKey()).privateKey(rsa.privateKey()).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthConverter() {
        return new Converter<Jwt, AbstractAuthenticationToken>() {
            @Override
            public AbstractAuthenticationToken convert(@NonNull Jwt source) {
                return handleSelfSignedToken(source);
            }

            private AbstractAuthenticationToken handleSelfSignedToken(Jwt source) {
                log.info("JWT Subject: {}", source.getSubject());

                User user = userRepository.findByEmail(source.getSubject())
                        .orElseThrow(() -> new NotFoundException("User not found"));
                log.info("User found: {}", user.getEmail());

                String scope = source.getClaim("scope");
                Collection<? extends GrantedAuthority> authorities = getAuthorities(scope);

                authorities.forEach(auth -> log.info("User authority: {}",
                        auth.getAuthority()));

                return new UserAuthentication(source, user, authorities);
            }

            private Collection<? extends GrantedAuthority> getAuthorities(String scope) {
                return List.of(new SimpleGrantedAuthority(scope));
            }
        };
    }
}