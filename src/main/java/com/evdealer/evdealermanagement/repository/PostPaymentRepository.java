package com.evdealer.evdealermanagement.repository;

import com.evdealer.evdealermanagement.entity.post.PostPayment;

import java.util.List;
import java.util.Optional;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostPaymentRepository extends JpaRepository<PostPayment, String> {
    Optional<PostPayment> findTopByProductIdAndPaymentStatusOrderByIdDesc(
            String productId,
            PostPayment.PaymentStatus status);

    Optional<PostPayment> findFirstByProductIdAndPaymentStatusOrderByCreatedAtDesc(
            String productId, PostPayment.PaymentStatus status);

    boolean existsByAccountIdAndPaymentStatus(String accountId, PostPayment.PaymentStatus status);

    Optional<PostPayment> findTopByProductIdAndPaymentStatusOrderByCreatedAtDesc(
            String productId,
            PostPayment.PaymentStatus paymentStatus);

    List<PostPayment> findAllByOrderByCreatedAtDesc();

    PostPayment findByProductId(String productId);

    PostPayment findFirstByProductIdOrderByCreatedAtDesc(String productId);

    Page<PostPayment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT p FROM PostPayment p
            WHERE p.product.id = :productId
              AND (p.paymentStatus = 'PENDING' OR p.paymentStatus = 'FAILED')
            ORDER BY p.createdAt DESC
            """)
    Optional<PostPayment> findLatestUncompletedByProductId(@Param("productId") String productId);

    @EntityGraph(attributePaths = {"product", "postPackage"})
    Page<PostPayment> findByProductSellerIdOrderByCreatedAtDesc(String sellerId, Pageable pageable);

    Optional<PostPayment> findTopByProductIdOrderByCreatedAtDesc(String id);
}
