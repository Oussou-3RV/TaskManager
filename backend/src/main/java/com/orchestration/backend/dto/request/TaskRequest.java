package com.orchestration.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaskRequest {

    @NotNull(message = "Le CPU requis est obligatoire")
    @DecimalMin(value = "0.1", message = "Le CPU doit etre superieur a 0.1")
    private BigDecimal cpuRequired;

    @NotNull(message = "La memoire requise est obligatoire")
    @Min(value = 1, message = "La memoire doit etre superieure a 0")
    private Integer memoryRequiredMb;

    @Pattern(regexp = "low|normal|high", message = "La priorite doit etre low, normal ou high")
    private String priority = "normal";
}