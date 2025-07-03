package com.eventradar.backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class GeocodingService {
    private final WebClient webClient;

    @Autowired
    public GeocodingService(@Qualifier("nominatimWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Optional<double[]> geocode(String location) {
        Mono<NominatimResult[]> mono = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", location)
                        .queryParam("format", "json")
                        .queryParam("limit", "1")
                        .build())
                .header("User-Agent", "eventradar/1.0")
                .retrieve()
                .bodyToMono(NominatimResult[].class);
        NominatimResult[] results = mono.block();
        if (results != null && results.length > 0) {
            try {
                double lat = Double.parseDouble(results[0].lat);
                double lon = Double.parseDouble(results[0].lon);
                return Optional.of(new double[]{lat, lon});
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    // Liefert bis zu 'limit' Ortsvorschläge für Autocomplete
    public java.util.List<String> suggest(String query, int limit) {
        Mono<NominatimResult[]> mono = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("limit", String.valueOf(limit))
                        .build())
                .header("User-Agent", "eventradar/1.0")
                .retrieve()
                .bodyToMono(NominatimResult[].class);
        NominatimResult[] results = mono.block();
        java.util.List<String> suggestions = new java.util.ArrayList<>();
        if (results != null) {
            for (NominatimResult r : results) {
                if (r.display_name != null && !r.display_name.isBlank()) {
                    suggestions.add(r.display_name);
                }
            }
        }
        return suggestions;
    }

    private static class NominatimResult {
        public String lat;
        public String lon;
        public String display_name;
    }
}
