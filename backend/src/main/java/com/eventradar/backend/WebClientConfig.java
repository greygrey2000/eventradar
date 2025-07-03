package com.eventradar.backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient ticketmasterWebClient() {
        return WebClient.builder()
                .baseUrl("https://app.ticketmaster.com/discovery/v2")
                .build();
    }
    @Bean
    public WebClient nominatimWebClient() {
        return WebClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader("User-Agent", "eventradar/1.0")
                .build();
    }
}
