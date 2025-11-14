package com.evdealer.evdealermanagement.mapper.product;

import com.evdealer.evdealermanagement.dto.post.common.ProductImageResponse;
import com.evdealer.evdealermanagement.dto.product.detail.ProductDetail;
import com.evdealer.evdealermanagement.dto.product.detail.ProductImageDto;
import com.evdealer.evdealermanagement.dto.product.show.ProductResponse;
import com.evdealer.evdealermanagement.entity.battery.BatteryDetails;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.entity.product.ProductImages;
import com.evdealer.evdealermanagement.entity.vehicle.VehicleDetails;
import com.evdealer.evdealermanagement.service.implement.SellerReviewService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class ProductMapper {


    // Entity -> DTO
    public static ProductDetail toDetailDto(Product product) {
        if (product == null)
            return null;

        // Initialize images để tránh lazy loading exception
        Hibernate.initialize(product.getImages());

        // FIX: Map ProductImages sang ProductImageDto
        List<ProductImageDto> imagesList = Collections.emptyList();
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imagesList = product.getImages().stream()
                    .sorted(Comparator.comparing(
                            ProductImages::getPosition,
                            Comparator.nullsLast(Integer::compareTo)))
                    .map(ProductImageDto::fromEntity) // Convert sang DTO
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
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .expiresAt(product.getExpiresAt())
                .featuredEndAt(product.getFeaturedEndAt())

                // Seller info
                .sellerId(product.getSeller() != null ? product.getSeller().getId() : null)
                .sellerName(product.getSeller() != null ? product.getSeller().getFullName() : null)
                .sellerPhone(product.getSeller() != null ? product.getSeller().getPhone() : null)

                // Address info
                .addressDetail(product.getAddressDetail())
                .city(product.getCity())
                .district(product.getDistrict())
                .ward(product.getWard())

                // Product images - Sử dụng biến đã convert
                .productImagesList(imagesList)
                .brandName(brandName)
                .modelName(modelName)
                .version(version)
                .batteryType(batteryType)
                .isHot(product.getIsHot() != null ? product.getIsHot() : false)
                .build();
    }

    public static ProductImageResponse toMapDto(ProductImages productImages) {
        if (productImages == null)
            return null;

        return ProductImageResponse.builder()
                .id(productImages.getId())
                .url(productImages.getImageUrl())
                .width(productImages.getWidth())
                .height(productImages.getHeight())
                .position(productImages.getPosition())
                .isPrimary(productImages.getIsPrimary())
                .build();
    }

    // DTO -> Entity
    public static Product toEntity(ProductDetail dto) {
        if (dto == null)
            return null;

        log.debug("Mapping ProductDetail to Product: {}", dto);

        // Validate UUID
        if (dto.getId() != null) {
            try {
                UUID.fromString(dto.getId());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid UUID format for id: " + dto.getId());
            }
        }

        Product.ProductType productType = null;
        if (dto.getType() != null) {
            try {
                productType = Product.ProductType.valueOf(dto.getType());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid ProductType: " + dto.getType());
            }
        }

        Product.Status status = null;
        if (dto.getStatus() != null) {
            try {
                status = Product.Status.valueOf(dto.getStatus());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid Status: " + dto.getStatus());
            }
        }

        Product.ConditionType conditionType = null;
        if (dto.getConditionType() != null) {
            try {
                conditionType = Product.ConditionType.valueOf(dto.getConditionType());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid ConditionType: " + dto.getConditionType());
            }
        }

        return Product.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .type(productType)
                .price(dto.getPrice())
                .status(status)
                .conditionType(conditionType != null ? conditionType : Product.ConditionType.USED)
                .createdAt(dto.getCreatedAt())
                // ✅ Address info khi map ngược
                .addressDetail(dto.getAddressDetail())
                .city(dto.getCity())
                .district(dto.getDistrict())
                .ward(dto.getWard())
                .isHot(dto.getIsHot() != null ? dto.getIsHot() : false)
                .build();
    }

    public static ProductResponse mapToProductResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .type(p.getType())
                .price(p.getPrice())
                .manufactureYear(p.getManufactureYear())
                .conditionType(p.getConditionType())
                .sellerId(p.getSeller() != null ? p.getSeller().getId() : null)
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .addressDetail(p.getAddressDetail())
                .city(p.getCity())
                .district(p.getDistrict())
                .ward(p.getWard())
                .sellerPhone(p.getSellerPhone())
                .saleType(p.getSaleType())
                .postingFee(p.getPostingFee())
                .rejectReason(p.getRejectReason())
                .expiresAt(p.getExpiresAt())
                .featuredEndAt(p.getFeaturedEndAt())
                .approvedBy(p.getApprovedBy() != null ? p.getApprovedBy().getId() : null)
                .isHot(p.getIsHot())
                .remindBefore2Sent(p.isRemindBefore2Sent())
                .build();
    }
}