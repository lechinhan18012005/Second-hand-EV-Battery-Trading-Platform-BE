package com.evdealer.evdealermanagement.dto.vehicle.update;

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
public class VehicleUpdateProductRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title;

    @Size(max = 10_000, message = "Description must not exceed 10,000 characters")
    String description;

    @Positive(message = "Price must be greater than 0")
    @Digits(integer = 15, fraction = 2, message = "Price must be a valid number with up to 15 digits and 2 decimal places")
    BigDecimal price;

    @Size(max = 255, message = "City must not exceed 255 characters")
    String city;

    @Size(max = 255, message = "District must not exceed 255 characters")
    String district;

    @Size(max = 255, message = "Ward must not exceed 255 characters")
    String ward;

    @Size(max = 10_000, message = "Address detail must not exceed 10,000 characters")
    String addressDetail;

    String brandId;

    @Min(value = 0, message = "Battery health percent must be at least 0")
    @Max(value = 100, message = "Battery health percent cannot exceed 100")
    Short batteryHealthPercent;

    @PositiveOrZero(message = "Mileage must be 0 or greater")
    Integer mileageKm;

    String modelId;

    @Min(value = 1900, message = "Year must be greater than or equal to 1900")
    Short year;

    String versionId;

    String categoryId;
}
