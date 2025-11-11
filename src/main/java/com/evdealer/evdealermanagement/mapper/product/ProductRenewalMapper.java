package com.evdealer.evdealermanagement.mapper.product;

import java.math.BigDecimal;

import com.evdealer.evdealermanagement.dto.product.renewal.ProductRenewalResponse;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;

public class ProductRenewalMapper {

    private ProductRenewalMapper() {
        // chặn khởi tạo
    }

    public static ProductRenewalResponse mapToProductRenewalResponse(Product product,
            BigDecimal totalPayable,
            String paymentUrl) {
        if (product == null) {
            return null;
        }

        return ProductRenewalResponse.builder()
                .productId(product.getId())
                .status(product.getStatus())
                .totalPayable(totalPayable)
                .currency("VND")
                .paymentUrl(paymentUrl)
                .updatedAt(product.getUpdatedAt())
                .startRenewalAt(product.getStartRenewalAt())
                .build();
    }
}
