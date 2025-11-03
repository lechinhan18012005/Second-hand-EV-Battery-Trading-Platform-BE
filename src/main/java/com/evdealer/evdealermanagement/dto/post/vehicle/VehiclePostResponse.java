package com.evdealer.evdealermanagement.dto.post.vehicle;


import com.evdealer.evdealermanagement.dto.post.common.ProductImageResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class VehiclePostResponse {

    String productId;
    String status;
    String title;
    String description;
    BigDecimal price;
    String sellerPhone;
    String city;
    String district;
    String ward;
    String addressDetail;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt;
    String categoryId;
    String brandId;
    String brandName;
    String categoryName;
    Short batteryHealthPercent;
    Integer mileageKm;
    String modelName;
    Byte warrantyMonths;
    Boolean hasInsurance;
    Boolean hasRegistration;
    List<ProductImageResponse> images;
}
