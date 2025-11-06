package com.evdealer.evdealermanagement.dto.account.review;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SellerReviewRequest {

    String purchaseRequestId;
    int rating;
    String comment;
    List<String> tags;
}
