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
    WHERE p.status = :status
      
      AND EXISTS (
          SELECT 1 FROM PostPayment pay WHERE pay.product = p
      )
     
      AND NOT EXISTS (
          SELECT 1
          FROM PostPayment pay2
          JOIN pay2.postPackage pkg2
          WHERE pay2.product = p
            
            AND pay2.createdAt = (
                SELECT MAX(pp.createdAt)
                FROM PostPayment pp
                WHERE pp.product = p
            )
            
            AND pkg2.code IN ('SPECIAL', 'PRIORITY')
            
            AND p.featuredEndAt IS NOT NULL
            AND p.featuredEndAt < :nowVN
      )
    ORDER BY
        (
            SELECT 
                CASE 
                    WHEN pkg.code = 'SPECIAL' THEN 0
                    WHEN pkg.code = 'PRIORITY' THEN 1
                    WHEN pkg.code = 'STANDARD' THEN 2
                    ELSE 3
                END
            FROM PostPayment pay2
            JOIN pay2.postPackage pkg
            WHERE pay2.product = p
              AND pay2.createdAt = (
                  SELECT MAX(pp.createdAt)
                  FROM PostPayment pp
                  WHERE pp.product = p
              )
        ) ASC,
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
    List<Product> findExpiringBetween(@Param("status") Product.Status status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // tìm để gửi thông báo
    @Query("SELECT p FROM Product p " +
            "WHERE p.expiresAt BETWEEN :start AND :end " +
            "AND (p.remindBefore2Sent = false OR p.remindBefore2Sent IS NULL) " +
            "AND p.status = 'ACTIVE'")
    List<Product> findExpiringBetweenAndNotReminded(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    //tìm để ẩn đi vì hết hạn
    @Query("SELECT p FROM Product p " +
            "WHERE p.expiresAt < :now " +
            "AND p.status = 'ACTIVE'")
    List<Product> findExpiredAndActive(@Param("now") LocalDateTime now);
}