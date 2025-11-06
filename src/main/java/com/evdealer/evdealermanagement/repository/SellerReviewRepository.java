package com.evdealer.evdealermanagement.repository;

import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.entity.account.SellerReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SellerReviewRepository extends JpaRepository<SellerReview, String> {

    // để show lên trang profile seller
    Page<SellerReview> findBySeller_IdOrderByCreatedAtDesc(String sellerId, Pageable pageable);

    // để chặn review trùng theo purchase_request_id (thực ra đã unique ở DB rồi)
    boolean existsByPurchaseRequest_Id(String purchaseRequestId);

    @Query("SELECT avg(r.rating) FROM SellerReview r WHERE r.seller.id = :sellerId")
    Double getAverageRatingBySeller(String sellerId);

    long countBySeller_Id(String sellerId);
}
