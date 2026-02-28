package io.openleap.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "gateway.security")
public final class SecurityProperties {
    private List<String> allowedServices;

    public List<String> getAllowedServices() {
        return allowedServices;
    }

    public void setAllowedServices(List<String> allowedServices) {
        this.allowedServices = allowedServices;
    }
}
