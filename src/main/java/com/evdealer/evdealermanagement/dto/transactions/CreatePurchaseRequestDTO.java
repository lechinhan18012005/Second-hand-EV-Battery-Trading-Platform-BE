package com.evdealer.evdealermanagement.dto.transactions;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePurchaseRequestDTO {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotNull(message = "Offered price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal offeredPrice;

    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String buyerMessage;
}
