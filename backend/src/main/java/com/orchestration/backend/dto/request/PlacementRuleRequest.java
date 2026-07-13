package com.orchestration.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PlacementRuleRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    @NotBlank(message = "La politique est obligatoire")
    @Pattern(
            regexp = "least_loaded|lowest_latency|balanced",
            message = "La politique doit etre least_loaded, lowest_latency ou balanced"
    )
    private String policy;

    private Boolean isActive = true;
}