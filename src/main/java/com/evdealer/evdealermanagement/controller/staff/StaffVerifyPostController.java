package com.evdealer.evdealermanagement.controller.staff;

import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.dto.post.verification.PostVerifyRequest;
import com.evdealer.evdealermanagement.dto.post.verification.PostVerifyResponse;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.entity.report.Report;
import com.evdealer.evdealermanagement.service.implement.ReportService;
import com.evdealer.evdealermanagement.service.implement.StaffService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/staff/post")
@RequiredArgsConstructor
public class StaffVerifyPostController {

    private final StaffService staffService;
    private final ReportService reportService;

    @PostMapping("/{productId}/verify/active")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<PostVerifyResponse> approvePost(@PathVariable String productId) {
        PostVerifyResponse response = staffService.verifyPostActive(productId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{productId}/verify/reject")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<PostVerifyResponse> rejectPost(
            @PathVariable String productId,
            @Valid @RequestBody PostVerifyRequest request) {
        PostVerifyResponse response = staffService.verifyPostReject(productId, request.getRejectReason());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending/review")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public PageResponse<PostVerifyResponse> listPendingPosts(
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return staffService.listPendingPosts(pageable);
    }

    @GetMapping("/pending/review/type")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public PageResponse<PostVerifyResponse> listPendingPostsByType(
            @RequestParam(name = "type", required = false) Product.ProductType type,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return staffService.listPendingPostsByType(type, pageable);
    }

    @PatchMapping("/reports/{reportId}/status")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<Report.ReportStatus> resolveReport(@PathVariable String reportId) {
        Report.ReportStatus status = reportService.updateStatusReport(reportId);
        return ResponseEntity.ok(status);
    }

}
