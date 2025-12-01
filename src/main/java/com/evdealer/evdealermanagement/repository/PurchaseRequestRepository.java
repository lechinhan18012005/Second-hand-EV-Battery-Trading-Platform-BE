package com.evdealer.evdealermanagement.repository;

import com.evdealer.evdealermanagement.entity.transactions.ContractDocument;
import com.evdealer.evdealermanagement.entity.transactions.PurchaseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, String> {

    List<PurchaseRequest> findByProductId(String productId);

    Page<PurchaseRequest> findByBuyerId(String buyerId, Pageable pageable);

    Page<PurchaseRequest> findBySellerId(String sellerId, Pageable pageable);

    Page<PurchaseRequest> findByBuyerIdAndStatus(String buyerId, PurchaseRequest.RequestStatus status,
            Pageable pageable);

    Page<PurchaseRequest> findBySellerIdAndStatus(String sellerId, PurchaseRequest.RequestStatus status,
            Pageable pageable);

    @Query("SELECT pr FROM PurchaseRequest pr " + "WHERE pr.product.id = :productId " + "AND pr.buyer.id = :buyerId "
            + "AND pr.status NOT IN ('REJECTED', 'CANCELLED', 'EXPIRED')")
    Optional<PurchaseRequest> findActivePurchaseRequest(@Param("productId") String productId,
            @Param("buyerId") String buyerId);

    long countBySellerIdAndStatus(String sellerId, PurchaseRequest.RequestStatus status);

    @Query("SELECT pr FROM PurchaseRequest pr " + "WHERE pr.contractId = :contractId")
    Optional<PurchaseRequest> findByContractId(@Param("contractId") String contractId);

    boolean existsByBuyerIdAndProductIdAndStatusIn(String buyerId, String productId,
            java.util.List<PurchaseRequest.RequestStatus> statuses);

    @EntityGraph(attributePaths = { "product" })
    @Query("""
                SELECT pr
                FROM PurchaseRequest pr
                WHERE pr.status = 'COMPLETED'
                  AND (pr.buyer.id = :accountId OR pr.seller.id = :accountId)
                ORDER BY pr.completedAt DESC
            """)
    Page<PurchaseRequest> findCompletedTransactionsByAccountId(String accountId, Pageable pageable);

    @Query("""
                SELECT cd
                FROM ContractDocument cd
                WHERE cd.purchaseRequest.buyer.id = :accountId
                   OR cd.purchaseRequest.seller.id = :accountId
                ORDER BY cd.signedAt DESC
            """)
    Page<ContractDocument> findAllByAccountInvolved(String accountId, Pageable pageable);

    List<PurchaseRequest> findByContractStatus(PurchaseRequest.ContractStatus status);

    @Query("""
            SELECT pr FROM PurchaseRequest pr
            JOIN FETCH pr.product p
            LEFT JOIN FETCH p.images
            WHERE pr.buyer.id = :buyerId
              AND pr.contractStatus = 'COMPLETED'
            """)
    Page<PurchaseRequest> findCompletedByBuyerId(@Param("buyerId") String buyerId, Pageable pageable);

    // Lấy 1 đơn bất kỳ (mới nhất) theo productId để suy ra sellerId
    Optional<PurchaseRequest> findTopByProduct_IdOrderByCreatedAtDesc(String productId);

    Optional<PurchaseRequest> findTopByProduct_IdAndSeller_IdAndBuyer_IdAndContractStatus(
            String productId,
            String sellerId,
            String buyerId,
            PurchaseRequest.ContractStatus contractStatus);

}
