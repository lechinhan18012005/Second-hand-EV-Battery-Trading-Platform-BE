package com.evdealer.evdealermanagement.entity.account;

import com.evdealer.evdealermanagement.entity.BaseEntity;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.entity.transactions.PurchaseRequest;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "seller_reviews",
        uniqueConstraints = {
                // 1 đơn mua chỉ được review 1 lần
                @UniqueConstraint(name = "uq_review_per_transaction", columnNames = {"purchase_request_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerReview extends  BaseEntity {

    // liên kết với đơn mua
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;

    // để query nhanh theo seller
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Account seller;

    // để biết ai review
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Account buyer;

    // để biết đánh giá sản phẩm nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int rating;   // 1..5

    @Column(columnDefinition = "TEXT")
    private String comment;

    // "UY_TIN,GIAO_NHANH"
    private String tags;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
