package com.evdealer.evdealermanagement.dto.product.renewal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.evdealer.evdealermanagement.entity.product.Product;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductRenewalResponse {
    String productId;
    Product.Status status;
    BigDecimal totalPayable;
    String currency;
    String paymentUrl;
    LocalDateTime updatedAt;
    LocalDateTime startRenewalAt;
}
