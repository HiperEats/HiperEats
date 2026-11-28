package pt.ua.hipereats.adapters.in.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import jakarta.servlet.http.HttpServletResponse;
import pt.ua.hipereats.adapters.out.security.JwtKeys;
import pt.ua.hipereats.adapters.out.security.SecurityProperties;
import tools.jackson.databind.json.JsonMapper;

/**
 * Who can call what (HE-28).
 *
 * <ul>
 *   <li>Open to everyone: logging in ({@code POST /api/v1/sessions}), {@code /api/status} and
 *       {@code /actuator/health}.</li>
 *   <li>Everything else needs a valid access token: {@code Authorization: Bearer <token>}.
 *       Without one, or with a forged or expired one, the answer is 401.</li>
 *   <li>The role is checked on each endpoint with {@code @PreAuthorize}, next to the endpoint
 *       itself; a wrong role gets 403.</li>
 * </ul>
 * Stateless: no server session and no cookie, so there is no CSRF to protect against.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper json) throws Exception {
        AuthenticationEntryPoint notAuthenticated = (request, response, exception) -> {
            response.setHeader("WWW-Authenticate", "Bearer");
            writeProblem(response, json, HttpStatus.UNAUTHORIZED,
                    "Log in first and send the access token (Authorization: Bearer <token>).");
        };
        AccessDeniedHandler wrongRole = (request, response, exception) ->
                writeProblem(response, json, HttpStatus.FORBIDDEN, "Your role cannot do this.");

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/api/v1/sessions").permitAll()
                        .requestMatchers("/api/status", "/actuator/health", "/actuator/health/**", "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(server -> server
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesFromToken()))
                        .authenticationEntryPoint(notAuthenticated)
                        .accessDeniedHandler(wrongRole))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(notAuthenticated)
                        .accessDeniedHandler(wrongRole))
                .build();
    }

    /** The claim "role" (for example COURIER) becomes the authority ROLE_COURIER. */
    private static JwtAuthenticationConverter rolesFromToken() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    JwtEncoder jwtEncoder(SecurityProperties properties) {
        return JwtKeys.encoder(properties);
    }

    /**
     * Checks the signature and the expiry of each token, plus every other token validator bean,
     * such as {@link ActiveAccountTokenValidator} (none in a web slice test).
     */
    @Bean
    JwtDecoder jwtDecoder(SecurityProperties properties, List<OAuth2TokenValidator<Jwt>> extraValidators) {
        NimbusJwtDecoder decoder = JwtKeys.decoder(properties);
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefault());
        validators.addAll(extraValidators);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    private static void writeProblem(HttpServletResponse response, JsonMapper json, HttpStatus status, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detail));
    }
}
