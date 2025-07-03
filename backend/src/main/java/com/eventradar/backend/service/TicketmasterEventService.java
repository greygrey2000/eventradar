package com.eventradar.backend.service;

import com.eventradar.backend.dto.TicketmasterEventDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TicketmasterEventService {
    @Value("${ticketmaster.api.key}")
    private String ticketmasterApiKey;

    private final WebClient webClient;

    private static final Logger log = LoggerFactory.getLogger(TicketmasterEventService.class);

    public TicketmasterEventService(@Qualifier("ticketmasterWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public List<TicketmasterEventDTO> fetchEvents(Map<String, String> params) {
        Map<String, String> paramsWithLimit = new HashMap<>(params);
        paramsWithLimit.putIfAbsent("size", "10");
        try {
            URI uri = buildUri(paramsWithLimit);
            Mono<HashMap<String, Object>> responseMono = webClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.value() == 401,
                        resp -> resp.bodyToMono(String.class).map(msg -> new RuntimeException("Nicht autorisiert (401): Prüfe API-Key.")))
                    .onStatus(status -> status.value() == 429,
                        resp -> resp.bodyToMono(String.class).map(msg -> new RuntimeException("Rate-Limit erreicht (429): Bitte später erneut versuchen.")))
                    .onStatus(status -> status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class).map(msg -> new RuntimeException("Serverfehler bei Ticketmaster: " + msg)))
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<HashMap<String, Object>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .retryWhen(Retry.backoff(1, Duration.ofSeconds(2)).filter(ex -> ex instanceof WebClientResponseException && ((WebClientResponseException) ex).getStatusCode().value() == 429));
            HashMap<String, Object> body = responseMono.block();
            List<TicketmasterEventDTO> result = new ArrayList<>();
            if (body != null && body.containsKey("_embedded")) {
                Map<String, Object> embedded = (Map<String, Object>) body.get("_embedded");
                if (embedded.containsKey("events")) {
                    List<Map<String, Object>> events = (List<Map<String, Object>>) embedded.get("events");
                    for (Map<String, Object> event : events) {
                        result.add(parseEvent(event));
                    }
                }
            }
            return result;
        } catch (WebClientResponseException | WebClientRequestException e) {
            throw handleError(e);
        } catch (RuntimeException e) {
            log.error("Unerwarteter Fehler beim Abrufen von Ticketmaster Events", e);
            throw new RuntimeException("Unbekannter Fehler bei Ticketmaster.");
        }
    }

    public Map<String, Object> fetchEventsWithPaging(Map<String, String> params) {
        Map<String, String> paramsWithLimit = new HashMap<>(params);
        paramsWithLimit.putIfAbsent("size", "10");
        try {
            URI uri = buildUri(paramsWithLimit);
            Mono<HashMap<String, Object>> responseMono = webClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.value() == 401,
                        resp -> resp.bodyToMono(String.class).map(msg -> new RuntimeException("Nicht autorisiert (401): Prüfe API-Key.")))
                    .onStatus(status -> status.value() == 429,
                        resp -> resp.bodyToMono(String.class).map(msg -> new RuntimeException("Rate-Limit erreicht (429): Bitte später erneut versuchen.")))
                    .onStatus(status -> status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class).map(msg -> new RuntimeException("Serverfehler bei Ticketmaster: " + msg)))
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<HashMap<String, Object>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .retryWhen(Retry.backoff(1, Duration.ofSeconds(2)).filter(ex -> ex instanceof WebClientResponseException && ((WebClientResponseException) ex).getStatusCode().value() == 429));
            HashMap<String, Object> body = responseMono.block();
            List<TicketmasterEventDTO> result = new ArrayList<>();
            int page = 0, size = 10, total = 0, totalPages = 1;
            if (body != null) {
                if (body.containsKey("_embedded")) {
                    Map<String, Object> embedded = (Map<String, Object>) body.get("_embedded");
                    if (embedded.containsKey("events")) {
                        List<Map<String, Object>> events = (List<Map<String, Object>>) embedded.get("events");
                        for (Map<String, Object> event : events) {
                            result.add(parseEvent(event));
                        }
                    }
                }
                if (body.containsKey("page")) {
                    Map<String, Object> pageObj = (Map<String, Object>) body.get("page");
                    page = ((Number) pageObj.getOrDefault("number", 0)).intValue();
                    size = ((Number) pageObj.getOrDefault("size", 10)).intValue();
                    total = ((Number) pageObj.getOrDefault("totalElements", result.size())).intValue();
                    totalPages = ((Number) pageObj.getOrDefault("totalPages", 1)).intValue();
                }
            }
            Map<String, Object> response = new HashMap<>();
            response.put("events", result);
            response.put("page", page);
            response.put("size", size);
            response.put("total", total);
            response.put("totalPages", totalPages);
            return response;
        } catch (WebClientResponseException | WebClientRequestException e) {
            throw handleError(e);
        } catch (RuntimeException e) {
            log.error("Unerwarteter Fehler beim Abrufen von Ticketmaster Events", e);
            throw new RuntimeException("Unbekannter Fehler bei Ticketmaster.");
        }
    }

    private URI buildUri(Map<String, String> paramsWithLimit) {
        org.springframework.web.util.UriComponentsBuilder builder = org.springframework.web.util.UriComponentsBuilder
                .fromHttpUrl("https://app.ticketmaster.com/discovery/v2/events.json")
                .queryParam("apikey", ticketmasterApiKey);
        // Paging
        if (paramsWithLimit.containsKey("size")) {
            builder.queryParam("size", paramsWithLimit.get("size"));
        }
        if (paramsWithLimit.containsKey("page")) {
            builder.queryParam("page", paramsWithLimit.get("page"));
        }
        // Sortierung
        if (paramsWithLimit.containsKey("sort")) {
            builder.queryParam("sort", paramsWithLimit.get("sort"));
        } else {
            builder.queryParam("sort", "date,asc");
        }
        // Filter: keyword, Zeitraum, Venue, Klassifikation, Stadt, latlong, radius, unit
        if (paramsWithLimit.containsKey("keyword")) {
            builder.queryParam("keyword", paramsWithLimit.get("keyword"));
        }
        if (paramsWithLimit.containsKey("startDateTime")) {
            builder.queryParam("startDateTime", paramsWithLimit.get("startDateTime"));
        }
        if (paramsWithLimit.containsKey("endDateTime")) {
            builder.queryParam("endDateTime", paramsWithLimit.get("endDateTime"));
        }
        if (paramsWithLimit.containsKey("venueId")) {
            builder.queryParam("venueId", paramsWithLimit.get("venueId"));
        }
        if (paramsWithLimit.containsKey("classificationName")) {
            builder.queryParam("classificationName", paramsWithLimit.get("classificationName"));
        }
        if (paramsWithLimit.containsKey("city")) {
            builder.queryParam("city", paramsWithLimit.get("city"));
        }
        if (paramsWithLimit.containsKey("latlong")) {
            builder.queryParam("latlong", paramsWithLimit.get("latlong"));
        }
        if (paramsWithLimit.containsKey("radius")) {
            builder.queryParam("radius", paramsWithLimit.get("radius"));
        }
        if (paramsWithLimit.containsKey("unit")) {
            builder.queryParam("unit", paramsWithLimit.get("unit"));
        }
        // Weitere Parameter dynamisch anhängen (außer den bereits behandelten)
        for (Map.Entry<String, String> entry : paramsWithLimit.entrySet()) {
            String key = entry.getKey();
            if (!List.of("apikey", "size", "page", "sort", "keyword", "startDateTime", "endDateTime", "venueId", "classificationName", "city", "latlong", "radius", "unit").contains(key)) {
                builder.queryParam(key, entry.getValue());
            }
        }
        return builder.build(true).toUri();
    }

    private TicketmasterEventDTO parseEvent(Map<String, Object> event) {
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
        return dto;
    }

    private RuntimeException handleError(Throwable e) {
        if (e instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) e;
            int code = ex.getStatusCode().value();
            if (code == 401) {
                return new RuntimeException("Nicht autorisiert (401): Prüfe API-Key.");
            } else if (code == 429) {
                return new RuntimeException("Rate-Limit erreicht (429): Bitte später erneut versuchen.");
            } else if (ex.getStatusCode().is5xxServerError()) {
                return new RuntimeException("Serverfehler bei Ticketmaster: " + ex.getMessage());
            } else {
                return new RuntimeException("Fehler bei Ticketmaster: " + ex.getMessage());
            }
        } else if (e instanceof WebClientRequestException) {
            return new RuntimeException("Verbindungsfehler zu Ticketmaster: " + e.getMessage());
        } else {
            log.error("Unbekannter Fehler bei Ticketmaster", e);
            return new RuntimeException("Unbekannter Fehler bei Ticketmaster.");
        }
    }
}
