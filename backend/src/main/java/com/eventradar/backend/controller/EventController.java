package com.eventradar.backend.controller;

import com.eventradar.backend.dto.TicketmasterEventDTO;
import com.eventradar.backend.service.TicketmasterEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import com.eventradar.backend.model.User;
import com.eventradar.backend.repository.UserRepository;
import com.eventradar.backend.service.GeocodingService;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/events")
public class EventController {
    @Autowired
    private TicketmasterEventService ticketmasterEventService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GeocodingService geocodingService;

    @GetMapping
    public ResponseEntity<List<TicketmasterEventDTO>> getEvents(@RequestParam Map<String, String> params, Authentication authentication) {
        // Wenn kein latlong gesetzt ist, nutze User-Location
        if (!params.containsKey("latlong") && authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null && user.getLocation() != null && !user.getLocation().isBlank()) {
                java.util.Optional<double[]> coordsOptional = geocodingService.geocode(user.getLocation());
                if (coordsOptional.isPresent()) {
                    double[] coords = coordsOptional.get();
                    String latlong = coords[0] + "," + coords[1];
                    params = new java.util.HashMap<>(params); // kopieren, falls unveränderlich
                    params.put("latlong", latlong);
                    params.put("radius", "50");
                    params.put("unit", "km");
                }
            }
        }
        List<TicketmasterEventDTO> events = ticketmasterEventService.fetchEvents(params);
        return ResponseEntity.ok(events);
    }
}
