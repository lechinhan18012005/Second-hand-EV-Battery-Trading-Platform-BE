package com.evdealer.evdealermanagement.mapper.post;

import com.evdealer.evdealermanagement.dto.post.verification.PostVerifyResponse;
import com.evdealer.evdealermanagement.entity.post.PostPayment;
import com.evdealer.evdealermanagement.entity.product.Product;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PostVerifyMapper {

    public static PostVerifyResponse toResponse(Product product, PostPayment payment) {
        if (product == null) {
            return null;
        }

        PostVerifyResponse.PostVerifyResponseBuilder builder = PostVerifyResponse.builder()
                // Basic info
                .id(product.getId()).status(product.getStatus()).rejectReason(product.getRejectReason())
                .title(product.getTitle()).description(product.getDescription()).images(product.getImages())
                .productType(product.getType()).createdAt(product.getCreatedAt()).updateAt(product.getUpdatedAt())

                // Price & Location
                .price(product.getPrice()).city(product.getCity()).district(product.getDistrict())
                .ward(product.getWard()).addressDetail(product.getAddressDetail())

                // Dates
                .featuredEndAt(product.getFeaturedEndAt()).expiresAt(product.getExpiresAt())

                // Seller info
                .sellerId(product.getSeller() != null ? product.getSeller().getId() : null)
                .sellerName(product.getSeller() != null ? product.getSeller().getFullName() : null)
                .sellerPhone(product.getSeller() != null ? product.getSeller().getPhone() : null)
                .sellerEmail(product.getSeller() != null ? product.getSeller().getEmail() : null);

        // Brand/Model/Version (common for both types)
        switch (product.getType()) {
            case VEHICLE -> {
                if (product.getVehicleDetails() != null && product.getVehicleDetails().getBrand() != null) {
                    builder.brandId(product.getVehicleDetails().getBrand().getId())
                            .brandName(product.getVehicleDetails().getBrand().getName());
                }
            }
            case BATTERY -> {
                if (product.getBatteryDetails() != null && product.getBatteryDetails().getBrand() != null) {
                    builder.brandId(product.getBatteryDetails().getBrand().getId())
                            .brandName(product.getBatteryDetails().getBrand().getName());
                }
            }
            default -> {
            }
        }

        Integer postDays = null;

        if (payment.getPostPackage() != null) {
            Integer base = payment.getPostPackage().getBaseDurationDays();
            postDays = (base != null) ? base : 30;
        }

        // Payment info
        if (payment != null && payment.getPostPackage() != null) {
            builder.packageName(payment.getPostPackage().getName())
                    .packageCode(payment.getPostPackage().getCode())
                    .amount(payment.getAmount())
                    .featuredDays(
                            payment.getPostPackageOption() != null
                                    ? payment.getPostPackageOption().getDurationDays()
                                    : null)
                    .postDays(postDays);
        }

        log.info("DEBUG payment: id={}, hasOption={}, hasPackage={}, baseDurationDays={}",
                payment.getId(),
                payment.getPostPackageOption() != null,
                payment.getPostPackage() != null,
                payment.getPostPackage() != null
                        ? payment.getPostPackage().getBaseDurationDays()
                        : null);

        // ========== Type-specific fields ==========
        switch (product.getType()) {
            case VEHICLE -> {
                // Vehicle-specific
                if (product.getVehicleDetails().getModel() != null) {
                    builder.modelId(product.getVehicleDetails().getModel().getId())
                            .modelName(product.getVehicleDetails().getModel().getName());
                }
                if (product.getVehicleDetails().getVersion() != null) {
                    builder.versionId(product.getVehicleDetails().getVersion().getId())
                            .versionName(product.getVehicleDetails().getVersion().getName());
                }
                if (product.getVehicleDetails().getCategory() != null) {
                    builder.categoryId(product.getVehicleDetails().getCategory().getId())
                            .categoryName(product.getVehicleDetails().getCategory().getName());
                }
                builder.batteryHealthPercent(product.getVehicleDetails().getBatteryHealthPercent())
                        .mileageKm(product.getVehicleDetails().getMileageKm()).year(product.getManufactureYear());
            }

            case BATTERY -> {
                // Battery-specific
                if (product.getBatteryDetails().getBatteryType() != null) {
                    builder.batteryTypeId(product.getBatteryDetails().getBatteryType().getId())
                            .batteryType(product.getBatteryDetails().getBatteryType().getName());
                }
                builder.healthPercent(product.getBatteryDetails().getHealthPercent())
                        .capacityKwh(product.getBatteryDetails().getCapacityKwh())
                        .voltageV(product.getBatteryDetails().getVoltageV());
            }

            default -> {
                // Handle other types if needed
            }
        }

        return builder.build();
    }

    /**
     * Overload method if you don't have payment info
     */
    public static PostVerifyResponse toResponse(Product product) {
        return toResponse(product, null);
    }
}