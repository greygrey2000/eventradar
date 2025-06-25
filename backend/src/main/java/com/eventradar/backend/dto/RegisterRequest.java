package com.eventradar.backend.dto;

import java.util.List;
import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String name;
    private String location;
    private List<String> interests;
}