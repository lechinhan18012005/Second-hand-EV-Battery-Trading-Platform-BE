package com.evdealer.evdealermanagement.mapper.vehicle;

import com.evdealer.evdealermanagement.dto.post.common.ProductImageResponse;
import com.evdealer.evdealermanagement.dto.post.vehicle.VehiclePostRequest;
import com.evdealer.evdealermanagement.dto.post.vehicle.VehiclePostResponse;
import com.evdealer.evdealermanagement.dto.vehicle.brand.VehicleBrandsResponse;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.entity.vehicle.VehicleBrands;
import com.evdealer.evdealermanagement.entity.vehicle.VehicleDetails;

import java.util.List;
import java.util.stream.Collectors;

public class VehicleMapper {
    public static VehicleBrandsResponse mapToVehicleBrandsResponse(VehicleBrands e) {
        return VehicleBrandsResponse.builder()
                .brandId(e.getId())
                .brandName(e.getName())
                .logoUrl(e.getLogoUrl())
                .build();
    }

    public static VehiclePostResponse toVehiclePostResponse(Product product,
                                                            VehicleDetails details,
                                                            VehiclePostRequest request,
                                                            List<ProductImageResponse> uploadedImages) {
        if (product == null || details == null) return null;

        // Ảnh: ưu tiên ảnh mới upload, nếu không có thì dùng ảnh cũ
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

        return VehiclePostResponse.builder()
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
                .categoryId(request != null ? request.getCategoryId() : (details.getCategory() != null ? details.getCategory().getId() : null))
                .categoryName(details.getCategory() != null ? details.getCategory().getName() : null)
                .modelName(details.getModel() != null ? details.getModel().getName() : null)
                .batteryHealthPercent(details.getBatteryHealthPercent())
                .mileageKm(details.getMileageKm())
                .hasInsurance(details.getHasInsurance())
                .hasRegistration(details.getHasRegistration())
                .warrantyMonths(details.getWarrantyMonths())

                .createdAt(product.getCreatedAt())
                .images(finalImages)
                .build();
    }
}
