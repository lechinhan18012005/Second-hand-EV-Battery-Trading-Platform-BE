package com.evdealer.evdealermanagement.dto.product.detail;

import com.evdealer.evdealermanagement.entity.battery.BatteryDetails;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.entity.product.ProductImages;
import com.evdealer.evdealermanagement.entity.vehicle.VehicleDetails;
import com.evdealer.evdealermanagement.utils.PriceSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetail {

    private String id;
    private String title;
    private String description;
    private String type;
    private List<ProductImageDto> productImagesList;

    @JsonSerialize(using = PriceSerializer.class)
    private BigDecimal price;
    private String conditionType;

    private String sellerId;
    private String sellerName;
    private String sellerPhone;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime featuredEndAt;

    private String addressDetail;
    private String city;
    private String district;
    private String ward;

    private String brandName;
    private String modelName;
    private String version;
    private String batteryType;
    private String rejectReason;

    private Boolean isHot;

    private Boolean isWishlisted;

    private String sellerAvatarUrl;

    private Boolean hasReview;

    public static ProductDetail fromEntity(Product product) {
        if (product == null)
            return null;

        Hibernate.initialize(product.getImages());

        List<ProductImageDto> imagesList = Collections.emptyList();
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imagesList = product.getImages().stream()
                    .sorted(Comparator.comparing(
                            ProductImages::getPosition,
                            Comparator.nullsLast(Integer::compareTo)))
                    .map(ProductImageDto::fromEntity)
                    .collect(Collectors.toList());
        }

        String brandName = null;
        String modelName = null;
        String version = null;
        String batteryType = null;

        if (product.getType() == Product.ProductType.VEHICLE) {

            Hibernate.initialize(product.getVehicleDetails());

            VehicleDetails vehicleDetails = product.getVehicleDetails();

            if (vehicleDetails != null && vehicleDetails.getVersion() != null) {
                version = vehicleDetails.getVersion().getName();

                if (vehicleDetails.getBrand() != null && vehicleDetails.getBrand().getName() != null) {
                    brandName = vehicleDetails.getBrand().getName();
                }
                if (vehicleDetails.getModel() != null && vehicleDetails.getModel().getName() != null) {
                    modelName = vehicleDetails.getModel().getName();
                }
            }
        } else if (product.getType() == Product.ProductType.BATTERY) {

            Hibernate.initialize(product.getBatteryDetails());

            BatteryDetails batteryDetails = product.getBatteryDetails();

            if (batteryDetails != null && batteryDetails.getBrand() != null) {

                brandName = batteryDetails.getBrand().getName();

                if (batteryDetails.getBatteryType() != null) {
                    batteryType = batteryDetails.getBatteryType().getName();
                }
            }
        }

        return ProductDetail.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .type(product.getType() != null ? product.getType().name() : null)
                .price(product.getPrice())
                .conditionType(product.getConditionType() != null ? product.getConditionType().name() : null)
                .sellerId(product.getSeller() != null ? product.getSeller().getId() : null)
                .sellerName(product.getSeller() != null ? product.getSeller().getFullName() : null)
                .sellerPhone(product.getSeller() != null ? product.getSeller().getPhone() : null)
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .createdAt(product.getCreatedAt())
                .addressDetail(product.getAddressDetail())
                .city(product.getCity())
                .district(product.getDistrict())
                .ward(product.getWard())
                .productImagesList(imagesList) // Dùng biến đã được infer đúng type
                .modelName(modelName)
                .version(version)
                .brandName(brandName)
                .batteryType(batteryType)
                .rejectReason(product.getRejectReason())
                .isHot(product.getIsHot() != null ? product.getIsHot() : false)
                .sellerAvatarUrl(
                        product.getSeller() != null && product.getSeller().getAvatarUrl() != null
                                ? product.getSeller().getAvatarUrl()
                                : null)
                .build();
    }
}