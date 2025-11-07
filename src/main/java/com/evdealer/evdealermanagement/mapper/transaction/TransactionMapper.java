package com.evdealer.evdealermanagement.mapper.transaction;

import com.evdealer.evdealermanagement.dto.transactions.TransactionResponse;
import com.evdealer.evdealermanagement.entity.post.PostPackage;
import com.evdealer.evdealermanagement.entity.post.PostPackageOption;
import com.evdealer.evdealermanagement.entity.post.PostPayment;
import com.evdealer.evdealermanagement.entity.product.Product;

public class TransactionMapper {

    private TransactionMapper() {
    }

    public static TransactionResponse toTransactionResponse(PostPayment p) {
        if (p == null) return null;

        Product product = p.getProduct();
        PostPackage postPackage = p.getPostPackage();
        PostPackageOption opt = p.getPostPackageOption();

        Integer durationDays = null;
        if (opt != null && opt.getDurationDays() != null) {
            durationDays = opt.getDurationDays();
        } else if (postPackage != null) {
            durationDays = postPackage.getBaseDurationDays();
        }

        return TransactionResponse.builder()
                .paymentId(p.getId())
                .createdAt(p.getCreatedAt())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null)
                .packageName(postPackage != null ? postPackage.getName() : null)
                .durationDays(durationDays)
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getTitle() : null)
                .build();
    }
}
