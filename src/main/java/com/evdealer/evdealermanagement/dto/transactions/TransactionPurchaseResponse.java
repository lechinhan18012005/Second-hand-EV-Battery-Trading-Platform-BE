package com.evdealer.evdealermanagement.dto.transactions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionPurchaseResponse {
    private String productId;
    private String productTitle;
    private LocalDateTime completedAt;
}
