package io.openleap.gateway.config;

import io.openleap.gateway.service.SpringDynamicClientRegistrationRepository;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Map;

@Profile("local")
@Configuration
@EnableConfigurationProperties({ClientRegistrationProperties.class, OAuth2ClientProperties.class})
@EnableWebFluxSecurity
public class LocalConfig {
    private final OAuth2ClientProperties clientProperties;
    private final ClientRegistrationProperties clientRegistrationProperties;
    String[] allowedServices = {"/identity/**", "/actuator/**"};

    public LocalConfig(OAuth2ClientProperties clientProperties, ClientRegistrationProperties clientRegistrationProperties) {
        this.clientProperties = clientProperties;
        this.clientRegistrationProperties = clientRegistrationProperties;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.authorizeExchange(auth ->
                auth.pathMatchers(allowedServices).permitAll()
                        .anyExchange().authenticated());
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
    }

    @Bean
    ReactiveClientRegistrationRepository dynamicClientRegistrationRepository() {
        var registrationDetails = new SpringDynamicClientRegistrationRepository.ClientRegistrationDetails(
                "local-client",
                clientRegistrationProperties.getRegistrationEndpoint(),
                clientRegistrationProperties.getRegistrationUsername(),
                clientRegistrationProperties.getRegistrationPassword(),
                clientRegistrationProperties.getRegistrationScopes(),
                clientRegistrationProperties.getGrantTypes(),
                clientRegistrationProperties.getRedirectUris(),
                clientRegistrationProperties.getTokenEndpoint(),
                clientRegistrationProperties.getBaseUrl());

        Map<String, ClientRegistration> staticClients = (new OAuth2ClientPropertiesMapper(clientProperties)).asClientRegistrations();

        var repo = new SpringDynamicClientRegistrationRepository(registrationDetails, staticClients);
        repo.registerNewClients();
        return repo;
    }

}