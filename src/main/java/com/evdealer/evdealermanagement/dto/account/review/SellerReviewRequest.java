package com.evdealer.evdealermanagement.dto.account.review;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SellerReviewRequest {

    @NotBlank(message = "Product ID không được để trống")
    String productId;

    @Min(value = 1, message = "Rating phải từ 1 đến 5")
    @Max(value = 5, message = "Rating phải từ 1 đến 5")
    int rating;

    @Size(min = 10, max = 500, message = "Comment phải từ 10 đến 500 ký tự")
    String comment;

    @Size(max = 5, message = "Tối đa 5 tags")
    List<String> tags;
}
