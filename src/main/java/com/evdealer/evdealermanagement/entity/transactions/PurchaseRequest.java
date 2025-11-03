package com.evdealer.evdealermanagement.entity.transactions;

import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_requests", indexes = {
        @Index(name = "idx_product", columnList = "product_id"),
        @Index(name = "idx_buyer", columnList = "buyer_id"),
        @Index(name = "idx_seller", columnList = "seller_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_contract", columnList = "contract_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_purchase_request_product"))
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_purchase_request_buyer"))
    private Account buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_purchase_request_seller"))
    private Account seller;

    @Column(name = "offered_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal offeredPrice;

    @Column(name = "buyer_message", columnDefinition = "TEXT")
    private String buyerMessage;

    @Column(name = "seller_response_message", columnDefinition = "TEXT")
    private String sellerResponseMessage;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_status", columnDefinition = "VARCHAR(20)")
    private ContractStatus contractStatus;

    // Eversign contract information
    @Column(name = "contract_id", length = 100)
    private String contractId; // document_hash from Eversign

    @Column(name = "contract_url", length = 500)
    private String contractUrl; // View contract URL

    @Column(name = "buyer_sign_url", length = 500)
    private String buyerSignUrl; // Embedded signing URL for buyer

    @Column(name = "seller_sign_url", length = 500)
    private String sellerSignUrl; // Embedded signing URL for seller

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "buyer_signed_at")
    private LocalDateTime buyerSignedAt;

    @Column(name = "seller_signed_at")
    private LocalDateTime sellerSignedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = VietNamDatetime.nowVietNam();
        }
        if (status == null) {
            status = RequestStatus.PENDING;
        }
    }

    // Enums
    public enum RequestStatus {
        PENDING,           // Waiting for seller response
        ACCEPTED,          // Seller accepted
        REJECTED,          // Seller rejected
        CONTRACT_SENT,     // Contract sent to both parties
        COMPLETED,         // Contract completed (both signed)
        CANCELLED,         // Cancelled
        EXPIRED,
        CONTRACT_SIGNED,
        CONTRACT_FAILED// Request expired
    }

    public enum ContractStatus {
        PENDING,           // Creating contract
        SENT,              // Contract sent
        BUYER_SIGNED,      // Buyer signed
        SELLER_SIGNED,     // Seller signed
        COMPLETED,         // Both signed
        FAILED,            // Contract creation failed
        CANCELLED,         // Cancelled
        DECLINED           // One party declined to sign
    }
}