package com.evdealer.evdealermanagement.entity.product;

import com.evdealer.evdealermanagement.entity.BaseEntity;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.entity.battery.BatteryDetails;
import com.evdealer.evdealermanagement.entity.post.PostPayment;
import com.evdealer.evdealermanagement.entity.vehicle.VehicleDetails;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Product extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ProductType type;

    @Column(precision = 15, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 10)
    private ConditionType conditionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id", nullable = false)
    private Account seller;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "address_detail", columnDefinition = "TEXT")
    private String addressDetail;

    @Column(name = "city", length = 255)
    private String city;

    @Column(name = "district", length = 255)
    private String district;

    @Column(name = "ward", length = 255)
    private String ward;

    @Column(name = "seller_phone", length = 255)
    private String sellerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", length = 20)
    private SaleType saleType;

    @Column(name = "posting_fee", precision = 8, scale = 2)
    private BigDecimal postingFee;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "remind_before2_sent")
    private boolean remindBefore2Sent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approved_by")
    private Account approvedBy;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductImages> images;

    @Column(name = "manufacture_year")
    private Short manufactureYear;

    @Column(name = "featured_end_at")
    private LocalDateTime featuredEndAt;

    @Column(name = "start_renewal_at")
    private LocalDateTime startRenewalAt;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL)
    private VehicleDetails vehicleDetails;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL)
    private BatteryDetails batteryDetails;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = false)
    private Set<PostPayment> postPayments = new HashSet<>();

    @Column(name = "is_hot", nullable = false)
    @Builder.Default
    private Boolean isHot = false;

    // ============== EQUALS & HASHCODE - FIX STACKOVERFLOW ==============
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Product))
            return false;
        Product product = (Product) o;
        return getId() != null && getId().equals(product.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    // ==================================================================

    public enum ProductType {
        BATTERY, VEHICLE
    }

    public enum ConditionType {
        NEW, USED
    }

    public enum Status {
        DRAFT, ACTIVE, SOLD, PENDING_REVIEW, PENDING_PAYMENT, REJECTED, EXPIRED, HIDDEN
    }

    public enum SaleType {
        AUCTION, FIXED_PRICE, NEGOTIATION
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = VietNamDatetime.nowVietNam();
        }
    }
}