package io.openleap.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.List;

@ConfigurationProperties(prefix = "oleap.client.registration")
public final class ClientRegistrationProperties {
    URI registrationEndpoint;
    String registrationUsername;
    String registrationPassword;
    List<String> registrationScopes;
    List<String> grantTypes;
    List<String> redirectUris;
    URI tokenEndpoint;
    String baseUrl;
    String unregistrationEndpoint;

    public String getUnregistrationEndpoint() {
        return unregistrationEndpoint;
    }

    public void setUnregistrationEndpoint(String unregistrationEndpoint) {
        this.unregistrationEndpoint = unregistrationEndpoint;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public URI getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    public void setRegistrationEndpoint(URI registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
    }

    public String getRegistrationUsername() {
        return registrationUsername;
    }

    public void setRegistrationUsername(String registrationUsername) {
        this.registrationUsername = registrationUsername;
    }

    public String getRegistrationPassword() {
        return registrationPassword;
    }

    public void setRegistrationPassword(String registrationPassword) {
        this.registrationPassword = registrationPassword;
    }

    public List<String> getRegistrationScopes() {
        return registrationScopes;
    }

    public void setRegistrationScopes(List<String> registrationScopes) {
        this.registrationScopes = registrationScopes;
    }

    public List<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grantTypes = grantTypes;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public URI getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(URI tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }
}

