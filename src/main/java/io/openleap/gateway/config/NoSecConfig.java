package io.openleap.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Profile("nosec")
@Configuration
@EnableWebFluxSecurity
public class NoSecConfig {

    @Bean
    public SecurityWebFilterChain noSecFilterChain(ServerHttpSecurity http) {
        return http.authorizeExchange(e -> e.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }
}
