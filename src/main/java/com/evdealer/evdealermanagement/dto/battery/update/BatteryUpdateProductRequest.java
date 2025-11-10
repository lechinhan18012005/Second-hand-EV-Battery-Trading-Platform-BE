package com.evdealer.evdealermanagement.dto.battery.update;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatteryUpdateProductRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 10_000, message = "Description must not exceed 10,000 characters")
    private String description;

    @Positive(message = "Price must be greater than 0")
    @Digits(integer = 15, fraction = 2, message = "Price must have at most 15 integer digits and 2 decimal places")
    private BigDecimal price;

    @Size(max = 255, message = "City must not exceed 255 characters")
    private String city;

    @Size(max = 255, message = "District must not exceed 255 characters")
    private String district;

    @Size(max = 255, message = "Ward must not exceed 255 characters")
    private String ward;

    @Size(max = 10_000, message = "Address detail must not exceed 10,000 characters")
    private String addressDetail;

    private String batteryTypeId;

    private String brandId;

    @Positive(message = "Capacity must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Capacity must have at most 10 integer digits and 2 decimal places")
    private BigDecimal capacityKwh;

    @Min(value = 0, message = "Health percent must be at least 0")
    @Max(value = 100, message = "Health percent cannot exceed 100")
    private Integer healthPercent;
    
    @Positive(message = "Voltage must be greater than 0")
    private Integer voltageV;
}
