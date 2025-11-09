package com.evdealer.evdealermanagement.dto.battery.type;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBatteryTypeRequest {
    @NotBlank(message = "Battery type name is required")
    private String name;
}
