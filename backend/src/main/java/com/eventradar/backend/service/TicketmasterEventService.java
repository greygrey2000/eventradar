package com.eventradar.backend.service;

import com.eventradar.backend.dto.TicketmasterEventDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TicketmasterEventService {
    @Value("${ticketmaster.api.key}")
    private String ticketmasterApiKey;

    private final WebClient webClient;

    public TicketmasterEventService() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2 MB
                .build();
        this.webClient = WebClient.builder()
                .baseUrl("https://app.ticketmaster.com/discovery/v2")
                .exchangeStrategies(strategies)
                .build();
    }

    public List<TicketmasterEventDTO> fetchEvents(Map<String, String> params) {
        // Anfrage immer auf 10 Ergebnisse limitieren
        Map<String, String> paramsWithLimit = new HashMap<>(params);
        paramsWithLimit.putIfAbsent("size", "10");
        WebClient.RequestHeadersSpec<?> req = webClient.get().uri(uriBuilder -> {
            uriBuilder.path("/events.json").queryParam("apikey", ticketmasterApiKey);
            paramsWithLimit.forEach((k, v) -> {
                if (!k.equalsIgnoreCase("apikey")) uriBuilder.queryParam(k, v);
            });
            return uriBuilder.build();
        });
        Mono<HashMap<String, Object>> responseMono = req.accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(new org.springframework.core.ParameterizedTypeReference<HashMap<String, Object>>(){});
        HashMap<String, Object> body = responseMono.block();
        List<TicketmasterEventDTO> result = new ArrayList<>();
        if (body != null && body.containsKey("_embedded")) {
            Map<String, Object> embedded = (Map<String, Object>) body.get("_embedded");
            if (embedded.containsKey("events")) {
                List<Map<String, Object>> events = (List<Map<String, Object>>) embedded.get("events");
                for (Map<String, Object> event : events) {
                    TicketmasterEventDTO dto = new TicketmasterEventDTO();
                    dto.setId((String) event.get("id"));
                    dto.setName((String) event.get("name"));
                    Map<String, Object> dates = (Map<String, Object>) event.get("dates");
                    if (dates != null && dates.containsKey("start")) {
                        Map<String, Object> start = (Map<String, Object>) dates.get("start");
                        dto.setDate((String) start.get("localDate"));
                    }
                    List<Map<String, Object>> images = (List<Map<String, Object>>) event.get("images");
                    if (images != null && !images.isEmpty()) {
                        Map<String, Object> img = images.get(0);
                        dto.setImageUrl((String) img.get("url"));
                    }
                    dto.setUrl((String) event.get("url"));
                    if (event.containsKey("_embedded")) {
                        Map<String, Object> emb = (Map<String, Object>) event.get("_embedded");
                        if (emb.containsKey("venues")) {
                            List<Map<String, Object>> venues = (List<Map<String, Object>>) emb.get("venues");
                            if (!venues.isEmpty()) {
                                Map<String, Object> venue = venues.get(0);
                                dto.setVenue((String) venue.get("name"));
                            }
                        }
                    }
                    List<String> classList = new ArrayList<>();
                    if (event.containsKey("classifications")) {
                        List<Map<String, Object>> classifications = (List<Map<String, Object>>) event.get("classifications");
                        for (Map<String, Object> c : classifications) {
                            if (c.containsKey("segment")) {
                                Map<String, Object> seg = (Map<String, Object>) c.get("segment");
                                classList.add((String) seg.get("name"));
                            }
                        }
                    }
                    dto.setClassifications(classList);
                    result.add(dto);
                }
            }
        }
        return result;
    }
}
