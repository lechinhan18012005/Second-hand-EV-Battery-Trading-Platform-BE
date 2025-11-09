package com.evdealer.evdealermanagement.mapper.post;

import org.hibernate.Hibernate;

import com.evdealer.evdealermanagement.dto.post.verification.PostVerifyResponse;
import com.evdealer.evdealermanagement.entity.battery.BatteryDetails;
import com.evdealer.evdealermanagement.entity.post.PostPayment;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.entity.vehicle.VehicleDetails;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class PostVerifyMapper {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private PostVerifyMapper() {
    }

    public static PostVerifyResponse mapToPostVerifyResponse(Product p, PostPayment payment) {
        if (p == null)
            return null;

        // Thumbnail: lấy ảnh đầu tiên nếu có
        String thumbnail = null;
        if (p.getImages() != null && !p.getImages().isEmpty() && p.getImages().get(0) != null) {
            thumbnail = p.getImages().get(0).getImageUrl();
        }

        String brandName = null;
        String modelName = null;
        String version = null;
        String batteryType = null;

        if (p.getType() == Product.ProductType.VEHICLE) {
            Hibernate.initialize(p.getVehicleDetails());
            VehicleDetails vehicleDetails = p.getVehicleDetails();

            if (vehicleDetails != null && vehicleDetails.getVersion() != null) {
                version = vehicleDetails.getVersion().getName();

                if (vehicleDetails.getBrand() != null && vehicleDetails.getBrand().getName() != null) {
                    brandName = vehicleDetails.getBrand().getName();
                }
                if (vehicleDetails.getModel() != null && vehicleDetails.getModel().getName() != null) {
                    modelName = vehicleDetails.getModel().getName();
                }
            }
        } else if (p.getType() == Product.ProductType.BATTERY) {
            Hibernate.initialize(p.getBatteryDetails());
            BatteryDetails batteryDetails = p.getBatteryDetails();

            if (batteryDetails != null && batteryDetails.getBrand() != null) {
                brandName = batteryDetails.getBrand().getName();

                if (batteryDetails.getBatteryType() != null) {
                    batteryType = batteryDetails.getBatteryType().getName();
                }
            }
        }

        // Xử lý featuredEndAt và expiresAt
        LocalDateTime featuredEndAt = p.getFeaturedEndAt();
        LocalDateTime expiresAt = p.getExpiresAt();

        // Nếu product chưa được duyệt (PENDING_REVIEW), tính toán thời gian dự kiến
        if (p.getStatus() == Product.Status.PENDING_REVIEW && payment != null) {
            LocalDateTime now = ZonedDateTime.now(VIETNAM_ZONE).toLocalDateTime();

            // Tính thời gian featured dự kiến
            if (payment.getPostPackageOption() != null &&
                    payment.getPostPackageOption().getDurationDays() != null) {
                int elevatedDays = payment.getPostPackageOption().getDurationDays();
                if (elevatedDays > 0) {
                    featuredEndAt = now.plusDays(elevatedDays);
                }
            }

            // Thời gian hết hạn dự kiến (30 ngày)
            expiresAt = now.plusDays(30);
        }

        // ====== NEW: Tính featuredDays & postDays ======
        Integer featuredDays = null;
        Integer postDays = null;

        if (p.getStatus() == Product.Status.PENDING_REVIEW && payment != null) {

            // Lấy trực tiếp từ package option + rule 30 ngày
            if (payment.getPostPackageOption() != null &&
                    payment.getPostPackageOption().getDurationDays() != null) {
                featuredDays = payment.getPostPackageOption().getDurationDays();
            }
            postDays = 30;
        } else {
            // Đã duyệt/đang chạy: trả số ngày còn lại (không âm)
            LocalDateTime now = ZonedDateTime.now(VIETNAM_ZONE).toLocalDateTime();
            if (featuredEndAt != null) {
                long d = ChronoUnit.DAYS.between(now, featuredEndAt);
                featuredDays = (int) Math.max(d, 0);
            }
            if (expiresAt != null) {
                long d = ChronoUnit.DAYS.between(now, expiresAt);
                postDays = (int) Math.max(d, 0);
            }
        }
        // ===============================================

        return PostVerifyResponse.builder()
                .id(p.getId())
                .status(p.getStatus())
                .rejectReason(p.getRejectReason())
                .title(p.getTitle())
                .thumbnail(thumbnail)
                .productType(p.getType())
                .sellerName(p.getSeller() != null ? p.getSeller().getFullName() : null)
                .sellerId(p.getSeller() != null ? p.getSeller().getId() : null)
                .sellerPhone(p.getSellerPhone())
                .createdAt(p.getCreatedAt())
                .updateAt(p.getUpdatedAt())
                .brandName(brandName)
                .batteryType(batteryType)
                .modelName(modelName)
                .versionName(version)
                .featuredEndAt(featuredEndAt)
                .expiresAt(expiresAt)
                .postDays(postDays)
                .featuredDays(featuredDays)
                .packageName(payment != null && payment.getPostPackage() != null
                        ? payment.getPostPackage().getName()
                        : null)
                .amount(payment != null ? payment.getAmount() : null)
                .build();
    }

}