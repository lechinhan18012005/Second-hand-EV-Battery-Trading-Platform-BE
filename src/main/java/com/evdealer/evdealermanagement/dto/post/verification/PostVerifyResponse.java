package com.evdealer.evdealermanagement.dto.post.verification;

import com.evdealer.evdealermanagement.entity.product.ProductImages;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.evdealer.evdealermanagement.entity.product.Product;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostVerifyResponse {

    // ========== Basic Product Info ==========
    private String id;
    private Product.Status status;
    private String rejectReason;
    private String title;
    private String description;
    private List<ProductImages> images;
    private Product.ProductType productType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========== Price & Location (Common) ==========
    private BigDecimal price;
    private String city;
    private String district;
    private String ward;
    private String addressDetail;

    // ========== Brand / Model / Version (Common) ==========
    private String brandId;
    private String brandName;
    private String modelId;
    private String modelName;
    private String versionId;
    private String versionName;

    // ========== Battery Fields (Common for both types) ==========
    private Integer healthPercent; // For battery post
    private Short batteryHealthPercent; // For vehicle post

    // ========== Battery-Specific Fields ==========
    private String batteryTypeId;
    private String batteryType;
    private BigDecimal capacityKwh;
    private Integer voltageV;

    // ========== Vehicle-Specific Fields ==========
    private Integer mileageKm;
    private Short year;
    private String categoryId;
    private String categoryName;

    // ========== Package / Payment Info ==========
    private String packageName;
    private String packageCode;
    private BigDecimal amount;
    private Integer featuredDays;
    private Integer postDays;
    private LocalDateTime featuredEndAt;
    private LocalDateTime expiresAt;

    // ========== Seller Info ==========
    private String sellerId;
    private String sellerName;
    private String sellerPhone;
    private String sellerEmail;
}