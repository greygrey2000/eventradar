package com.eventradar.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class TicketmasterEventDTO {
    private String id;
    private String name;
    private String date;
    private String imageUrl;
    private String url;
    private String venue;
    private List<String> classifications;
}
