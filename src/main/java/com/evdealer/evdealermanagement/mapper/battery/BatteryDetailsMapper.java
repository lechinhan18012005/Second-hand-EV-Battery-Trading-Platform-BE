package com.evdealer.evdealermanagement.mapper.battery;

import com.evdealer.evdealermanagement.dto.battery.BatteryDetailsDto;
import com.evdealer.evdealermanagement.dto.battery.brand.BatteryBrandsResponse;
import com.evdealer.evdealermanagement.dto.battery.detail.BatteryDetailResponse;
import com.evdealer.evdealermanagement.dto.post.battery.BatteryPostRequest;
import com.evdealer.evdealermanagement.dto.post.battery.BatteryPostResponse;
import com.evdealer.evdealermanagement.dto.post.common.ProductImageResponse;
import com.evdealer.evdealermanagement.entity.battery.BatteryBrands;
import com.evdealer.evdealermanagement.entity.battery.BatteryDetails;
import com.evdealer.evdealermanagement.entity.battery.BatteryTypes;
import com.evdealer.evdealermanagement.entity.product.Product;

import java.util.List;

public class BatteryDetailsMapper {

    public static BatteryDetailsDto toDto(BatteryDetails entity) {
        if (entity == null)
            return null;

        return BatteryDetailsDto.builder()
                .productId(entity.getProductId() != null ? entity.getProductId() : null)
                .batteryTypeId(entity.getBatteryType() != null ? entity.getBatteryType().getId() : null)
                .batteryTypeName(entity.getBatteryType() != null ? entity.getBatteryType().getName() : null)
                .brandId(entity.getBrand() != null ? entity.getBrand().getId() : null)
                .brandName(entity.getBrand() != null ? entity.getBrand().getName() : null)
                .capacityKwh(entity.getCapacityKwh())
                .healthPercent(entity.getHealthPercent())
                .build();
    }

    // DTO -> Entity (chỉ map data cơ bản, service sẽ set quan hệ)
    public static BatteryDetails toEntity(BatteryDetailsDto dto) {
        if (dto == null)
            return null;

        BatteryDetails entity = new BatteryDetails();

        if (dto.getProductId() != null) {
            entity.setProductId(dto.getProductId());
        }

        entity.setCapacityKwh(dto.getCapacityKwh());
        entity.setHealthPercent(dto.getHealthPercent());

        return entity;
    }

    public static void setBatteryType(BatteryDetails entity, BatteryTypes batteryType) {
        entity.setBatteryType(batteryType);
    }

    public static void setBrand(BatteryDetails entity, BatteryBrands brand) {
        entity.setBrand(brand);
    }

    public static BatteryDetailResponse toBatteryDetailResponse(BatteryDetails details) {
        if (details == null) return null;

        Product p = details.getProduct();
        BatteryBrandsResponse brandDto = BatteryMapper.mapToBatteryBrandsResponse(details.getBrand());

        return BatteryDetailResponse.builder()
                .productTitle(p != null ? p.getTitle() : null)
                .productPrice(p != null ? p.getPrice() : null)
                .productStatus(p != null && p.getStatus() != null ? p.getStatus().name() : null)
                .brandName(brandDto != null ? brandDto.getBrandName() : null)
                .brandLogoUrl(brandDto != null ? brandDto.getLogoUrl() : null)
                .batteryTypeName(details.getBatteryType() != null ? details.getBatteryType().getName() : null)
                .capacityKwh(details.getCapacityKwh())
                .voltageV(details.getVoltageV())
                .healthPercent(details.getHealthPercent())
                .origin(details.getOrigin())
                .build();
    }

    public static BatteryPostResponse toBatteryPostResponse(Product product,
                                                            BatteryDetails details,
                                                            BatteryPostRequest request,
                                                            List<ProductImageResponse> uploadedImages) {
        if (product == null || details == null) return null;

        List<ProductImageResponse> finalImages;
        if (uploadedImages != null && !uploadedImages.isEmpty()) {
            finalImages = uploadedImages;
        } else {
            finalImages = product.getImages().stream()
                    .map(img -> ProductImageResponse.builder()
                            .id(img.getId())
                            .url(img.getImageUrl())
                            .width(img.getWidth())
                            .height(img.getHeight())
                            .position(img.getPosition())
                            .isPrimary(img.getIsPrimary())
                            .build())
                    .toList();
        }

        // Build response
        return BatteryPostResponse.builder()
                .productId(product.getId())
                .status(product.getStatus().name())
                .sellerPhone(product.getSellerPhone())

                .title(request != null ? request.getTitle() : product.getTitle())
                .description(request != null ? request.getDescription() : product.getDescription())
                .price(request != null ? request.getPrice() : product.getPrice())
                .city(request != null ? request.getCity() : product.getCity())
                .district(request != null ? request.getDistrict() : product.getDistrict())
                .ward(request != null ? request.getWard() : product.getWard())
                .addressDetail(request != null ? request.getAddressDetail() : product.getAddressDetail())

                .brandId(request != null ? request.getBrandId() : (details.getBrand() != null ? details.getBrand().getId() : null))
                .brandName(details.getBrand() != null ? details.getBrand().getName() : null)
                .batteryTypeName(details.getBatteryType() != null ? details.getBatteryType().getName() : null)
                .capacityKwh(details.getCapacityKwh())
                .healthPercent(details.getHealthPercent())
                .voltageV(details.getVoltageV())

                .createdAt(product.getCreatedAt())
                .images(finalImages)
                .build();
    }
}
