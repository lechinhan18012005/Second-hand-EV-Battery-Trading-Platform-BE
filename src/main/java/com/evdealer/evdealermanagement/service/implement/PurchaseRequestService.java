package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.transactions.*;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.entity.notify.Notification;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.entity.transactions.PurchaseRequest;
import com.evdealer.evdealermanagement.repository.ProductRepository;
import com.evdealer.evdealermanagement.repository.PurchaseRequestRepository;
import com.evdealer.evdealermanagement.utils.CurrencyFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final ProductRepository productRepository;
    private final UserContextService userContextService;
    private final EversignService eversignService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // -----------------------------
    // 1️⃣ Buyer gửi yêu cầu mua
    // -----------------------------
    @Transactional
    public PurchaseRequestResponse createPurchaseRequest(CreatePurchaseRequestDTO dto) {
        Account buyer = userContextService.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        if (buyer.getEmail() == null || buyer.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bạn phải cập nhật địa chỉ email trước khi có thể gửi yêu cầu mua hàng.");
        }

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (product.getStatus() != Product.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not available for purchase");
        }

        boolean alreadyRequest = purchaseRequestRepository.existsByBuyerIdAndProductIdAndStatusIn(
                buyer.getId(),
                product.getId(),
                List.of(
                        PurchaseRequest.RequestStatus.PENDING,
                        PurchaseRequest.RequestStatus.CONTRACT_SENT,
                        PurchaseRequest.RequestStatus.ACCEPTED));

        if (alreadyRequest) {
            log.warn("Duplicate purchase request detected for buyer={} and product={}",
                    buyer.getId(), product.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bạn đã gửi yêu cầu mua sản phẩm này rồi. Vui lòng chờ người bán phản hồi.");
        }

        PurchaseRequest request = new PurchaseRequest();
        request.setId(UUID.randomUUID().toString());
        request.setProduct(product);
        request.setBuyer(buyer);
        request.setSeller(product.getSeller());
        request.setOfferedPrice(dto.getOfferedPrice() != null ? dto.getOfferedPrice() : product.getPrice());
        request.setBuyerMessage(dto.getBuyerMessage());
        request.setStatus(PurchaseRequest.RequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        request.setHasPurchaseRequested(true);

        PurchaseRequest saved = purchaseRequestRepository.save(request);

        try {
            // ✅ Truyền đúng 5 tham số cho EmailService
            emailService.sendPurchaseRequestNotification(
                    request.getSeller().getEmail(),
                    buyer.getFullName(),
                    product.getTitle(),
                    request.getOfferedPrice(),
                    request.getId());
        } catch (Exception e) {
            log.warn("Failed to send purchase request email: {}", e.getMessage());
        }

        // Notification cho seller
        try {
            notificationService.createAndPush(
                    request.getSeller().getId(),
                    "Yêu cầu mua hàng",
                    String.format("%s muốn mua %s với giá %s.",
                            buyer.getFullName(),
                            product.getTitle(),
                            CurrencyFormatter.format(request.getOfferedPrice())),
                    Notification.NotificationType.PURCHASE_REQUEST,
                    saved.getId());
        } catch (Exception e) {
            log.warn("Failed to create notification: {}", e.getMessage());
        }

        return mapToResponse(saved);
    }

    // -----------------------------
    // 2️⃣ Buyer xem các request
    // -----------------------------
    @Transactional(readOnly = true)
    public Page<PurchaseRequestResponse> getBuyerRequests(Pageable pageable) {
        Account buyer = userContextService.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        return purchaseRequestRepository.findByBuyerId(buyer.getId(), pageable)
                .map(this::mapToResponse);
    }

    // -----------------------------
    // 3️⃣ Seller xem các request
    // -----------------------------
    @Transactional(readOnly = true)
    public Page<PurchaseRequestResponse> getSellerRequests(Pageable pageable) {
        Account seller = userContextService.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        return purchaseRequestRepository.findBySellerId(seller.getId(), pageable)
                .map(this::mapToResponse);
    }

    // -----------------------------
    // 5️⃣ Xem chi tiết
    // -----------------------------
    @Transactional(readOnly = true)
    public PurchaseRequestResponse getRequestDetail(String requestId) {
        PurchaseRequest request = purchaseRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        return mapToResponse(request);
    }

    // -----------------------------
    // 6️⃣ Seller phản hồi (accept / reject)
    // -----------------------------
    @Transactional
    public PurchaseRequestResponse respondToPurchaseRequest(SellerResponseDTO dto) {
        Account seller = userContextService.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        PurchaseRequest request = purchaseRequestRepository.findById(dto.getRequestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!request.getSeller().getId().equals(seller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the seller of this request");
        }

        if (request.getStatus() != PurchaseRequest.RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request has already been processed");
        }

        return dto.getAccept()
                ? handleAcceptRequest(request, dto.getResponseMessage())
                : handleRejectRequest(request, dto.getRejectReason());
    }

    private PurchaseRequestResponse handleAcceptRequest(PurchaseRequest request, String responseMessage) {
        Product product = request.getProduct();

        // Kiểm tra lại product đã bị accept request khác chưa
        if (product.getStatus() != Product.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sản phẩm này đã có yêu cầu mua được chấp nhận hoặc không còn khả dụng.");
        }
        product.setStatus(Product.Status.HIDDEN);
        productRepository.save(product);

        request.setSellerResponseMessage(responseMessage);
        request.setRespondedAt(LocalDateTime.now());
        request.setStatus(PurchaseRequest.RequestStatus.ACCEPTED);
        request.setContractStatus(PurchaseRequest.ContractStatus.PENDING);

        try {
            ContractInfoDTO contractInfo = eversignService.createBlankContractForManualInput(
                    request.getBuyer(),
                    request.getSeller(),
                    product);

            if (contractInfo != null && contractInfo.getContractId() != null) {
                request.setContractId(contractInfo.getContractId());
                request.setContractUrl(contractInfo.getContractUrl());
                request.setBuyerSignUrl(contractInfo.getBuyerSignUrl());
                request.setSellerSignUrl(contractInfo.getSellerSignUrl());
                request.setContractStatus(PurchaseRequest.ContractStatus.SENT);
                request.setStatus(PurchaseRequest.RequestStatus.CONTRACT_SENT);

                PurchaseRequest saved = purchaseRequestRepository.save(request);
                eversignService.createAndSaveContractDocument(saved);
                sendContractEmails(saved, contractInfo);

                // THÊM: Notification cho buyer
                try {
                    notificationService.createAndPush(
                            request.getBuyer().getId(),
                            "Yêu cầu mua đã được chấp nhận",
                            String.format("%s đã chấp nhận yêu cầu cho %s. Vui lòng ký hợp đồng điện tử.",
                                    request.getSeller().getFullName(),
                                    request.getProduct().getTitle()),
                            Notification.NotificationType.PURCHASE_REQUEST_ACCEPTED,
                            request.getId());
                } catch (Exception e) {
                    log.warn("Failed to create notification: {}", e.getMessage());
                }

                return mapToResponse(saved);
            } else {
                throw new IllegalStateException("Eversign API returned missing contract info");
            }

        } catch (Exception e) {
            log.error("Contract creation failed for request {}: {}", request.getId(), e.getMessage(), e);
            request.setContractStatus(PurchaseRequest.ContractStatus.FAILED);
            request.setStatus(PurchaseRequest.RequestStatus.CONTRACT_FAILED);
            purchaseRequestRepository.save(request);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Đã chấp nhận yêu cầu nhưng không thể tạo hợp đồng điện tử.");
        }
    }

    private void sendContractEmails(PurchaseRequest request, ContractInfoDTO contractInfo) {
        try {
            emailService.sendContractToBuyer(
                    request.getBuyer().getEmail(),
                    request.getBuyer().getFullName(),
                    request.getSeller().getFullName(),
                    request.getProduct().getTitle(),
                    contractInfo.getBuyerSignUrl());
            emailService.sendContractToSeller(
                    request.getSeller().getEmail(),
                    request.getSeller().getFullName(),
                    request.getBuyer().getFullName(),
                    request.getProduct().getTitle(),
                    contractInfo.getSellerSignUrl());
        } catch (Exception e) {
            log.warn("Email sending failed: {}", e.getMessage());
        }
    }

    private PurchaseRequestResponse handleRejectRequest(PurchaseRequest request, String rejectReason) {
        request.setStatus(PurchaseRequest.RequestStatus.REJECTED);
        request.setRejectReason(rejectReason);
        request.setRespondedAt(LocalDateTime.now());
        PurchaseRequest saved = purchaseRequestRepository.save(request);

        try {
            emailService.sendPurchaseRejectedNotification(
                    request.getBuyer().getEmail(),
                    request.getSeller().getFullName(),
                    request.getProduct().getTitle(),
                    rejectReason);
        } catch (Exception e) {
            log.warn("Failed to send rejection email: {}", e.getMessage());
        }

        // Notification cho buyer
        try {
            String reason = (rejectReason == null || rejectReason.isBlank())
                    ? "Không có lý do cụ thể"
                    : rejectReason;
            notificationService.createAndPush(
                    request.getBuyer().getId(),
                    "Yêu cầu mua bị từ chối",
                    String.format("%s đã từ chối yêu cầu cho %s. Lý do: %s",
                            request.getSeller().getFullName(),
                            request.getProduct().getTitle(),
                            reason),
                    Notification.NotificationType.PURCHASE_REQUEST_REJECTED,
                    request.getId());
        } catch (Exception e) {
            log.warn("Failed to create notification: {}", e.getMessage());
        }

        return mapToResponse(saved);
    }

    private PurchaseRequestResponse mapToResponse(PurchaseRequest request) {
        Product product = request.getProduct();
        return PurchaseRequestResponse.builder()
                .id(request.getId())
                .productId(product.getId())
                .productTitle(product.getTitle())
                .productPrice(product.getPrice())
                .buyerId(request.getBuyer().getId())
                .buyerName(request.getBuyer().getFullName())
                .buyerEmail(request.getBuyer().getEmail())
                .sellerId(request.getSeller().getId())
                .sellerName(request.getSeller().getFullName())
                .sellerEmail(request.getSeller().getEmail())
                .offeredPrice(request.getOfferedPrice())
                .buyerMessage(request.getBuyerMessage())
                .sellerResponseMessage(request.getSellerResponseMessage())
                .status(request.getStatus().name())
                .contractStatus(request.getContractStatus() != null ? request.getContractStatus().name() : null)
                .contractUrl(request.getContractUrl())
                .rejectReason(request.getRejectReason())
                .createdAt(request.getCreatedAt())
                .respondedAt(request.getRespondedAt())
                .hasPurchaseRequested(request.isHasPurchaseRequested())
                .build();
    }
}
