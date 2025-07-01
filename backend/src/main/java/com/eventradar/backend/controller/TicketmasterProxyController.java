package com.eventradar.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/ticketmaster")
public class TicketmasterProxyController {

    @Value("${ticketmaster.api.key}")
    private String ticketmasterApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/events")
    public ResponseEntity<String> proxyEvents(@RequestParam Map<String, String> params) {
        String url = "https://app.ticketmaster.com/discovery/v2/events.json";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("apikey", ticketmasterApiKey);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase("apikey")) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        String finalUrl = builder.toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(finalUrl, HttpMethod.GET, entity, String.class);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
}
