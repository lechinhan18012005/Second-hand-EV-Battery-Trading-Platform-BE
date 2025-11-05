package com.evdealer.evdealermanagement.service.implement;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.evdealer.evdealermanagement.dto.transactions.ContractInfoDTO;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.entity.notify.Notification;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.entity.transactions.ContractDocument;
import com.evdealer.evdealermanagement.entity.transactions.PurchaseRequest;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.repository.ContractDocumentRepository;
import com.evdealer.evdealermanagement.repository.ProductRepository;
import com.evdealer.evdealermanagement.repository.PurchaseRequestRepository;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class EversignService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ContractDocumentRepository contractDocumentRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // CLoudinary config
    @Value("${CLOUDINARY_CLOUD_NAME}")
    private String cloudName;

    @Value("${CLOUDINARY_API_KEY}")
    private String cloudApiKey;

    @Value("${CLOUDINARY_API_SECRET}")
    private String cloudApiSecret;

    // Eversign Config
    @Getter
    @Value("${EVERSIGN_API_KEY}")
    private String apiKey;

    @Getter
    @Value("${EVERSIGN_BUSINESS_ID}")
    private String businessId;

    @Value("${EVERSIGN_TEMPLATE_ID}")
    private String templateId;

    @Value("${EVERSIGN_SANDBOX:true}") // ✅ mặc định sandbox
    private boolean sandboxMode;

    @Value("${APP_BASE_URL:http://localhost:8080}")
    private String appBaseUrl;

    private static final String EVERSIGN_API_BASE = "https://api.eversign.com/api";

    /**
     * Tạo hợp đồng để hai bên tự điền và ký (sandbox mode)
     */
    public ContractInfoDTO createBlankContractForManualInput(
            Account buyer,
            Account seller,
            Product product
    ) {
        try {
            log.info("🚀 [Eversign] Tạo hợp đồng trống (sandboxMode={})", sandboxMode);

            Map<String, Object> requestBody = buildContractRequest(buyer, seller, product);

            String url = String.format("%s/document?business_id=%s&access_key=%s",
                    EVERSIGN_API_BASE, businessId, apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            log.info("📬 [Eversign] Response status: {}", response.getStatusCode());
            log.debug("📥 [Eversign] Full response: {}", response.getBody());

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new AppException(ErrorCode.CONTRACT_BUILD_FAILED);
            }

            Map<String, Object> body = response.getBody();
            String documentHash = (String) body.get("document_hash");
            if (documentHash == null) {
                throw new AppException(ErrorCode.CONTRACT_BUILD_FAILED);
            }

            // Tạo link ký
            String buyerSignUrl = null;
            String sellerSignUrl = null;

            Object signersObj = body.get("signers");
            if (signersObj instanceof List<?> signersList) {
                for (Object obj : signersList) {
                    if (obj instanceof Map<?, ?> signer) {
                        String email = (String) signer.get("email");
                        String embeddedUrl = (String) signer.get("embedded_signing_url");
                        if (email != null && email.equalsIgnoreCase(buyer.getEmail())) {
                            buyerSignUrl = embeddedUrl;
                        } else if (email != null && email.equalsIgnoreCase(seller.getEmail())) {
                            sellerSignUrl = embeddedUrl;
                        }
                    }
                }
            }

            return ContractInfoDTO.builder()
                    .contractId(documentHash)
                    .contractUrl(buildContractViewUrl(documentHash))
                    .buyerSignUrl(buyerSignUrl)
                    .sellerSignUrl(sellerSignUrl)
                    .status("PENDING")
                    .build();

        } catch (Exception e) {
            log.error("🔥 [Eversign] Lỗi khi tạo hợp đồng trống: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi tạo hợp đồng với Eversign: " + e.getMessage());
        }
    }

    private Map<String, Object> buildContractRequest(
            Account buyer,
            Account seller,
            Product product
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("sandbox", sandboxMode ? 1 : 0); // ✅ bật sandbox
        body.put("business_id", businessId);
        body.put("template_id", templateId);
        body.put("title", "Hợp đồng mua bán sản phẩm - ECO GREEN");
        body.put("message", "Vui lòng điền thông tin và ký hợp đồng (sandbox).");
//        body.put("embedded_signing_enabled", 1);
        body.put("use_signer_order", 1);
        body.put("webhook_url", appBaseUrl + "/api/webhooks/eversign/document-complete");
        log.info("📡 Webhook URL gửi lên Eversign: {}", appBaseUrl + "/api/webhooks/eversign/document-complete");

        // 👥 Người ký
        List<Map<String, Object>> signers = new ArrayList<>();
        signers.add(Map.of(
                "role", "seller",
                "name", seller.getFullName(),
                "email", seller.getEmail(),
                "signing_order", 1
        ));
        signers.add(Map.of(
                "role", "buyer",
                "name", buyer.getFullName(),
                "email", buyer.getEmail(),
                "signing_order", 2
        ));
        body.put("signers", signers);

        log.debug("🧰 [Eversign] Request body (sandbox={}): {}", sandboxMode, body);
        return body;
    }

    private String buildContractViewUrl(String documentHash) {
        return String.format(
                "%s/document?business_id=%s&document_hash=%s&access_key=%s",
                EVERSIGN_API_BASE, businessId, documentHash, apiKey
        );
    }

    // ✅ CHỈ CẦN DUY NHẤT PHƯƠNG THỨC NÀY ĐỂ LÀM VIỆC LƯU TRỮ
    @Transactional
    public void createAndSaveContractDocument(PurchaseRequest request) {
        try {
            String documentHash = request.getContractId();
            if (documentHash == null) {
                log.error("❌ Không thể lưu ContractDocument vì request ID {} thiếu contractId.", request.getId());
                return;
            }

            if (contractDocumentRepository.findByDocumentId(documentHash).isPresent()) {
                log.warn("⚠️ ContractDocument cho hash {} đã tồn tại. Bỏ qua.", documentHash);
                return;
            }

            log.info("📑 Bắt đầu tạo bản ghi ContractDocument cho documentHash: {}", documentHash);

            String finalDocUrl = String.format(
                    "https://api.eversign.com/download_final_document?access_key=%s&business_id=%s&document_hash=%s&audit_trail=1",
                    apiKey, businessId, documentHash
            );

            ContractDocument contract = new ContractDocument();
            contract.setPurchaseRequest(request);
            contract.setDocumentId(documentHash);
            contract.setTitle("Hợp đồng mua bán - " + request.getProduct().getTitle());
            contract.setPdfUrl(finalDocUrl);
            contract.setSignerEmail(request.getBuyer().getEmail());
            contract.setSignedAt(VietNamDatetime.nowVietNam()); // Chính xác, chưa ký nên để null

            contractDocumentRepository.save(contract);
            log.info("✅ [DB] Đã lưu ContractDocument thành công với URL: {}", finalDocUrl);

        } catch (Exception e) {
            log.error("❌ [Eversign] Lỗi nghiêm trọng khi lưu ContractDocument: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi tạo và lưu ContractDocument: " + e.getMessage());
        }
    }

    @Transactional
    public void processDocumentCompletion(String documentHash) {
        log.info("🔍 Bắt đầu xử lý webhook cho document hash: {}", documentHash);

        PurchaseRequest request = purchaseRequestRepository.findByContractId(documentHash)
                .orElse(null);

        if (request == null) {
            log.warn("⚠️ Webhook được nhận nhưng không tìm thấy request nào cho contract hash: {}", documentHash);
            return; // Dừng xử lý nếu không tìm thấy request
        }

        // Chỉ cập nhật nếu trạng thái chưa phải là COMPLETED để tránh xử lý nhiều lần
        if (request.getContractStatus() == PurchaseRequest.ContractStatus.COMPLETED) {
            log.warn("⚠️ Webhook cho hợp đồng đã hoàn thành được nhận lại, bỏ qua. Hash: {}", documentHash);
            return;
        }

        // 1. Cập nhật trạng thái cho PurchaseRequest
        request.setContractStatus(PurchaseRequest.ContractStatus.COMPLETED);
        request.setStatus(PurchaseRequest.RequestStatus.CONTRACT_SIGNED);
        // Có thể cập nhật thời gian ký ở đây nếu cần
        if (request.getBuyerSignedAt() == null) request.setBuyerSignedAt(VietNamDatetime.nowVietNam());
        if (request.getSellerSignedAt() == null) request.setSellerSignedAt(VietNamDatetime.nowVietNam());

        purchaseRequestRepository.save(request);
        log.info("✅ Cập nhật trạng thái hợp đồng thành COMPLETED cho request: {}", request.getId());

        //Notification cho cả buyer và seller
        String content = String.format("Giao dịch %s đã hoàn tất. Cảm ơn bạn!",
                request.getProduct().getTitle());

        try {
            notificationService.createAndPush(
                    request.getBuyer().getId(),
                    "Giao dịch hoàn tất",
                    content,
                    Notification.NotificationType.PURCHASE_REQUEST_COMPLETED,
                    request.getId()
            );
            notificationService.createAndPush(
                    request.getSeller().getId(),
                    "Giao dịch hoàn tất",
                    content,
                    Notification.NotificationType.PURCHASE_REQUEST_COMPLETED,
                    request.getId()
            );
        } catch (Exception e) {
            log.warn("Failed to create notifications: {}", e.getMessage());
        }

        Product product = request.getProduct();
        if (product != null) {
            product.setStatus(Product.Status.SOLD); // Đổi trạng thái thành SOLD
            productRepository.save(product); // Lưu lại sản phẩm
            log.info("✅ Cập nhật trạng thái sản phẩm ID {} thành SOLD.", product.getId());
        } else {
            log.warn("⚠️ Không tìm thấy sản phẩm liên quan đến request ID {}.", request.getId());
        }

        // 2. Gọi phương thức lưu trữ file PDF (tên mới rõ ràng hơn)
        // Phương thức này giờ là một phần của cùng một transaction
        saveFinalContract(request);
    }

    // Đổi tên phương thức cũ để rõ ràng hơn, logic bên trong giữ nguyên
    // Phương thức này giờ sẽ được gọi bởi processDocumentCompletion
    private void saveFinalContract(PurchaseRequest request) {
        try {
            String documentHash = request.getContractId();
            log.info("📑 [Eversign] Bắt đầu lưu link hợp đồng, documentHash={}", documentHash);

            // ✅ Tạo URL tải trực tiếp từ Eversign
            String finalDocUrl = String.format(
                    "https://api.eversign.com/download_final_document?access_key=%s&business_id=%s&document_hash=%s&audit_trail=1",
                    apiKey, businessId, documentHash
            );

            // ✅ Lưu (hoặc cập nhật nếu đã tồn tại)
            ContractDocument contract = contractDocumentRepository.findByDocumentId(documentHash)
                    .orElse(new ContractDocument());

            contract.setDocumentId(documentHash);
            contract.setPurchaseRequest(request);
            contract.setTitle("Hợp đồng mua bán - " + request.getProduct().getTitle());
            contract.setPdfUrl(finalDocUrl);
            contract.setSignerEmail(request.getBuyer().getEmail());
            contract.setSignedAt(VietNamDatetime.nowVietNam());

            contractDocumentRepository.save(contract);

            log.info("✅ [DB] Đã lưu ContractDocument thành công với URL Eversign: {}", finalDocUrl);

        } catch (Exception e) {
            log.error("❌ [Eversign] Lỗi khi lưu link hợp đồng: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi xử lý và lưu hợp đồng từ Eversign: " + e.getMessage());
        }
    }


    @Scheduled(fixedDelay = 60000) // 3 phút (cho nhanh hơn)
    @Transactional
    public void autoSyncCompletedContracts() {
        log.info("🔄 [Auto-Sync] Bắt đầu kiểm tra các hợp đồng pending...");

        List<PurchaseRequest> pendingRequests = purchaseRequestRepository
                .findByContractStatus(PurchaseRequest.ContractStatus.SENT);

        if (pendingRequests.isEmpty()) {
            log.debug("✅ [Auto-Sync] Không có hợp đồng pending");
            return;
        }

        log.info("📋 [Auto-Sync] Tìm thấy {} hợp đồng cần kiểm tra", pendingRequests.size());

        for (PurchaseRequest request : pendingRequests) {
            try {
                String documentHash = request.getContractId();
                if (documentHash == null) {
                    log.warn("⚠️ Request {} không có contractId", request.getId());
                    continue;
                }

                // Gọi Eversign API để check status
                String url = String.format(
                        "%s/document?business_id=%s&document_hash=%s&access_key=%s",
                        EVERSIGN_API_BASE, businessId, documentHash, apiKey
                );

                log.debug("🔍 Checking document: {}", documentHash);
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map<String, Object> doc = response.getBody();

                    // Eversign trả về is_completed = 1 (string) hoặc true
                    Object isCompletedObj = doc.get("is_completed");
                    boolean isCompleted = "1".equals(String.valueOf(isCompletedObj))
                            || Boolean.TRUE.equals(isCompletedObj);

                    log.debug("📊 Document {} - is_completed: {}", documentHash, isCompletedObj);

                    if (isCompleted) {
                        log.info("🎉 [Auto-Sync] Phát hiện hợp đồng {} đã completed!", documentHash);
                        processDocumentCompletion(documentHash);
                    } else {
                        log.debug("⏳ Document {} vẫn chưa hoàn tất", documentHash);
                    }
                }

            } catch (Exception e) {
                log.error("❌ [Auto-Sync] Lỗi khi check hợp đồng {}: {}",
                        request.getContractId(), e.getMessage());
            }
        }
    }
}
