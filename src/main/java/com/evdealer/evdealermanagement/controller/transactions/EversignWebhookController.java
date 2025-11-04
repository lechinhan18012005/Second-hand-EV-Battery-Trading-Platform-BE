package com.evdealer.evdealermanagement.controller.transactions;

import com.evdealer.evdealermanagement.repository.PurchaseRequestRepository;
import com.evdealer.evdealermanagement.service.implement.EversignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/eversign")
@RequiredArgsConstructor
@Slf4j
public class EversignWebhookController {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final EversignService eversignService;

    /**
     * Webhook cho document completed (all signers signed)
     */
    @PostMapping("/document-complete")
    public ResponseEntity<?> handleDocumentComplete(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader Map<String, String> headers
    ) {
        log.info("================== WEBHOOK RECEIVED ==================");
        log.info("📥 Headers: {}", headers);
        log.info("📦 Payload: {}", payload);
        log.info("=====================================================");

        if (payload == null || !payload.containsKey("document_hash")) {
            log.error("❌ Webhook nhận được body rỗng hoặc thiếu 'document_hash'");
            return ResponseEntity.ok(Map.of("success", true, "message", "Test webhook received"));
        }

        String documentHash = (String) payload.get("document_hash");
        log.info("🎯 Processing document_hash: {}", documentHash);

        try {
            eversignService.processDocumentCompletion(documentHash);
            log.info("✅ Webhook processed successfully for: {}", documentHash);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("❌ Lỗi khi xử lý webhook cho {}: {}", documentHash, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/admin/manual-complete/{documentHash}")
    public ResponseEntity<?> manualComplete(@PathVariable String documentHash) {
        log.info("🔧 [ADMIN] Manual trigger cho document: {}", documentHash);

        try {
            // Verify với Eversign trước
            String url = String.format(
                    "https://api.eversign.com/document?business_id=%s&document_hash=%s&access_key=%s",
                    eversignService.getBusinessId(), // Cần expose getter
                    documentHash,
                    eversignService.getApiKey() // Cần expose getter
            );

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getBody() != null) {
                Object isCompleted = response.getBody().get("is_completed");
                log.info("📊 Eversign status: is_completed = {}", isCompleted);

                boolean completed = "1".equals(String.valueOf(isCompleted))
                        || Boolean.TRUE.equals(isCompleted);

                if (!completed) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "error", "Document chưa được ký hoàn tất trên Eversign",
                            "is_completed", isCompleted
                    ));
                }
            }

            // Process completion
            eversignService.processDocumentCompletion(documentHash);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ Đã xử lý thành công document: " + documentHash
            ));

        } catch (Exception e) {
            log.error("❌ Lỗi: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}