package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.account.review.SellerReviewRequest;
import com.evdealer.evdealermanagement.dto.account.review.SellerReviewResponse;
import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.entity.account.SellerReview;
import com.evdealer.evdealermanagement.entity.transactions.PurchaseRequest;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.repository.PurchaseRequestRepository;
import com.evdealer.evdealermanagement.repository.SellerReviewRepository;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerReviewService {

    private final SellerReviewRepository sellerReviewRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;

    @Transactional
    public SellerReviewResponse createSellerReview(String currentBuyerId, SellerReviewRequest request) {
        log.info("START createSellerReview - buyerId: {}, purchaseRequestId: {}",
                currentBuyerId, request.getPurchaseRequestId());

        try {
            // Validation: Purchase Request ID
            if (request.getPurchaseRequestId() == null || request.getPurchaseRequestId().isEmpty()) {
                log.warn("Invalid purchase request ID - buyerId: {}", currentBuyerId);
                throw new AppException(ErrorCode.INVALID_INPUT, "The purchase request id is invalid");
            }

            // 1. Lấy đơn mua
            log.debug("Fetching purchase request - id: {}", request.getPurchaseRequestId());
            PurchaseRequest purchaseRequest = purchaseRequestRepository
                    .findById(request.getPurchaseRequestId())
                    .orElseThrow(() -> {
                        log.error("Purchase request not found - id: {}", request.getPurchaseRequestId());
                        return new AppException(ErrorCode.PURCHASE_REQUEST_NOT_FOUND);
                    });
            log.debug("Purchase request found - sellerId: {}, buyerId: {}, status: {}",
                    purchaseRequest.getSeller().getId(),
                    purchaseRequest.getBuyer().getId(),
                    purchaseRequest.getContractStatus());

            // 2. Check đúng buyer
            if (!purchaseRequest.getBuyer().getId().equals(currentBuyerId)) {
                log.warn("Forbidden: User {} is not the buyer of purchase request {}",
                        currentBuyerId, request.getPurchaseRequestId());
                throw new AppException(ErrorCode.FORBIDDEN, "You are not the buyer in this transaction.");
            }
            log.debug("Buyer verification passed - buyerId: {}", currentBuyerId);

            // 3. Check trạng thái đã hoàn thành
            if (purchaseRequest.getContractStatus() != PurchaseRequest.ContractStatus.COMPLETED) {
                log.warn("Purchase request not completed - purchaseRequestId: {}, status: {}",
                        request.getPurchaseRequestId(), purchaseRequest.getContractStatus());
                throw new AppException(ErrorCode.FORBIDDEN, "Only evaluated when the transaction is completed");
            }
            log.debug("Purchase request status verified - status: COMPLETED");

            // 4. Check đã review đơn này chưa
            if (sellerReviewRepository.existsByPurchaseRequest_Id(purchaseRequest.getId())) {
                log.warn("Duplicate review attempt - buyerId: {}, purchaseRequestId: {}",
                        currentBuyerId, request.getPurchaseRequestId());
                throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "You have already rated this transaction");
            }
            log.debug("Duplicate check passed - no existing review found");

            // 5. Tạo review
            log.debug("Creating seller review - rating: {}, tags: {}",
                    request.getRating(), request.getTags());
            SellerReview review = SellerReview.builder()
                    .purchaseRequest(purchaseRequest)
                    .seller(purchaseRequest.getSeller())
                    .buyer(purchaseRequest.getBuyer())
                    .product(purchaseRequest.getProduct())
                    .rating(request.getRating())
                    .comment(request.getComment())
                    .tags(request.getTags() != null && !request.getTags().isEmpty()
                            ? String.join(",", request.getTags())
                            : null)
                    .createdAt(VietNamDatetime.nowVietNam())
                    .build();

            SellerReview savedReview = sellerReviewRepository.save(review);
            log.info("SUCCESS createSellerReview - reviewId: {}, sellerId: {}, buyerId: {}, rating: {}",
                    savedReview.getId(),
                    savedReview.getSeller().getId(),
                    savedReview.getBuyer().getId(),
                    savedReview.getRating());

            SellerReviewResponse response = toResponse(savedReview);
            log.debug("Review response created - reviewId: {}", response.getId());

            return response;

        } catch (AppException e) {
            log.error("FAILED createSellerReview - buyerId: {}, errorCode: {}, message: {}",
                    currentBuyerId, e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("UNEXPECTED ERROR createSellerReview - buyerId: {}, error: {}",
                    currentBuyerId, e.getMessage(), e);
            throw new AppException(ErrorCode.SERVICE_UNAVAILABLE, "Failed to create seller review");
        }
    }


    @Transactional(readOnly = true)
    public PageResponse<SellerReviewResponse> getReviewsOfSeller(String sellerId, Pageable pageable) {
        log.info("START getReviewsOfSeller - sellerId: {}, page: {}, size: {}",
                sellerId, pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<SellerReview> page = sellerReviewRepository.findBySeller_IdOrderByCreatedAtDesc(sellerId, pageable);

            log.debug("Reviews fetched - sellerId: {}, totalElements: {}, totalPages: {}",
                    sellerId, page.getTotalElements(), page.getTotalPages());

            PageResponse<SellerReviewResponse> response = PageResponse.fromPage(page, this::toResponse);

            log.info("SUCCESS getReviewsOfSeller - sellerId: {}, returned: {} reviews",
                    sellerId, response.getItems().size());

            return response;

        } catch (Exception e) {
            log.error("FAILED getReviewsOfSeller - sellerId: {}, error: {}",
                    sellerId, e.getMessage(), e);
            throw new AppException(ErrorCode.SERVICE_UNAVAILABLE, "Failed to get seller reviews");
        }
    }

    private SellerReviewResponse toResponse(SellerReview r) {
        log.trace("Converting review to response - reviewId: {}", r.getId());

        List<String> tags = (r.getTags() != null && !r.getTags().isEmpty())
                ? Arrays.stream(r.getTags().split(",")).toList()
                : List.of();

        return SellerReviewResponse.builder()
                .id(r.getId())
                .sellerId(r.getSeller().getId())
                .buyerId(r.getBuyer().getId())
                .sellerName(r.getSeller().getFullName())
                .buyerName(r.getBuyer().getFullName())
                .productId(r.getProduct().getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .tags(tags)
                .build();
    }

    @Transactional(readOnly = true)
    public Double getAvgRatingOfSeller(String sellerId) {
        log.info("START getAvgRatingOfSeller - sellerId: {}", sellerId);

        try {
            Double avg = sellerReviewRepository.getAverageRatingBySeller(sellerId);
            Double result = avg != null ? avg : 0.0;

            log.info("SUCCESS getAvgRatingOfSeller - sellerId: {}, avgRating: {}, isNull: {}",
                    sellerId, result, avg == null);

            return result;

        } catch (Exception e) {
            log.error("FAILED getAvgRatingOfSeller - sellerId: {}, error: {}",
                    sellerId, e.getMessage(), e);
            return 0.0;
        }
    }

    @Transactional(readOnly = true)
    public long countReviewsOfSeller(String sellerId) {
        log.info("START countReviewsOfSeller - sellerId: {}", sellerId);

        try {
            long count = sellerReviewRepository.countBySeller_Id(sellerId);

            log.info("SUCCESS countReviewsOfSeller - sellerId: {}, count: {}", sellerId, count);

            return count;

        } catch (Exception e) {
            log.error("FAILED countReviewsOfSeller - sellerId: {}, error: {}",
                    sellerId, e.getMessage(), e);
            return 0;
        }
    }
}
