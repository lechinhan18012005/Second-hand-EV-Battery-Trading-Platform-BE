package com.evdealer.evdealermanagement.dto.transactions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionPackageResponse {
    private String paymentId;
    private LocalDateTime createdAt;
    private BigDecimal amount;
    private String paymentMethod;
    private String packageName;
    private String productId;
    private String productName;
}
