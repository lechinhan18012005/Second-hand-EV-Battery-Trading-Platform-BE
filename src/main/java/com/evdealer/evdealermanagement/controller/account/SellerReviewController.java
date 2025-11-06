package com.evdealer.evdealermanagement.controller.account;

import com.evdealer.evdealermanagement.dto.account.custom.CustomAccountDetails;
import com.evdealer.evdealermanagement.dto.account.review.SellerReviewRequest;
import com.evdealer.evdealermanagement.dto.account.review.SellerReviewResponse;
import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.service.implement.SellerReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/seller-reviews")
@RequiredArgsConstructor
public class SellerReviewController {

    private final SellerReviewService sellerReviewService;

    @PostMapping
    public ResponseEntity<SellerReviewResponse> create(
            @AuthenticationPrincipal CustomAccountDetails user,
            @RequestBody SellerReviewRequest request
    ) {
        String buyerId = user.getAccountId();
        return ResponseEntity.ok(sellerReviewService.createSellerReview(buyerId, request));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<PageResponse<SellerReviewResponse>> list(
            @PathVariable String sellerId,
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(sellerReviewService.getReviewsOfSeller(sellerId, pageable));
    }

    @GetMapping("/seller/{sellerId}/stats")
    public ResponseEntity<Map<String, Object>> stats(@PathVariable String sellerId) {
        Map<String, Object> res = new HashMap<>();
        res.put("average", sellerReviewService.getAvgRatingOfSeller(sellerId));
        res.put("total", sellerReviewService.countReviewsOfSeller(sellerId));
        return ResponseEntity.ok(res);
    }
}
