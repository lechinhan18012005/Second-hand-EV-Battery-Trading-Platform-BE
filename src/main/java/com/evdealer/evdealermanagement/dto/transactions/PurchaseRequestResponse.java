package com.evdealer.evdealermanagement.dto.transactions;

import com.evdealer.evdealermanagement.entity.transactions.PurchaseRequest;
import com.evdealer.evdealermanagement.utils.PriceSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestResponse {
    private String id;
    private String productId;
    private String productTitle;

    @JsonSerialize(using = PriceSerializer.class)
    private BigDecimal productPrice;

    private String buyerId;
    private String buyerName;
    private String buyerEmail;
    private String buyerPhone;

    private String sellerId;
    private String sellerName;
    private String sellerEmail;
    private String sellerPhone;

    @JsonSerialize(using = PriceSerializer.class)
    private BigDecimal offeredPrice;

    private String buyerMessage;
    private String sellerResponseMessage;

    private String status;
    private String contractStatus;
    private String contractUrl;
    private String contractId;

    private String rejectReason;

    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private LocalDateTime buyerSignedAt;
    private LocalDateTime sellerSignedAt;

    private boolean hasPurchaseRequested;

    public static PurchaseRequestResponse fromEntity(PurchaseRequest request) {
        if (request == null) return null;

        return PurchaseRequestResponse.builder()
                .id(request.getId())
                .productId(request.getProduct() != null ? request.getProduct().getId() : null)
                .productTitle(request.getProduct() != null ? request.getProduct().getTitle() : null)
                .productPrice(request.getProduct() != null ? request.getProduct().getPrice() : null)

                .buyerId(request.getBuyer() != null ? request.getBuyer().getId() : null)
                .buyerName(request.getBuyer() != null ? request.getBuyer().getFullName() : null)
                .buyerEmail(request.getBuyer() != null ? request.getBuyer().getEmail() : null)
                .buyerPhone(request.getBuyer() != null ? request.getBuyer().getPhone() : null)

                .sellerId(request.getProduct() != null && request.getProduct().getSeller() != null ? request.getProduct().getSeller().getId() : null)
                .sellerName(request.getProduct() != null && request.getProduct().getSeller() != null ? request.getProduct().getSeller().getFullName() : null)
                .sellerEmail(request.getProduct() != null && request.getProduct().getSeller() != null ? request.getProduct().getSeller().getEmail() : null)
                .sellerPhone(request.getProduct() != null && request.getProduct().getSeller() != null ? request.getProduct().getSeller().getPhone() : null)

                .offeredPrice(request.getOfferedPrice())
                .buyerMessage(request.getBuyerMessage())
                .sellerResponseMessage(request.getSellerResponseMessage())

                .status(request.getStatus() != null ? request.getStatus().name() : null)
                .contractStatus(request.getContractStatus() != null ? request.getContractStatus().name() : null)
                .contractUrl(request.getContractUrl())
                .contractId(request.getContractId())

                .rejectReason(request.getRejectReason())

                .createdAt(request.getCreatedAt())
                .respondedAt(request.getRespondedAt())
                .buyerSignedAt(request.getBuyerSignedAt())
                .sellerSignedAt(request.getSellerSignedAt())
                .build();
    }
}