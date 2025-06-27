package com.eventradar.backend.dto;

import java.util.List;

public class UserProfileDTO {
    private String name;
    private String email;
    private String location;
    private List<String> interests;

    public UserProfileDTO(String name, String email, String location, List<String> interests) {
        this.name = name;
        this.email = email;
        this.location = location;
        this.interests = interests;
    }
    
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getLocation() { return location; }
    public List<String> getInterests() { return interests; }
}
