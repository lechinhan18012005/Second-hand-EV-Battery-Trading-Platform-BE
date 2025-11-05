package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.product.detail.ProductDetail;
import com.evdealer.evdealermanagement.dto.product.status.ProductStatusResponse;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.mapper.product.ProductMapper;
import com.evdealer.evdealermanagement.repository.AccountRepository;
import com.evdealer.evdealermanagement.repository.ProductRepository;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final ProductRepository productRepository;
    private final AuthenticationManager authentication;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final EmailService emailService;


    /**
     * ✅ Lấy danh sách sản phẩm của 1 seller theo trạng thái (ACTIVE, SOLD,...)
     */
    @Transactional
    public List<ProductDetail> getProductsByStatus(String sellerId, Product.Status status) {
        return productRepository.findBySellerAndStatus(sellerId, status)
                .stream()
                .map(ProductMapper::toDetailDto) // 🧭 dùng mapper chuẩn
                .toList();
    }

    /**
     * ✅ Đánh dấu sản phẩm là đã bán (ACTIVE → SOLD)
     */
    @Transactional
    public ProductStatusResponse markSold(String memberId, String productId) {
        Product product = productRepository
                .findByIdAndSellerId(productId, memberId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Nếu đã bán rồi thì không cần làm gì thêm
        if (product.getStatus() == Product.Status.SOLD) {
            return ProductStatusResponse.builder()
                    .id(product.getId())
                    .status(product.getStatus())
                    .build();
        }

        // Chỉ cho phép đổi từ ACTIVE → SOLD
        if (product.getStatus() != Product.Status.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        product.setStatus(Product.Status.SOLD);
        product.setUpdatedAt(VietNamDatetime.nowVietNam());
        productRepository.save(product);

        return ProductStatusResponse.builder()
                .id(product.getId())
                .status(product.getStatus())
                .build();
    }

    /**
     * Lấy chi tiết sản phẩm của seller đang đăng nhập
     */
    @Transactional
    public ProductDetail getProductDetailOfMember(String sellerId, String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Chỉ cho phép xem sản phẩm thuộc seller đang đăng nhập
        if (product.getSeller() == null || !product.getSeller().getId().equals(sellerId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Dùng mapper chuẩn để convert entity → dto
        return ProductMapper.toDetailDto(product);
    }

    public long countAccountMemberInPeriod(LocalDate date) {
        ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate target = (date != null) ? date : LocalDate.now(VN);
        LocalDateTime start = target.atStartOfDay();
        LocalDateTime end = target.plusDays(1).atStartOfDay();
        return accountRepository.countCreatedInPeriod(start, end, Account.Role.MEMBER);
    }


}
