package com.evdealer.evdealermanagement.entity.notify;

import com.evdealer.evdealermanagement.entity.BaseEntity;
import com.evdealer.evdealermanagement.entity.account.Account;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @NotNull(message = "Account không được để trống")
    Account account;

    @Column(name = "title", length = 255, nullable = false)
    @NotBlank(message = "Title không được để trống")
    @Size(max = 255, message = "Title không được vượt quá 255 ký tự")
    String title;

    @Column(name = "content", columnDefinition = "TEXT")
    @NotBlank(message = "Content không được để trống")
    @Size(max = 10_000, message = "Content không được vượt quá 10.000 ký tự")
    String content;

    @Column(name = "ref_id", length = 100)
    @Size(max = 100, message = "RefId không được vượt quá 100 ký tự")
    String refId;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Type không được để trống")
    NotificationType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    @NotNull(message = "CreatedAt không được để trống")
    LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    boolean read = false;

    public enum NotificationType {
        PRODUCT_EXPIRE_SOON,
        PRODUCT_SOLD,
        PAYMENT_SUCCESS,
        SYSTEM_ANNOUNCEMENT,
        PURCHASE_REQUEST_ACCEPTED,     // Seller chấp nhận → notify Buyer
        PURCHASE_REQUEST_REJECTED,     // Seller từ chối → notify Buyer
        PURCHASE_REQUEST_COMPLETED,    // Hai bên ký xong → notify 2 bên
        PURCHASE_REQUEST_CANCELLED     // Hủy (bên nào cũng được) → notify bên còn lại
    }
}
