package com.eventradar.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.eventradar.backend.service.GeocodingService;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;

@RestController
@RequestMapping("/api/places")
public class PlaceController {
    @Autowired
    private GeocodingService geocodingService;

    @Value("${ticketmaster.api.key}")
    private String ticketmasterApiKey;

    @Autowired
    @Qualifier("ticketmasterWebClient")
    private org.springframework.web.reactive.function.client.WebClient ticketmasterWebClient;

    @GetMapping("/suggest/places")
    public ResponseEntity<List<String>> suggestPlaces(@RequestParam("q") String query) {
        List<String> suggestions = geocodingService.suggest(query, 5);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/suggest/venues")
    public ResponseEntity<?> suggestVenues(@RequestParam("keyword") String keyword, @RequestParam(value = "latlong", required = false) String latlong) {
        try {
            var response = ticketmasterWebClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                            .path("/suggest")
                            .queryParam("apikey", ticketmasterApiKey)
                            .queryParam("keyword", keyword);
                        if (latlong != null && !latlong.isBlank()) {
                            builder = builder.queryParam("latlong", latlong).queryParam("radius", "50").queryParam("unit", "km");
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            return ResponseEntity.status(502).body("Ticketmaster Suggest-Proxy Fehler: " + e.getMessage());
        }
    }

    @GetMapping("/geocode")
    public ResponseEntity<?> geocode(@RequestParam("location") String location) {
        var coordsOpt = geocodingService.geocode(location);
        if (coordsOpt.isPresent()) {
            double[] coords = coordsOpt.get();
            java.util.Map<String, Double> result = new java.util.HashMap<>();
            result.put("lat", coords[0]);
            result.put("lng", coords[1]);
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(404).body("Ort konnte nicht gefunden werden");
        }
    }
}
