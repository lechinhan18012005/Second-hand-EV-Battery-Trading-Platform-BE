package com.evdealer.evdealermanagement.controller.payment;

import com.evdealer.evdealermanagement.dto.account.custom.CustomAccountDetails;
import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.dto.transactions.TransactionContractResponse;
import com.evdealer.evdealermanagement.dto.transactions.TransactionPackageResponse;
import com.evdealer.evdealermanagement.dto.transactions.TransactionPurchaseResponse;
import com.evdealer.evdealermanagement.dto.transactions.TransactionResponse;
import com.evdealer.evdealermanagement.service.implement.TransactionService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/show")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public PageResponse<TransactionResponse> getAllTransactions(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return transactionService.getAllTransactions(pageable);
    }

    @GetMapping("/package/history")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<PageResponse<TransactionPackageResponse>> getTransactionPackageHistory(
            @AuthenticationPrincipal CustomAccountDetails accountDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
            ) {
        return ResponseEntity.ok(transactionService.getAllTransactionsBySellerId(accountDetails.getAccountId(), pageable));
    }

    @GetMapping("/purchase/history")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<PageResponse<TransactionPurchaseResponse>> getTransactionPurchaseHistory(
            @AuthenticationPrincipal CustomAccountDetails accountDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
            ) {
        return ResponseEntity.ok(transactionService.getAllTransactionsPurchaseByBuyerAndSellerId(accountDetails.getAccountId(), pageable));
    }

    @GetMapping("/contract/history")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<PageResponse<TransactionContractResponse>> getTransactionContractHistory(
            @AuthenticationPrincipal CustomAccountDetails accountDetails,
            @PageableDefault(size = 10, sort = "signedAt", direction = Sort.Direction.DESC) Pageable pageable
            ) {
        return ResponseEntity.ok(transactionService.getAllTransactionsContractByBuyerAndSellerId(accountDetails.getAccountId(), pageable));
    }
}
