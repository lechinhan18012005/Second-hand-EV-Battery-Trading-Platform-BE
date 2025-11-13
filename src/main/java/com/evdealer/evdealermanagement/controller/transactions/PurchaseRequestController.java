package com.evdealer.evdealermanagement.controller.transactions;

import com.evdealer.evdealermanagement.dto.transactions.CreatePurchaseRequestDTO;
import com.evdealer.evdealermanagement.dto.transactions.PurchaseRequestResponse;
import com.evdealer.evdealermanagement.dto.transactions.SellerResponseDTO;
import com.evdealer.evdealermanagement.service.implement.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/member/purchase-request")
@RequiredArgsConstructor
@Slf4j
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    private static final String FRONTEND_BASE_URL = "https://evdealer.com";

    // -----------------------------
    // 1. Tạo yêu cầu mua
    // -----------------------------
    @PostMapping("/create")
    public ResponseEntity<PurchaseRequestResponse> create(@Valid @RequestBody CreatePurchaseRequestDTO dto) {
        log.info("Creating purchase request for product: {}", dto.getProductId());
        PurchaseRequestResponse response = purchaseRequestService.createPurchaseRequest(dto);
        return ResponseEntity.ok(response);
    }

    // -----------------------------
    // 2. Seller phản hồi (POST)
    // -----------------------------
    @PostMapping("/respond")
    public ResponseEntity<PurchaseRequestResponse> respond(@Valid @RequestBody SellerResponseDTO dto) {
        log.info("Seller responding to purchase request via POST: {}", dto.getRequestId());
        PurchaseRequestResponse response = purchaseRequestService.respondToPurchaseRequest(dto);
        return ResponseEntity.ok(response);
    }

    // -----------------------------
    // 3. Seller phản hồi qua Email (GET)
    // -----------------------------
    @GetMapping("/respond/email")
    public ResponseEntity<?> respondFromEmail(
            @RequestParam(required = false) String reason,
            @RequestParam String requestId,
            @RequestParam boolean accept) {

        log.info("Seller responding from email link. Request ID: {}, Accept: {}", requestId, accept);

        // Tạo DTO từ query param
        SellerResponseDTO dto = new SellerResponseDTO();
        dto.setRequestId(requestId);
        dto.setAccept(accept);

        if(accept) {
            dto.setResponseMessage("Đồng ý bán sản phẩm. Vui lòng xem và ký hợp đồng.");
            dto.setRejectReason(null);
        } else {
            String defaultMessage = (reason != null && !reason.isBlank())
                    ? reason
                    : "Xin lỗi, hiện tại tôi chưa thể bán sản phẩm này.";
            dto.setResponseMessage(defaultMessage);
            dto.setRejectReason(defaultMessage);
        }

        try {
            PurchaseRequestResponse response = purchaseRequestService.respondToPurchaseRequest(dto);

            String redirectUrl = String.format(FRONTEND_BASE_URL + "/seller/requests/%s?status=%s",
                    requestId, response.getStatus());

            return ResponseEntity.status(302)
                    .location(URI.create(redirectUrl))
                    .build();

        } catch (ResponseStatusException e) {
            log.error("Error responding to purchase request {} from email: {}", requestId, e.getReason(), e);
            String errorRedirectUrl = String.format(FRONTEND_BASE_URL + "/error?message=purchase_request_error&reason=%s",
                    e.getReason().replace(' ', '+'));

            return ResponseEntity.status(302)
                    .location(URI.create(errorRedirectUrl))
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error processing email response for request {}: {}", requestId, e.getMessage(), e);
            String errorRedirectUrl = String.format(FRONTEND_BASE_URL + "/error?message=purchase_request_internal_error");

            return ResponseEntity.status(302)
                    .location(URI.create(errorRedirectUrl))
                    .build();
        }
    }

    // -----------------------------
    // 4. Lấy request của Buyer
    // -----------------------------
    @GetMapping("/buyer")
    public ResponseEntity<Page<PurchaseRequestResponse>> getBuyerRequests(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("Fetching buyer's purchase requests");
        Page<PurchaseRequestResponse> requests = purchaseRequestService.getBuyerRequests(pageable);
        return ResponseEntity.ok(requests);
    }

    // -----------------------------
    // 5. Lấy request của Seller
    // -----------------------------
    @GetMapping("/seller")
    public ResponseEntity<Page<PurchaseRequestResponse>> getSellerRequests(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("Fetching seller's purchase requests");
        Page<PurchaseRequestResponse> requests = purchaseRequestService.getSellerRequests(pageable);
        return ResponseEntity.ok(requests);
    }

    // -----------------------------
    // 7. Chi tiết request
    // -----------------------------
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseRequestResponse> getDetail(@PathVariable String id) {
        log.info("Fetching purchase request detail: {}", id);
        PurchaseRequestResponse response = purchaseRequestService.getRequestDetail(id);
        return ResponseEntity.ok(response);
    }

    // -----------------------------
    // 8. Test endpoint nhanh accept/reject POST
    // -----------------------------
    @PostMapping("/test/respond")
    public ResponseEntity<PurchaseRequestResponse> testRespond(
            @RequestParam String requestId,
            @RequestParam boolean accept) {

        SellerResponseDTO dto = new SellerResponseDTO();
        dto.setRequestId(requestId);
        dto.setAccept(accept);
        dto.setResponseMessage(accept ? "Test accept" : "Test reject");
        dto.setRejectReason(!accept ? "Test reject" : null);

        log.info("Testing respond for requestId={} accept={}", requestId, accept);
        PurchaseRequestResponse response = purchaseRequestService.respondToPurchaseRequest(dto);
        return ResponseEntity.ok(response);
    }
}
