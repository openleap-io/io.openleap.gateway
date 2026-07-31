package io.openleap.gateway.config;

import io.openleap.gateway.service.SpringDynamicClientRegistrationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper;
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

import java.util.Map;

@Profile({"spring"})
@Configuration
@EnableConfigurationProperties({ClientRegistrationProperties.class, OAuth2ClientProperties.class})
@EnableWebFluxSecurity
public class SpringAuthServerConfig {
    private final OAuth2ClientProperties clientProperties;
    private final ClientRegistrationProperties clientRegistrationProperties;
    private final EurekaInstanceConfigBean eurekaInstanceConfigBean;
    String[] allowedServices = {"/identity/**", "/actuator/**"};

    public SpringAuthServerConfig(OAuth2ClientProperties clientProperties, ClientRegistrationProperties clientRegistrationProperties, EurekaInstanceConfigBean eurekaInstanceConfigBean) {
        this.clientProperties = clientProperties;
        this.clientRegistrationProperties = clientRegistrationProperties;
        this.eurekaInstanceConfigBean = eurekaInstanceConfigBean;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.authorizeExchange(auth ->
                        auth.pathMatchers(allowedServices).permitAll()
                                .anyExchange().authenticated())
                .oauth2Login(oauth2Login ->
                        oauth2Login
                                .loginPage("/oauth2/authorization/gateway-client"))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${spring.security.oauth2.client.provider.openleap.issuer-uri}") String issuerUri) {
        return ReactiveJwtDecoders.fromIssuerLocation(issuerUri);
    }

    @Bean
    ReactiveClientRegistrationRepository dynamicClientRegistrationRepository() {
        var registrationDetails = new SpringDynamicClientRegistrationRepository.ClientRegistrationDetails(
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

        var repo = new SpringDynamicClientRegistrationRepository(registrationDetails, staticClients);
        repo.registerNewClients();
        return repo;
    }

}