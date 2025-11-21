package com.evdealer.evdealermanagement.dto.post.vehicle;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VehiclePostRequest {

    @NotBlank(message = "Please enter a title")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title;

    @NotBlank(message = "Please enter a description")
    @Size(max = 10_000, message = "Description must not exceed 10,000 characters")
    String description;

    @NotNull(message = "Please enter the price")
    @Positive(message = "Price must be greater than 0")
    @Digits(integer = 15, fraction = 2, message = "Price must be a valid number with up to 15 digits and 2 decimal places")
    BigDecimal price;

    @NotBlank(message = "Please enter the city")
    @Size(max = 255, message = "City must not exceed 255 characters")
    String city;

    @NotBlank(message = "Please enter the district")
    @Size(max = 255, message = "District must not exceed 255 characters")
    String district;

    @NotBlank(message = "Please enter the ward")
    @Size(max = 255, message = "Ward must not exceed 255 characters")
    String ward;

    @NotBlank(message = "Please enter the address detail")
    @Size(max = 10_000, message = "Address detail must not exceed 10,000 characters")
    String addressDetail;

    @NotBlank(message = "Please select the brand")
    String brandId;

    @NotNull(message = "Please enter the battery health percentage")
    @Min(value = 0, message = "Battery health percent must be at least 0")
    @Max(value = 100, message = "Battery health percent cannot exceed 100")
    Short batteryHealthPercent;

    @NotNull(message = "Please enter the mileage")
    @PositiveOrZero(message = "Mileage must be 0 or greater")
    Integer mileageKm;

    String modelId;

    @NotNull(message = "Please enter the manufacturing year")
    @Min(value = 1900, message = "Year must be greater than or equal to 1900")
    Short year;

    String versionId;

    @NotBlank(message = "Category ID is required")
    String categoryId;

}
