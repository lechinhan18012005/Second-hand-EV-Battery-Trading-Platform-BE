package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.dto.payment.TransactionResponse;
import com.evdealer.evdealermanagement.entity.post.PostPackage;
import com.evdealer.evdealermanagement.entity.post.PostPackageOption;
import com.evdealer.evdealermanagement.entity.post.PostPayment;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.repository.PostPaymentRepository;
import com.evdealer.evdealermanagement.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final PostPaymentRepository postPaymentRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getAllTransactions(Pageable pageable) {
        // Nếu đã set sort trong @PageableDefault thì chỉ cần findAll(pageable)
        Page<PostPayment> page = postPaymentRepository.findAll(pageable);

        return PageResponse.fromPage(page, p -> {
            Product product = p.getProduct(); // Nên preload để tránh N+1
            PostPackage postPackage = p.getPostPackage();
            PostPackageOption opt = p.getPostPackageOption();

            Integer durationDays = (opt != null && opt.getDurationDays() != null)
                    ? opt.getDurationDays()
                    : (postPackage != null ? postPackage.getBaseDurationDays() : null);

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
        });
    }
}
