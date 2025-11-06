package com.evdealer.evdealermanagement.dto.account.review;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SellerReviewResponse {

    String id;
    String sellerId;
    String sellerName;
    String buyerId;
    String buyerName;
    String productId;
    int rating;
    String comment;
    List<String> tags;
    LocalDateTime createdAt;
}
