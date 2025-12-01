package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.transactions.*;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.entity.notify.Notification;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.entity.transactions.PurchaseRequest;
import com.evdealer.evdealermanagement.exceptions.AppException;
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

    @Transactional
    public PurchaseRequestResponse createPurchaseRequest(CreatePurchaseRequestDTO dto) {
        Account buyer = userContextService.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        if (buyer.getEmail() == null || buyer.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bạn phải cập nhật địa chỉ email trước khi có thể gửi yêu cầu mua hàng.");
        }

        if (buyer.getAddress() == null || buyer.getAddress().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bạn phải cập nhật địa chỉ nơi ở trước khi có thể gửi yêu cầu mua hàng để làm hợp đồng điện tử.");
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
            emailService.sendPurchaseRequestNotification(
                    request.getSeller().getEmail(),
                    buyer.getFullName(),
                    product.getTitle(),
                    request.getOfferedPrice(),
                    request.getId());
        } catch (Exception e) {
            log.warn("Failed to send purchase request email: {}", e.getMessage());
        }

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

    @Transactional(readOnly = true)
    public Page<PurchaseRequestResponse> getBuyerRequests(Pageable pageable) {
        Account buyer = userContextService.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        return purchaseRequestRepository.findByBuyerId(buyer.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseRequestResponse> getSellerRequests(Pageable pageable) {
        Account seller = userContextService.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        return purchaseRequestRepository.findBySellerId(seller.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public PurchaseRequestResponse getRequestDetail(String requestId) {
        PurchaseRequest request = purchaseRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        return mapToResponse(request);
    }

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

        if (product.getStatus() != Product.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sản phẩm này đã có yêu cầu mua được chấp nhận hoặc không còn khả dụng.");
        }

        log.info("Updating product {} - Price: {} to {}",
                product.getId(), product.getPrice(), request.getOfferedPrice());

        product.setPrice(request.getOfferedPrice());
        product.setStatus(Product.Status.HIDDEN);
        productRepository.save(product);

        request.setSellerResponseMessage(responseMessage);
        request.setRespondedAt(LocalDateTime.now());
        request.setStatus(PurchaseRequest.RequestStatus.ACCEPTED);
        request.setContractStatus(PurchaseRequest.ContractStatus.PENDING);

        try {
            log.info("Creating Eversign contract for request {}", request.getId());

            if (request.getBuyer().getEmail() == null || request.getBuyer().getEmail().isBlank()) {
                throw new IllegalStateException("Buyer email is missing");
            }
            if (request.getSeller().getEmail() == null || request.getSeller().getEmail().isBlank()) {
                throw new IllegalStateException("Seller email is missing");
            }

            ContractInfoDTO contractInfo = eversignService.createBlankContractForManualInput(
                    request.getBuyer(),
                    request.getSeller(),
                    product);

            if (contractInfo == null) {
                log.error("Eversign returned NULL contractInfo");
                throw new IllegalStateException("Eversign returned null contract info");
            }

            if (contractInfo.getContractId() == null || contractInfo.getContractId().isBlank()) {
                log.error("Eversign returned NULL or EMPTY contractId. ContractInfo: {}", contractInfo);
                throw new IllegalStateException("Eversign returned missing contract ID");
            }

            log.info("Contract created successfully - ID: {}", contractInfo.getContractId());

            request.setContractId(contractInfo.getContractId());
            request.setContractUrl(contractInfo.getContractUrl());
            request.setBuyerSignUrl(contractInfo.getBuyerSignUrl());
            request.setSellerSignUrl(contractInfo.getSellerSignUrl());
            request.setContractStatus(PurchaseRequest.ContractStatus.SENT);
            request.setStatus(PurchaseRequest.RequestStatus.CONTRACT_SENT);

            PurchaseRequest saved = purchaseRequestRepository.save(request);
            log.info("Request saved with contract info");

            try {
                eversignService.createAndSaveContractDocument(saved);
                log.info("ContractDocument saved");
            } catch (Exception e) {
                log.error("Failed to save ContractDocument (non-critical): {}", e.getMessage());
            }

            try {
                sendContractEmails(saved, contractInfo);
                log.info("Contract emails sent");
            } catch (Exception e) {
                log.warn("Failed to send emails (non-critical): {}", e.getMessage());
            }

            try {
                notificationService.createAndPush(
                        request.getBuyer().getId(),
                        "Yêu cầu mua đã được chấp nhận",
                        String.format("%s đã chấp nhận yêu cầu cho %s với giá %s. Vui lòng ký hợp đồng điện tử.",
                                request.getSeller().getFullName(),
                                request.getProduct().getTitle(),
                                CurrencyFormatter.format(request.getOfferedPrice())),
                        Notification.NotificationType.PURCHASE_REQUEST_ACCEPTED,
                        request.getId());
                log.info("Notification sent to buyer");
            } catch (Exception e) {
                log.warn("Failed to send notification (non-critical): {}", e.getMessage());
            }

            log.info("Process completed successfully for request {}", request.getId());
            return mapToResponse(saved);

        } catch (IllegalStateException e) {
            log.error("Validation/State error: {}", e.getMessage(), e);
            rollbackProductStatus(product);
            request.setContractStatus(PurchaseRequest.ContractStatus.FAILED);
            request.setStatus(PurchaseRequest.RequestStatus.CONTRACT_FAILED);
            purchaseRequestRepository.save(request);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể tạo hợp đồng điện tử: " + e.getMessage());

        } catch (AppException e) {
            log.error("Eversign API error: {} - {}", e.getErrorCode(), e.getMessage(), e);
            rollbackProductStatus(product);
            request.setContractStatus(PurchaseRequest.ContractStatus.FAILED);
            request.setStatus(PurchaseRequest.RequestStatus.CONTRACT_FAILED);
            purchaseRequestRepository.save(request);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi kết nối với dịch vụ hợp đồng điện tử. Vui lòng thử lại sau.");

        } catch (Exception e) {
            log.error("Unexpected error during contract creation for request {}: {}",
                    request.getId(), e.getMessage(), e);
            rollbackProductStatus(product);
            request.setContractStatus(PurchaseRequest.ContractStatus.FAILED);
            request.setStatus(PurchaseRequest.RequestStatus.CONTRACT_FAILED);
            purchaseRequestRepository.save(request);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Đã chấp nhận yêu cầu nhưng không thể tạo hợp đồng điện tử. Vui lòng liên hệ hỗ trợ.");
        }
    }

    private void rollbackProductStatus(Product product) {
        try {
            log.warn("Reverting product {} status back to ACTIVE", product.getId());
            product.setStatus(Product.Status.ACTIVE);
            productRepository.save(product);
            log.info("Product status reverted successfully");
        } catch (Exception e) {
            log.error("Failed to revert product status: {}", e.getMessage(), e);
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