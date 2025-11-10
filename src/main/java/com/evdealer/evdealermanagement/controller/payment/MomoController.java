package com.evdealer.evdealermanagement.controller.payment;

import com.evdealer.evdealermanagement.dto.payment.MomoResponse;
import com.evdealer.evdealermanagement.entity.post.PostPayment;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.repository.PostPaymentRepository;
import com.evdealer.evdealermanagement.repository.ProductRepository;
import com.evdealer.evdealermanagement.service.implement.PaymentService;
import com.evdealer.evdealermanagement.service.implement.ProductRenewalService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evdealer.evdealermanagement.dto.payment.MomoRequest;
import com.evdealer.evdealermanagement.service.implement.MomoService;

import java.io.IOException;
import java.util.Map;

@RequestMapping("/api/momo")
@RestController
@RequiredArgsConstructor
@Slf4j
public class MomoController {

    @Autowired
    private MomoService momoService;
    private final PaymentService paymentService;
    private final PostPaymentRepository postPaymentRepository;
    private final ProductRenewalService productRenewalService;
    private final ProductRepository productRepository;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @PostMapping
    public ResponseEntity<MomoResponse> createPayment(@RequestBody MomoRequest paymentRequest) {
        MomoResponse res = momoService.createPaymentRequest(paymentRequest);
        if (res.getResultCode() != null && res.getResultCode() == 0) {
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @GetMapping("/return")
    public void momoReturn(@RequestParam Map<String, String> params,
            HttpServletResponse response,
            HttpServletRequest request) throws IOException {
        log.info("🔔 MoMo return callback received");
        log.info("📦 Params: {}", params);

        String rawQuery = request.getQueryString();
        String paymentId = params.get("orderId"); // MoMo trả về orderId = paymentId của bạn
        String resultCode = params.get("resultCode"); // "0" = success

        try {
            if (paymentId == null || paymentId.isBlank()) {
                log.error("❌ Missing orderId (paymentId) in return");
                response.sendRedirect(frontendUrl + "/payment/momo-return"); // Redirect về FE ngay cả khi lỗi
                return;
            }

            // 1) Verify signature & success flag
            // boolean validSignature = momoService.verifyPaymentSignature(params); // đảm
            // bảo bạn có hàm này
            boolean success = "0".equals(resultCode);

            // log.info("🔐 Signature valid: {}", validSignature);
            log.info("✅ Payment success: {}", success);

            // 2) Load payment & product để quyết định route
            PostPayment payment = postPaymentRepository.findById(paymentId)
                    .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

            Product product = productRepository.findById(payment.getProduct().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            boolean isRenewal = (product.getStatus() == Product.Status.ACTIVE
                    || product.getStatus() == Product.Status.EXPIRED);

            log.info("🧭 RETURN router → productId={}, status={}, route={}",
                    product.getId(), product.getStatus(), (isRenewal ? "RENEWAL" : "POSTING"));

            // 3) Route đến handler phù hợp
            try {
                if (isRenewal) {
                    productRenewalService.handlePaymentCallbackFromRenewal(paymentId, success);
                } else {
                    paymentService.handlePaymentCallback(paymentId, success);
                }
                log.info("💾 Return callback handled successfully");
            } catch (Exception e) {
                log.error("❌ Error handling payment callback (return)", e);
                // vẫn redirect về FE nhưng mang trạng thái fail
                success = false;
            }

            // 4) Redirect về frontend (giống style VNPay): giữ nguyên query string mà MoMo
            // trả về
            String redirectUrl = frontendUrl + "/post/manage" + (rawQuery != null ? ("?" + rawQuery) : "");
            log.info("🔄 Redirecting to: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("❌ Error processing MoMo return", e);
            response.sendRedirect(frontendUrl + "/payment/momo-return");
        }
    }

    @PostMapping("/ipn")
    public ResponseEntity<String> momoIpn(@RequestBody Map<String, Object> body) {
        log.info("=== [MoMo IPN] Received callback ===");

        try {
            // Log toàn bộ body nhận từ MoMo (ẩn bớt nếu quá lớn)
            log.debug("MoMo IPN raw body: {}", body);

            // (Tuỳ chọn) Verify chữ ký HMAC từ body theo tài liệu MoMo
            String orderId = String.valueOf(body.get("orderId"));
            String resultCode = String.valueOf(body.get("resultCode"));
            boolean success = "0".equals(resultCode);

            log.info("Processing MoMo IPN for orderId={} | resultCode={} | success={}", orderId, resultCode, success);

            // Gọi service xử lý kết quả thanh toán
            paymentService.handlePaymentCallback(orderId, success);

            log.info("MoMo IPN handled successfully for orderId={}", orderId);
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error while handling MoMo IPN: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("FAILED");
        } finally {
            log.info("=== [MoMo IPN] Processing finished ===");
        }
    }
}
