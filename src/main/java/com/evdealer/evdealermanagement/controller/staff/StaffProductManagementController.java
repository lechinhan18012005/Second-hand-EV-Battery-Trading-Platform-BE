package com.evdealer.evdealermanagement.controller.staff;

import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.dto.post.verification.PostVerifyResponse;
import com.evdealer.evdealermanagement.entity.transactions.TransactionsHistory;
import com.evdealer.evdealermanagement.service.implement.ProductService;
import com.evdealer.evdealermanagement.service.implement.StaffService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/staff/product")
@Slf4j
@RequiredArgsConstructor
public class StaffProductManagementController {

    private final ProductService productService;
    private final StaffService staffService;

    @GetMapping("/by-status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<PageResponse<PostVerifyResponse>> getAllProductsWithStatus(@RequestParam String status,
            @PageableDefault(page = 0, size = 12, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            log.info("Request → Get all products by status: {}", status);
            PageResponse<PostVerifyResponse> products = productService.getAllProductsWithStatus(status.toUpperCase(),
                    pageable);

            if (products.getItems().isEmpty()) {
                log.info("No  products found");
                return ResponseEntity.noContent().build();
            }

            log.info("Found {} products", products.getItems().size());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error getting all products", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<PostVerifyResponse>> getAllProductsWithAllStatus(
            @PageableDefault(page = 0, size = 12, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            log.info("Request → Get all products");
            PageResponse<PostVerifyResponse> products = productService.getAllProductsWithAllStatus(pageable);

            if (products.getItems().isEmpty()) {
                log.info("No  products found");
                return ResponseEntity.noContent().build();
            }
            log.info("Found {} products", products.getItems().size());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error getting all products", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    /**
     * Lấy lịch sử giao dịch (các hợp đồng đã hoàn thành)
     */
    @GetMapping("/transaction/history")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public PageResponse<TransactionsHistory> getTransactionHistory(
            @PageableDefault(size = 10, sort = "signedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return staffService.getAllTransactionHistory(pageable);
    }

}
