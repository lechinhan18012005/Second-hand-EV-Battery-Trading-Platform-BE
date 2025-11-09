package com.evdealer.evdealermanagement.dto.post.verification;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.evdealer.evdealermanagement.entity.product.Product;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostVerifyResponse {

    // ========== Product info ==========
    private String id;
    private Product.Status status;
    private String rejectReason;
    private String title;
    private String thumbnail;
    private Product.ProductType productType;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;

    // ========== Model / Version ==========
    private String brandName;
    private String modelName;
    private String versionName;
    private String batteryType;

    // ========== Package / Fee ==========
    private String packageName;
    private BigDecimal amount;

    private String sellerId;
    private String sellerName;
    private String sellerPhone;

    private LocalDateTime featuredEndAt;
    private LocalDateTime expiresAt;
    private Integer featuredDays;
    private Integer postDays;
};