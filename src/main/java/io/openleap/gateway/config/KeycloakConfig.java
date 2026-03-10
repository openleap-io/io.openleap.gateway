package io.openleap.gateway.config;

import io.openleap.gateway.service.KeycloakDynamicClientRegistrationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Map;
import java.util.Optional;

@Profile({"keycloak"})
@Configuration
@EnableConfigurationProperties({ClientRegistrationProperties.class, OAuth2ClientProperties.class, CorsProperties.class, SecurityProperties.class})
@EnableWebFluxSecurity
public class KeycloakConfig {
    private final OAuth2ClientProperties clientProperties;
    private final ClientRegistrationProperties clientRegistrationProperties;
    private final CorsProperties corsProperties;
    private final SecurityProperties securityProperties;

    public KeycloakConfig(OAuth2ClientProperties clientProperties,
                          ClientRegistrationProperties clientRegistrationProperties,
                          CorsProperties corsProperties,
                          SecurityProperties securityProperties) {
        this.clientProperties = clientProperties;
        this.clientRegistrationProperties = clientRegistrationProperties;
        this.corsProperties = corsProperties;
        this.securityProperties = securityProperties;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            Optional<ReactiveClientRegistrationRepository> clientRegistrationRepository) {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.authorizeExchange(auth ->
                auth.pathMatchers(securityProperties.getAllowedServices().toArray(String[]::new)).permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()));
        clientRegistrationRepository.ifPresent(repo -> http.oauth2Login(Customizer.withDefaults()));
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(corsProperties.getAllowCredentials());
        configuration.setExposedHeaders(corsProperties.getExposedHeaders());
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${spring.security.oauth2.client.provider.openleap.issuer-uri}") String issuerUri) {
        return ReactiveJwtDecoders.fromIssuerLocation(issuerUri);
    }

    @Bean
    @ConditionalOnBooleanProperty("client-registration.enabled")
    ReactiveClientRegistrationRepository keycloakDynamicClientRegistrationRepository(EurekaInstanceConfigBean eurekaInstanceConfigBean) {
        var registrationDetails = new KeycloakDynamicClientRegistrationRepository.ClientRegistrationDetails(
                eurekaInstanceConfigBean.getInstanceId(),
                clientRegistrationProperties.getRegistrationEndpoint(),
                clientRegistrationProperties.getRegistrationUsername(),
                clientRegistrationProperties.getRegistrationPassword(),
                clientRegistrationProperties.getRegistrationScopes(),
                clientRegistrationProperties.getGrantTypes(),
                clientRegistrationProperties.getRedirectUris(),
                clientRegistrationProperties.getTokenEndpoint(),
                clientRegistrationProperties.getBaseUrl());

        Map<String, ClientRegistration> staticClients = (new OAuth2ClientPropertiesMapper(clientProperties)).asClientRegistrations();

        var repo = new KeycloakDynamicClientRegistrationRepository(registrationDetails, staticClients);
        repo.registerNewClients();
        return repo;
    }
}
