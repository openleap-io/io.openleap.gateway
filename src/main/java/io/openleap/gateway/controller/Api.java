package io.openleap.gateway.controller;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class Api {


    @GetMapping(value = "/token")
    public Mono<String> getToken(@RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient) {
        return Mono.just(authorizedClient.getAccessToken().getTokenValue());
    }

    @GetMapping(value = "/user")
    public Mono<OidcUser> getUser(OAuth2AuthenticationToken oauth2Authentication) {
        OidcUser oidcUser = (OidcUser) oauth2Authentication.getPrincipal();
        return Mono.just(oidcUser);
    }


}

