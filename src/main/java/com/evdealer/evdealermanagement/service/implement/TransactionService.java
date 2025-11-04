package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.dto.transactions.TransactionContractResponse;
import com.evdealer.evdealermanagement.dto.transactions.TransactionPurchaseResponse;
import com.evdealer.evdealermanagement.dto.transactions.TransactionResponse;
import com.evdealer.evdealermanagement.dto.transactions.TransactionPackageResponse;
import com.evdealer.evdealermanagement.entity.post.PostPackage;
import com.evdealer.evdealermanagement.entity.post.PostPackageOption;
import com.evdealer.evdealermanagement.entity.post.PostPayment;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.entity.transactions.ContractDocument;
import com.evdealer.evdealermanagement.entity.transactions.PurchaseRequest;
import com.evdealer.evdealermanagement.repository.ContractDocumentRepository;
import com.evdealer.evdealermanagement.repository.PostPaymentRepository;
import com.evdealer.evdealermanagement.repository.ProductRepository;
import com.evdealer.evdealermanagement.repository.PurchaseRequestRepository;
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
    private final PurchaseRequestRepository purchaseRequestRepository;

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
                    .productId(p.getProduct() != null ? p.getProduct().getId() : null)
                    .productName(product != null ? product.getTitle() : null)
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionPackageResponse> getAllTransactionsBySellerId(String sellerId, Pageable pageable) {
        Page<PostPayment> page = postPaymentRepository.findByProductSellerIdOrderByCreatedAtDesc(sellerId ,pageable);

        List<TransactionPackageResponse> history = page.getContent().stream()
                .map(payment -> TransactionPackageResponse.builder()
                        .paymentId(payment.getId())
                        .createdAt(payment.getCreatedAt())
                        .amount(payment.getAmount())
                        .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                        .packageName(payment.getPostPackage() != null ? payment.getPostPackage().getName() : null)
                        .productId(payment.getProduct().getId())
                        .productName(payment.getProduct().getTitle())
                        .build())
                .toList();

        return PageResponse.of(history, page);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionPurchaseResponse> getAllTransactionsPurchaseByBuyerAndSellerId(String accountId, Pageable pageable) {
        Page<PurchaseRequest> page = purchaseRequestRepository.findCompletedTransactionsByAccountId(accountId, pageable);

        List<TransactionPurchaseResponse> history = page.getContent().stream()
                .map(p -> TransactionPurchaseResponse.builder()
                        .productId(p.getProduct().getId())
                        .productTitle(p.getProduct().getTitle())
                        .completedAt(p.getCompletedAt())
                        .build())
                .toList();

        return PageResponse.of(history, page);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionContractResponse> getAllTransactionsContractByBuyerAndSellerId(String accountId, Pageable pageable) {
        Page<ContractDocument> page = purchaseRequestRepository.findAllByAccountInvolved(accountId, pageable);

        List<TransactionContractResponse> history = page.getContent().stream()
                .map(cd -> TransactionContractResponse.builder()
                        .documentId(cd.getDocumentId())
                        .title(cd.getTitle())
                        .pdfUrl(cd.getPdfUrl())
                        .signedAt(cd.getSignedAt())
                        .build())
                .toList();

        return PageResponse.of(history, page);
    }
}
