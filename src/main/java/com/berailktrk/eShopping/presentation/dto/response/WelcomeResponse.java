package com.berailktrk.eShopping.presentation.dto.response;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Welcome response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeResponse {
    
    private String application;
    private String version;
    private String status;
    private String message;
    private Map<String, String> endpoints;
}
