package com.evdealer.evdealermanagement.dto.product.status;

import java.time.LocalDateTime;

import com.evdealer.evdealermanagement.entity.product.Product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductActiveOrHiddenResponse {
    private String productId;
    private Product.Status status;
    private String message;
    private LocalDateTime updatedAt;
}
