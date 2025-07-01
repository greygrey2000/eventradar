package com.eventradar.backend.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class GeocodingService {
    private final WebClient webClient = WebClient.create("https://nominatim.openstreetmap.org");

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

    private static class NominatimResult {
        public String lat;
        public String lon;
    }
}
