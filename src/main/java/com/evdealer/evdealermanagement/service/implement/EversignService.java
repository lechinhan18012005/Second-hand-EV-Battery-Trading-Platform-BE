package com.evdealer.evdealermanagement.service.implement;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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

    // Eversign Config
    @Getter
    @Value("${EVERSIGN_API_KEY}")
    private String apiKey;

    @Getter
    @Value("${EVERSIGN_BUSINESS_ID}")
    private String businessId;

    @Value("${EVERSIGN_TEMPLATE_ID}")
    private String templateId;

    @Value("${EVERSIGN_SANDBOX:true}")
    private boolean sandboxMode;

    @Value("${APP_BASE_URL:http://localhost:8080}")
    private String appBaseUrl;

    private static final String EVERSIGN_API_BASE = "https://api.eversign.com/api";
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * Tạo hợp đồng để hai bên tự điền và ký (sandbox mode)
     */
    public ContractInfoDTO createBlankContractForManualInput(
            Account buyer,
            Account seller,
            Product product) {
        try {
            log.info("[Eversign] Tạo hợp đồng trống (sandboxMode={})", sandboxMode);

            Map<String, Object> requestBody = buildContractRequest(buyer, seller, product);

            // Log request body để debug
            log.info("📤 [Eversign] Request body: {}", requestBody);

            String url = String.format("%s/document?business_id=%s&access_key=%s",
                    EVERSIGN_API_BASE, businessId, apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response;
            try {
                response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            } catch (Exception apiError) {
                // Log chi tiết lỗi từ API
                log.error("❌ [Eversign] API call failed: {}", apiError.getMessage());
                if (apiError instanceof org.springframework.web.client.HttpClientErrorException) {
                    org.springframework.web.client.HttpClientErrorException httpError =
                            (org.springframework.web.client.HttpClientErrorException) apiError;
                    log.error("❌ [Eversign] Status: {}", httpError.getStatusCode());
                    log.error("❌ [Eversign] Response body: {}", httpError.getResponseBodyAsString());
                }
                throw new AppException(ErrorCode.CONTRACT_BUILD_FAILED);
            }

            log.info("[Eversign] Response status: {}", response.getStatusCode());
            log.info("[Eversign] Full response: {}", response.getBody());

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new AppException(ErrorCode.CONTRACT_BUILD_FAILED);
            }

            Map<String, Object> body = response.getBody();

            // Kiểm tra có error từ Eversign không
            if (body.containsKey("error")) {
                log.error("❌ [Eversign] API returned error: {}", body.get("error"));
                throw new AppException(ErrorCode.CONTRACT_BUILD_FAILED);
            }

            String documentHash = (String) body.get("document_hash");
            if (documentHash == null) {
                log.error("❌ [Eversign] No document_hash in response: {}", body);
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

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Eversign] Lỗi khi tạo hợp đồng: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi tạo hợp đồng với Eversign: " + e.getMessage());
        }
    }

    private Map<String, Object> buildContractRequest(
            Account buyer,
            Account seller,
            Product product) {
        Map<String, Object> body = new HashMap<>();
        body.put("sandbox", sandboxMode ? 1 : 0);
        body.put("business_id", businessId);
        body.put("template_id", templateId);
        body.put("title", "Hợp đồng mua bán sản phẩm - ECO GREEN");
        body.put("message", "Vui lòng điền thông tin và ký hợp đồng.");
        body.put("use_signer_order", 1);
        body.put("webhook_url", appBaseUrl + "/api/webhooks/eversign/document-complete");
        log.info("📡 Webhook URL gửi lên Eversign: {}", appBaseUrl + "/api/webhooks/eversign/document-complete");

        // 👥 Người ký
        List<Map<String, Object>> signers = new ArrayList<>();
        signers.add(Map.of(
                "role", "seller",
                "name", seller.getFullName(),
                "email", seller.getEmail(),
                "signing_order", 1));
        signers.add(Map.of(
                "role", "buyer",
                "name", buyer.getFullName(),
                "email", buyer.getEmail(),
                "signing_order", 2));
        body.put("signers", signers);

        List<Map<String, Object>> fields = new ArrayList<>();

        // ===== THÔNG TIN BUYER - CÓ THỂ EDIT =====
        fields.add(createEditableTextField("buyer_name", buyer.getFullName(), "buyer"));
        fields.add(createEditableTextField("buyer_phone",
                buyer.getPhone() != null ? buyer.getPhone() : "", "buyer"));
        fields.add(createEditableTextField("buyer_address",
                buyer.getAddress() != null ? buyer.getAddress() : "", "buyer"));

        // ===== THÔNG TIN SELLER - CÓ THỂ EDIT =====
        fields.add(createEditableTextField("seller_name", seller.getFullName(), "seller"));
        fields.add(createEditableTextField("seller_phone",
                seller.getPhone() != null ? seller.getPhone() : "", "seller"));
        fields.add(createEditableTextField("seller_address",
                seller.getAddress() != null ? seller.getAddress() : "", "seller"));

        // ===== THÔNG TIN PRODUCT - CHỈ ĐỌC (KHÔNG CHO EDIT) =====
        fields.add(createReadOnlyField("product_name",
                product.getTitle() != null ? product.getTitle() : ""));
        fields.add(createReadOnlyField("product_type",
                product.getType() != null ? product.getType().toString() : ""));
        fields.add(createReadOnlyField("product_manufacturer_year",
                product.getManufactureYear() != null ? product.getManufactureYear().toString() : ""));
        fields.add(createReadOnlyField("product_price",
                product.getPrice() != null ? formatPrice(product.getPrice()) : ""));
        fields.add(createReadOnlyField("product_brand",
                Product.ProductType.VEHICLE == product.getType() ?
                        product.getVehicleDetails().getBrand().getName() :
                        product.getBatteryDetails().getBrand().getName()));

        // ===== THÔNG TIN NGÀY THÁNG - CHỈ ĐỌC =====
        fields.add(createReadOnlyField("place", "Ho Chi Minh"));
        fields.add(createReadOnlyField("day",
                String.valueOf(VietNamDatetime.nowVietNam().getDayOfMonth())));
        fields.add(createReadOnlyField("month",
                String.valueOf(VietNamDatetime.nowVietNam().getMonthValue())));
        fields.add(createReadOnlyField("year",
                String.valueOf(VietNamDatetime.nowVietNam().getYear())));

        body.put("fields", fields);

        log.debug("[Eversign] Request body (sandbox={}): {}", sandboxMode, body);
        return body;
    }

    private String formatPrice(BigDecimal price) {
        return String.format("%,.0f VNĐ", price);
    }

    private Map<String, Object> createReadOnlyField(String identifier, String value) {
        Map<String, Object> field = new HashMap<>();
        field.put("identifier", identifier);
        field.put("value", value != null ? value : "");
        field.put("type", "text");
        field.put("read_only", true);
        return field;
    }

    private Map<String, Object> createEditableTextField(String identifier, String value, String signerRole) {
        Map<String, Object> field = new HashMap<>();
        field.put("identifier", identifier);
        field.put("value", value != null ? value : "");
        field.put("type", "text");  // Loại field có thể edit
        field.put("signer", signerRole);  // Chỉ định người ký nào có thể edit
        field.put("required", true);  // Bắt buộc phải điền
        return field;
    }

    private String buildContractViewUrl(String documentHash) {
        return String.format(
                "%s/document?business_id=%s&document_hash=%s&access_key=%s",
                EVERSIGN_API_BASE, businessId, documentHash, apiKey);
    }

    @Transactional
    public void createAndSaveContractDocument(PurchaseRequest request) {
        try {
            String documentHash = request.getContractId();
            if (documentHash == null) {
                log.error("Không thể lưu ContractDocument vì request ID {} thiếu contractId.", request.getId());
                return;
            }

            if (contractDocumentRepository.findByDocumentId(documentHash).isPresent()) {
                log.warn("ContractDocument cho hash {} đã tồn tại. Bỏ qua.", documentHash);
                return;
            }

            log.info("Bắt đầu tạo bản ghi ContractDocument cho documentHash: {}", documentHash);

            String finalDocUrl = String.format(
                    "https://api.eversign.com/download_final_document?access_key=%s&business_id=%s&document_hash=%s&audit_trail=1",
                    apiKey, businessId, documentHash);

            ContractDocument contract = new ContractDocument();
            contract.setPurchaseRequest(request);
            contract.setDocumentId(documentHash);
            contract.setTitle("Hợp đồng mua bán - " + request.getProduct().getTitle());
            contract.setPdfUrl(finalDocUrl);
            contract.setSignerEmail(request.getBuyer().getEmail());
            contract.setSignedAt(null); // Chưa ký, để null

            contractDocumentRepository.save(contract);
            log.info("[DB] Đã lưu ContractDocument (chưa ký) với URL: {}", finalDocUrl);

        } catch (Exception e) {
            log.error("[Eversign] Lỗi nghiêm trọng khi lưu ContractDocument: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi tạo và lưu ContractDocument: " + e.getMessage());
        }
    }

    @Transactional
    public void processDocumentCompletion(String documentHash) {
        log.info("Bắt đầu xử lý webhook cho document hash: {}", documentHash);

        PurchaseRequest request = purchaseRequestRepository.findByContractId(documentHash)
                .orElse(null);

        if (request == null) {
            log.warn("Webhook được nhận nhưng không tìm thấy request nào cho contract hash: {}", documentHash);
            return;
        }

        if (request.getContractStatus() == PurchaseRequest.ContractStatus.COMPLETED) {
            log.warn("Webhook cho hợp đồng đã hoàn thành được nhận lại, bỏ qua. Hash: {}", documentHash);
            return;
        }

        // Lấy thời gian ký thực tế từ Eversign
        LocalDateTime actualSignedTime = fetchActualSignedTimeFromEversign(documentHash);
        LocalDateTime signedTime = actualSignedTime != null ? actualSignedTime : VietNamDatetime.nowVietNam();

        log.info("Thời gian ký hợp đồng: {}", signedTime);

        // 1. Cập nhật trạng thái cho PurchaseRequest
        request.setContractStatus(PurchaseRequest.ContractStatus.COMPLETED);
        request.setStatus(PurchaseRequest.RequestStatus.CONTRACT_SIGNED);

        // Sử dụng thời gian thực tế từ Eversign
        if (request.getBuyerSignedAt() == null) {
            request.setBuyerSignedAt(signedTime);
        }
        if (request.getSellerSignedAt() == null) {
            request.setSellerSignedAt(signedTime);
        }

        purchaseRequestRepository.save(request);
        log.info("Cập nhật trạng thái hợp đồng thành COMPLETED cho request: {}", request.getId());

        // Notification cho cả buyer và seller
        String content = String.format("Giao dịch %s đã hoàn tất. Cảm ơn bạn!",
                request.getProduct().getTitle());

        try {
            notificationService.createAndPush(
                    request.getBuyer().getId(),
                    "Giao dịch hoàn tất",
                    content,
                    Notification.NotificationType.PURCHASE_REQUEST_COMPLETED,
                    request.getId());
            notificationService.createAndPush(
                    request.getSeller().getId(),
                    "Giao dịch hoàn tất",
                    content,
                    Notification.NotificationType.PURCHASE_REQUEST_COMPLETED,
                    request.getId());
        } catch (Exception e) {
            log.warn("Failed to create notifications: {}", e.getMessage());
        }

        Product product = request.getProduct();
        if (product != null) {
            product.setStatus(Product.Status.SOLD);
            productRepository.save(product);
            log.info("Cập nhật trạng thái sản phẩm ID {} thành SOLD.", product.getId());
        } else {
            log.warn("Không tìm thấy sản phẩm liên quan đến request ID {}.", request.getId());
        }

        // 2. Cập nhật ContractDocument với thời gian ký chính xác
        saveFinalContract(request, signedTime);
    }

    /**
     * Lấy thời gian ký thực tế từ Eversign API
     */
    /**
     * Lấy thời gian ký thực tế từ Eversign API
     */
    private LocalDateTime fetchActualSignedTimeFromEversign(String documentHash) {
        try {
            String url = String.format(
                    "%s/document?business_id=%s&document_hash=%s&access_key=%s",
                    EVERSIGN_API_BASE, businessId, documentHash, apiKey);

            log.debug("Đang lấy thông tin document từ Eversign: {}", documentHash);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> doc = response.getBody();

                // Eversign trả về "completed_time" (Unix timestamp)
                Object completedTimeObj = doc.get("completed_time");

                if (completedTimeObj != null) {
                    long timestamp = Long.parseLong(String.valueOf(completedTimeObj));

                    // Nếu timestamp > 1_000_000_000_000 (12 chữ số) thì là milliseconds
                    if (timestamp > 1_000_000_000_000L) {
                        timestamp = timestamp / 1000;
                    }

                    //  FIX: Chuyển UTC timestamp thành LocalDateTime theo timezone Việt Nam
                    Instant utcInstant = Instant.ofEpochSecond(timestamp);
                    LocalDateTime signedTimeVn = LocalDateTime.ofInstant(utcInstant, ZoneOffset.UTC );

                    log.info("[Eversign] UTC Instant={} → VN LocalDateTime={} (timestamp={})",
                            utcInstant, signedTimeVn, timestamp);
                    return signedTimeVn;
                }
                else {
                    log.warn("Eversign không trả về completed_time cho document: {}", documentHash);
                }
            } else {
                log.warn("Eversign API trả về status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.warn("Không lấy được thời gian ký từ Eversign: {}", e.getMessage());
        }

        return null; // Fallback về null, caller sẽ dùng thời gian hiện tại
    }

    /**
     * Cập nhật ContractDocument với thời gian ký chính xác
     */
    private void saveFinalContract(PurchaseRequest request, LocalDateTime signedTime) {
        try {
            String documentHash = request.getContractId();
            log.info("[Eversign] Bắt đầu cập nhật ContractDocument, documentHash={}", documentHash);

            String finalDocUrl = String.format(
                    "https://api.eversign.com/download_final_document?access_key=%s&business_id=%s&document_hash=%s&audit_trail=1",
                    apiKey, businessId, documentHash);

            ContractDocument contract = contractDocumentRepository.findByDocumentId(documentHash)
                    .orElseGet(() -> {
                        log.warn("ContractDocument chưa tồn tại, tạo mới (không nên xảy ra)");
                        ContractDocument newContract = new ContractDocument();
                        newContract.setDocumentId(documentHash);
                        newContract.setPurchaseRequest(request);
                        newContract.setTitle("Hợp đồng mua bán - " + request.getProduct().getTitle());
                        newContract.setSignerEmail(request.getBuyer().getEmail());
                        return newContract;
                    });

            // Cập nhật thông tin khi hoàn tất với thời gian chính xác
            contract.setPdfUrl(finalDocUrl);
            contract.setSignedAt(signedTime);

            contractDocumentRepository.save(contract);

            log.info("[DB] Đã cập nhật ContractDocument với thời gian ký: {} và URL: {}",
                    signedTime, finalDocUrl);

        } catch (Exception e) {
            log.error("[Eversign] Lỗi khi cập nhật ContractDocument: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi xử lý và lưu hợp đồng từ Eversign: " + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void autoSyncCompletedContracts() {
        log.info("[Auto-Sync] Bắt đầu kiểm tra các hợp đồng pending...");

        List<PurchaseRequest> pendingRequests = purchaseRequestRepository
                .findByContractStatus(PurchaseRequest.ContractStatus.SENT);

        if (pendingRequests.isEmpty()) {
            log.debug("[Auto-Sync] Không có hợp đồng pending");
            return;
        }

        log.info("[Auto-Sync] Tìm thấy {} hợp đồng cần kiểm tra", pendingRequests.size());

        for (PurchaseRequest request : pendingRequests) {
            try {
                String documentHash = request.getContractId();
                if (documentHash == null) {
                    log.warn("Request {} không có contractId", request.getId());
                    continue;
                }

                String url = String.format(
                        "%s/document?business_id=%s&document_hash=%s&access_key=%s",
                        EVERSIGN_API_BASE, businessId, documentHash, apiKey);

                log.debug(" Checking document: {}", documentHash);
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map<String, Object> doc = response.getBody();

                    Object isCompletedObj = doc.get("is_completed");
                    boolean isCompleted = "1".equals(String.valueOf(isCompletedObj))
                            || Boolean.TRUE.equals(isCompletedObj);

                    log.debug("Document {} - is_completed: {}", documentHash, isCompletedObj);

                    if (isCompleted) {
                        log.info("[Auto-Sync] Phát hiện hợp đồng {} đã completed!", documentHash);
                        processDocumentCompletion(documentHash);
                    } else {
                        log.debug(" Document {} vẫn chưa hoàn tất", documentHash);
                    }
                }

            } catch (Exception e) {
                log.error("[Auto-Sync] Lỗi khi check hợp đồng {}: {}",
                        request.getContractId(), e.getMessage());
            }
        }
    }
}