package io.openleap.gateway.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DynamicClientRegistrationRepository implements ReactiveClientRegistrationRepository, Iterable<ClientRegistration> {
    Logger logger = LoggerFactory.getLogger(DynamicClientRegistrationRepository.class);

    private final ClientRegistrationDetails clientRegistrationDetails;
    private final Map<String, ClientRegistration> staticClients;
    private final Map<String, ClientRegistration> registrations = new HashMap<>();

    public DynamicClientRegistrationRepository(ClientRegistrationDetails clientRegistrationDetails, Map<String, ClientRegistration> staticClients) {
        this.clientRegistrationDetails = clientRegistrationDetails;
        this.staticClients = staticClients;
    }

    @Override
    public Mono<ClientRegistration> findByRegistrationId(String registrationId) {
        logger.debug("Finding client registration for registrationId: {}", registrationId);
        if (registrations.containsKey(registrationId)) {
            logger.debug("Client registration found for registrationId: {}", registrationId);
            return Mono.just(registrations.get(registrationId));
        } else {
            logger.debug("Client registration not found for registrationId: {}. Registering new client.", registrationId);
            Mono<ClientRegistration> clientRegistration = registerClient(registrationId);
            if (clientRegistration == null) {
                logger.error("Failed to register client with registrationId: {}", registrationId);
            } else {
                registrations.put(registrationId, clientRegistration.block());
                logger.debug("Client registration successful for registrationId: {}", registrationId);
            }
            return clientRegistration;
        }
    }

    private Mono<ClientRegistration> registerClient(String registrationId) {
        WebClient oauth2Client = WebClient.builder().build();
        String token = createRegistrationToken();

        var staticRegistration = staticClients.get(registrationId);
        Assert.notNull(staticRegistration, "Invalid registrationId: " + registrationId);

        var body = Map.of(
                "client_name", staticRegistration.getClientName(),
                "grant_types", List.of(staticRegistration.getAuthorizationGrantType()),
                "scope", String.join(" ", staticRegistration.getScopes()),
                "redirect_uris", List.of(resolveCallbackUri(staticRegistration)),
                "instance_id", clientRegistrationDetails.instanceId,
                "registration_id", registrationId
        );

        var objectNodeMono = oauth2Client
                .post()
                .uri(clientRegistrationDetails.registrationEndpoint())
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(token);
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                })
                .body(Mono.just(body), Map.class)
                .retrieve()
                .bodyToMono(ObjectNode.class)
                .block();

        if (objectNodeMono == null) {
            logger.error("Failed to register client with registrationId: {}", registrationId);
            return null;
        }

        return createClientRegistration(staticRegistration, objectNodeMono);
    }

    private String resolveCallbackUri(ClientRegistration registration) {

        return UriComponentsBuilder.fromUriString(registration.getRedirectUri())
                .build(Map.of(
                        "baseUrl", "",
                        "action", "login",
                        "registrationId", registration.getRegistrationId()))
                .toString();

    }

    private Mono<ClientRegistration> createClientRegistration(ClientRegistration staticRegistration, ObjectNode body) {
        return Mono.just(ClientRegistration.withClientRegistration(staticRegistration)
                .clientId(body.get("client_id").asText())
                .clientSecret(body.get("client_secret").asText())
                .build());
    }

    private String createRegistrationToken() {
        WebClient oauth2Client = WebClient.builder().build();

        var body = new LinkedMultiValueMap<String, String>();
        body.put("grant_type", List.of("client_credentials"));
        body.put("scope", clientRegistrationDetails.registrationScopes());

        var result = oauth2Client
                .post()
                .uri(clientRegistrationDetails.tokenEndpoint())
                .headers(httpHeaders -> {
                    httpHeaders.setBasicAuth(clientRegistrationDetails.registrationUsername(), clientRegistrationDetails.registrationPassword());
                    httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                })
                .body(Mono.just(body), LinkedMultiValueMap.class)
                .retrieve()
                .bodyToMono(ObjectNode.class);

        var accessToken = result.block();
        if (accessToken == null || accessToken.get("access_token") == null) {
            logger.error("Failed to retrieve access token from response body.");
            return null;
        }
        return accessToken.get("access_token").asText();
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return registrations
                .values()
                .iterator();
    }

    public void registerNewClients() {
        staticClients.forEach((key, value) -> findByRegistrationId(key));
    }

    public record ClientRegistrationDetails(
            String instanceId,
            URI registrationEndpoint,
            String registrationUsername,
            String registrationPassword,
            List<String> registrationScopes,
            List<String> grantTypes,
            List<String> redirectUris,
            URI tokenEndpoint,
            String baseUrl
    ) {
    }
}