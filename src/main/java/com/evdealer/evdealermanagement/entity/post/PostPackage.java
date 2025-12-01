package com.evdealer.evdealermanagement.entity.post;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "post_packages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostPackage {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(length = 50, unique = true)
    String code; // Mã định danh ổn định (STANDARD / PRIORITY / SPECIAL)

    @Column(length = 100, nullable = false)
    String name;

    @Column(length = 255)
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", nullable = false, columnDefinition = "ENUM('FIXED','PER_DAY') default 'FIXED'")
    BillingMode billingMode; // FIXED / PER_DAY

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, columnDefinition = "ENUM('BASE','ADDON') default 'BASE'")
    Category category; // BASE / ADDON

    @Column(name = "duration_days")
    Integer durationDays;

    @Column(name = "base_duration_days")
    Integer baseDurationDays;

    @Column(precision = 10, scale = 2, nullable = false)
    BigDecimal price; // giá cố định (VD: 10,000 / 30 ngày)

    @Column(name = "daily_price", precision = 10, scale = 2)
    BigDecimal dailyPrice; // giá theo ngày (VD: 20,000/ngày)

    @Column(name = "includes_post_fee", nullable = false)
    Boolean includesPostFee; // 1: đã bao gồm phí đăng tin

    @Column(name = "priority_level", nullable = false)
    Integer priorityLevel; // 0 = thường, 1 = ưu tiên, 2 = đặc biệt

    @Column(name = "badge_label", length = 20)
    String badgeLabel; // VD: "HOT"

    @Column(name = "show_in_latest", nullable = false)
    Boolean showInLatest; // hiển thị trong mục "tin mới nhất"

    @Column(name = "show_top_search", nullable = false)
    Boolean showTopSearch; // hiển thị top tìm kiếm

    @Column(name = "list_price", precision = 10, scale = 2)
    BigDecimal listPrice; // giá gốc (để gạch, nếu có khuyến mãi)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('ACTIVE','INACTIVE') default 'ACTIVE'")
    Status status;

    @Column(name = "is_default", nullable = false)
    Boolean isDefault; // gói mặc định được tick sẵn

    @Column(name = "sort_order", nullable = false)
    Integer sortOrder; // thứ tự hiển thị trên UI

    @Column(name = "created_at", updatable = false)
    Instant createdAt;

    @Column(name = "updated_at")
    Instant updatedAt;


    // --- ENUMS ---
    public enum BillingMode {
        FIXED, PER_DAY
    }

    public enum Category {
        BASE, ADDON
    }

    public enum Status {
        ACTIVE, INACTIVE
    }
}
