package com.evdealer.evdealermanagement.repository;

import com.evdealer.evdealermanagement.entity.product.Product;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {
  List<Product> findByType(Product.ProductType type);

  boolean existsById(@NotNull String productId);

  @Query("SELECT p FROM Product p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :title, '%'))")
  Page<Product> findTitlesByTitleContainingIgnoreCase(@Param("title") String title, Pageable pageable);

  @Query("""
          SELECT p FROM Product p
          LEFT JOIN PostPayment pay
              ON pay.product = p
             AND pay.createdAt = (
                  SELECT MAX(pp.createdAt)
                  FROM PostPayment pp
                  WHERE pp.product = p
              )
          LEFT JOIN pay.postPackage pkg
          WHERE p.status = :status
            AND p.expiresAt > :nowVN
          ORDER BY
            CASE
              WHEN p.featuredEndAt >= :nowVN THEN 0
              ELSE 1
            END,
            CASE
              WHEN pkg.code = 'SPECIAL' THEN 0
              WHEN pkg.code = 'PRIORITY' THEN 1
              WHEN pkg.code = 'STANDARD' THEN 2
              ELSE 3
            END,
            p.createdAt DESC
      """)
  List<Product> findActiveFeaturedSorted(Product.Status status, LocalDateTime nowVN, Pageable pageable);

  Optional<Product> findById(@NotNull String productId);

  List<Product> findByStatus(Product.Status status);

  Page<Product> findByStatus(Product.Status status, Pageable pageable);

  Page<Product> findByStatusAndType(Product.Status status, Product.ProductType type, Pageable pageable);

  @Query("SELECT p FROM Product p WHERE p.seller.id = :sellerId AND p.status = :status ORDER BY p.createdAt DESC")
  List<Product> findBySellerAndStatus(@Param("sellerId") String sellerId, @Param("status") Product.Status status);

  Optional<Product> findByIdAndSellerId(String id, String sellerId);

  long countBySeller_Id(String sellerId);

  long countByStatus(Product.Status status);

  long countByStatusAndUpdatedAtBetween(Product.Status status, LocalDateTime start, LocalDateTime end);

  @Query("SELECT p FROM Product p WHERE p.status = :status AND p.remindBefore2Sent = false AND p.expiresAt BETWEEN :start AND :end")
  List<Product> findExpiringBetween(@Param("status") Product.Status status, @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  // tìm để gửi thông báo
  @Query("SELECT p FROM Product p " +
      "WHERE p.expiresAt BETWEEN :start AND :end " +
      "AND (p.remindBefore2Sent = false OR p.remindBefore2Sent IS NULL) " +
      "AND p.status = 'ACTIVE'")
  List<Product> findExpiringBetweenAndNotReminded(
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  // tìm để ẩn đi vì hết hạn
  @Query("SELECT p FROM Product p " +
      "WHERE p.expiresAt < :now " +
      "AND p.status = 'ACTIVE'")
  List<Product> findExpiredAndActive(@Param("now") LocalDateTime now);

  Page<Product> findBySeller_IdAndStatus(String sellerId, Product.Status status, Pageable pageable);

  Page<Product> findBySeller_IdAndStatusIn(String sellerId, List<Product.Status> statuses, Pageable pageable);

  @Query("SELECT p FROM Product p WHERE p.startRenewalAt IS NOT NULL AND p.startRenewalAt <= :now")
  List<Product> findByStartRenewalAtBeforeAndStartRenewalAtNotNull(@Param("now") LocalDateTime now);

}