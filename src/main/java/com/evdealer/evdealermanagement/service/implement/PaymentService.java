package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.payment.MomoRequest;
import com.evdealer.evdealermanagement.dto.payment.MomoResponse;
import com.evdealer.evdealermanagement.dto.payment.VnpayRequest;
import com.evdealer.evdealermanagement.dto.payment.VnpayResponse;
import com.evdealer.evdealermanagement.dto.post.packages.PackageRequest;
import com.evdealer.evdealermanagement.dto.post.packages.PackageResponse;
import com.evdealer.evdealermanagement.dto.post.packages.PostPackageOptionResponse;
import com.evdealer.evdealermanagement.dto.post.packages.PostPackageResponse;
import com.evdealer.evdealermanagement.entity.post.PostPackage;
import com.evdealer.evdealermanagement.entity.post.PostPackageOption;
import com.evdealer.evdealermanagement.entity.post.PostPayment;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.repository.PostPackageOptionRepository;
import com.evdealer.evdealermanagement.repository.PostPackageRepository;
import com.evdealer.evdealermanagement.repository.PostPaymentRepository;
import com.evdealer.evdealermanagement.repository.ProductRepository;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final ProductRepository productRepository;
    private final PostPackageRepository packageRepo;
    private final PostPackageOptionRepository optionRepo;
    private final PostPaymentRepository postPaymentRepository;
    private final VnpayService vnpayService;
    private final MomoService momoService;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private LocalDateTime nowVietNam() {
        return ZonedDateTime.now(VIETNAM_ZONE).toLocalDateTime();
    }

    @Transactional
    public PackageResponse choosePackage(String productId, PackageRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != Product.Status.DRAFT) {
            throw new AppException(ErrorCode.PRODUCT_NOT_DRAFT);
        }

        PostPackage pkg = packageRepo.findById(request.getPackageId()) // THƯỜNG OR ĐB
                .orElseThrow(() -> new AppException(ErrorCode.PACKAGE_NOT_FOUND));

        if (pkg.getStatus() != PostPackage.Status.ACTIVE) {
            throw new AppException(ErrorCode.PACKAGE_INACTIVE);
        }

        BigDecimal totalPayable;

        if (pkg.getBillingMode() == PostPackage.BillingMode.FIXED) {
            totalPayable = pkg.getPrice(); // 10.000đ
        } else if (pkg.getBillingMode() == PostPackage.BillingMode.PER_DAY) {
            if (request.getOptionId() == null) {
                throw new AppException(ErrorCode.PACKAGE_OPTION_REQUIRED);
            }
            PostPackageOption option = optionRepo.findById(request.getOptionId())
                    .orElseThrow(() -> new AppException(ErrorCode.PACKAGE_OPTION_NOT_FOUND));

            if (!option.getPostPackage().getId().equals(pkg.getId())) {
                throw new AppException(ErrorCode.PACKAGE_OPTION_NOT_BELONG_TO_PACKAGE);
            }

            totalPayable = option.getPrice().add(packageRepo.getPriceByCode("STANDARD", PostPackage.Status.ACTIVE));
        } else {
            throw new AppException(ErrorCode.PACKAGE_BILLING_MODE_INVALID);
        }

        boolean isFirstPost = !postPaymentRepository.existsByAccountIdAndPaymentStatus(
                product.getSeller().getId(),
                PostPayment.PaymentStatus.COMPLETED);

        if ("STANDARD".equalsIgnoreCase(pkg.getCode()) && isFirstPost) {
            totalPayable = BigDecimal.ZERO;
        }

        PostPayment payment = PostPayment.builder()
                .accountId(product.getSeller().getId())
                .product(product)
                .postPackage(pkg)
                .postPackageOption(optionRepo.findById(request.getOptionId()).orElse(null))
                .amount(totalPayable)
                .paymentMethod(request.getPaymentMethod() != null
                        ? PostPayment.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase())
                        : null)
                .paymentStatus(totalPayable.signum() == 0
                        ? PostPayment.PaymentStatus.COMPLETED
                        : PostPayment.PaymentStatus.PENDING)
                .createdAt(nowVietNam())
                .build();

        postPaymentRepository.save(payment);
        log.info("Payment saved with ID: {}", payment.getId());

        // update product status
        product.setStatus(totalPayable.signum() == 0
                ? Product.Status.PENDING_REVIEW
                : Product.Status.PENDING_PAYMENT);
        productRepository.save(product);

        String paymentUrl = null;
        if (totalPayable.signum() > 0 && request.getPaymentMethod() != null) {
            long amountVND = totalPayable.setScale(0, RoundingMode.HALF_UP).longValue();
            switch (request.getPaymentMethod().toUpperCase()) {
                case "VNPAY":
                    if (amountVND < 10000)
                        throw new AppException(ErrorCode.USER_NOT_FOUND);
                    VnpayResponse vnpayResponse = vnpayService.createPayment(
                            new VnpayRequest(payment.getId(), String.valueOf(amountVND)));
                    paymentUrl = vnpayResponse.getPaymentUrl();
                    break;
                case "MOMO":
                    MomoResponse momoResponse = momoService.createPaymentRequest(
                            new MomoRequest(payment.getId(), String.valueOf(amountVND)));
                    paymentUrl = momoResponse.getPayUrl();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported payment method: " + request.getPaymentMethod());
            }
        }

        return PackageResponse.builder()
                .productId(product.getId())
                .status(product.getStatus())
                .totalPayable(totalPayable)
                .currency("VND")
                .paymentUrl(paymentUrl)
                .build();
    }

    @Transactional
    public void handlePaymentCallback(String paymentId, boolean success) {
        log.info("Processing payment callback - PaymentId: {}, Success: {}", paymentId, success);

        PostPayment payment = postPaymentRepository.findById(paymentId)
                .orElseThrow(() -> {
                    log.error("Payment not found: {}", paymentId);
                    return new AppException(ErrorCode.PAYMENT_NOT_FOUND);
                });

        log.info("Payment found: ID={}, Status={}, Amount={}",
                payment.getId(), payment.getPaymentStatus(), payment.getAmount());

        Product product = productRepository.findById(payment.getProduct().getId())
                .orElseThrow(() -> {
                    log.error("Product not found: {}", payment.getProduct().getId());
                    return new AppException(ErrorCode.PRODUCT_NOT_FOUND);
                });

        log.info("Product found: ID={}, Status={}", product.getId(), product.getStatus());

        // Skip nếu đã xử lý rồi
        if (payment.getPaymentStatus() == PostPayment.PaymentStatus.COMPLETED ||
                payment.getPaymentStatus() == PostPayment.PaymentStatus.FAILED) {
            log.warn("Payment already processed with status: {}", payment.getPaymentStatus());
            return;
        }

        if (product.getStatus() == Product.Status.PENDING_PAYMENT) {
            if (success) {
                log.info("Payment successful - Updating to COMPLETED");
                payment.setPaymentStatus(PostPayment.PaymentStatus.COMPLETED);

                if (product.getPostingFee() == null) {
                    product.setPostingFee(payment.getAmount());
                } else {
                    product.setPostingFee(product.getPostingFee().add(payment.getAmount()));
                }
                product.setStatus(Product.Status.PENDING_REVIEW);

                log.info("Posting fee updated: {}", product.getPostingFee());
                log.info("Product status updated: {}", product.getStatus());
            } else {
                log.info("Payment failed - Updating to FAILED");
                payment.setPaymentStatus(PostPayment.PaymentStatus.FAILED);
                product.setStatus(Product.Status.PENDING_PAYMENT);
            }
        } else {
            log.warn(" Product is not in PENDING_PAYMENT status: {}", product.getStatus());
        }

        postPaymentRepository.save(payment);
        productRepository.save(product);

        log.info(" Payment and Product saved successfully");
        log.info(" Final - Payment status: {}, Product status: {}",
                payment.getPaymentStatus(), product.getStatus());
    }

    public List<PostPackageResponse> getAllPackages() {
        var packages = packageRepo.findByStatusOrderByPriorityLevelDesc(PostPackage.Status.ACTIVE);

        return packages.stream().map(p -> {
            List<PostPackageOptionResponse> optionResponses = optionRepo
                    .findByPostPackage_IdAndStatusOrderBySortOrderAsc(p.getId(), PostPackageOption.Status.ACTIVE)
                    .stream()
                    .map(o -> PostPackageOptionResponse.builder()
                            .id(o.getId())
                            .name(o.getName())
                            .durationDays(o.getDurationDays())
                            .price(o.getPrice())
                            .listPrice(o.getListPrice())
                            .isDefault(o.getIsDefault())
                            .sortOrder(o.getSortOrder())
                            .build())
                    .toList();

            String note = "STANDARD".equals(p.getCode()) ? "Miễn phí lần đăng đầu tiên" : null;

            return PostPackageResponse.builder()
                    .postPackageId(p.getId())
                    .postPackageCode(p.getCode())
                    .postPackageName(p.getName())
                    .postPackageDesc(p.getDescription())
                    .billingMode(p.getBillingMode())
                    .category(p.getCategory())
                    .baseDurationDays(p.getBaseDurationDays())
                    .price(p.getPrice())
                    .dailyPrice(p.getDailyPrice())
                    .includesPostFee(p.getIncludesPostFee())
                    .priorityLevel(p.getPriorityLevel())
                    .badgeLabel(p.getBadgeLabel())
                    .showInLatest(p.getShowInLatest())
                    .showTopSearch(p.getShowTopSearch())
                    .listPrice(p.getListPrice())
                    .isDefault(p.getIsDefault())
                    .note(note)
                    .options(optionResponses)
                    .build();
        }).toList();
    }

    @Transactional
    public VnpayResponse retryVnpayPayment(String paymentId) {
        log.info("Retrying VNPay payment for ID: {}", paymentId);

        PostPayment payment = postPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getPaymentStatus() != PostPayment.PaymentStatus.FAILED) {
            log.warn("Payment {} is not FAILED, cannot retry", paymentId);
            throw new AppException(ErrorCode.PAYMENT_NOT_RETRIABLE);
        }

        Product product = productRepository.findById(payment.getProduct().getId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Reset trạng thái để cho phép retry
        payment.setPaymentStatus(PostPayment.PaymentStatus.PENDING);
        product.setStatus(Product.Status.PENDING_PAYMENT);

        long amountVND = payment.getAmount().setScale(0, RoundingMode.HALF_UP).longValue();

        // Tạo URL thanh toán mới qua VNPay
        VnpayResponse vnpayResponse = vnpayService.createPayment(
                new VnpayRequest(payment.getId(), String.valueOf(amountVND)));

        postPaymentRepository.save(payment);
        productRepository.save(product);

        log.info("Retry payment URL created successfully for {}", paymentId);

        return vnpayResponse;
    }

    @Transactional
    public PackageResponse retryPackagePayment(String productId) {
        log.info("Retrying payment for product ID: {}", productId);

        PostPayment payment = postPaymentRepository.findLatestUncompletedByProductId(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        Product product = payment.getProduct();

        if(product.getStatus() != Product.Status.PENDING_PAYMENT) {
            log.warn("Product {} is not PENDING_PAYMENT, cannot retry", productId);
            throw new AppException(ErrorCode.PRODUCT_NOT_PENDING_PAYMENT);
        }

        if(payment.getPaymentStatus() == PostPayment.PaymentStatus.COMPLETED) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        PostPayment.PaymentMethod paymentMethod = payment.getPaymentMethod();
        if (paymentMethod == null) {
            throw new AppException(ErrorCode.PAYMENT_METHOD_MISSING);
        }
        long amount =  payment.getAmount().setScale(0, RoundingMode.HALF_UP).longValue();

        String paymentUrl;
        switch (paymentMethod) {
            case MOMO -> {
                MomoResponse momoResponse = momoService.createPaymentRequest(
                        new MomoRequest(payment.getId(), String.valueOf(amount))
                );
                paymentUrl = momoResponse.getPayUrl();
                log.info("New Momo payment URL generated {}", paymentUrl);
            }
            case VNPAY -> {
                if(amount < 10000) {
                    throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
                }
                VnpayResponse vnpayResponse = vnpayService.createPayment(
                        new VnpayRequest(payment.getId(), String.valueOf(amount))
                );
                paymentUrl = vnpayResponse.getPaymentUrl();
                log.info("New Vnpay payment URL generated {}", paymentUrl);
            }
            default -> throw new AppException(ErrorCode.PAYMENT_METHOD_UNSUPPORTED);
        }
        payment.setPaymentStatus(PostPayment.PaymentStatus.PENDING);
        payment.setCreatedAt(VietNamDatetime.nowVietNam());
        postPaymentRepository.save(payment);

        log.info("Payment {} set to PENDING and saved successfully.", payment.getId());

        return PackageResponse.builder()
                .productId(product.getId())
                .status(product.getStatus())
                .totalPayable(payment.getAmount())
                .currency("VND")
                .paymentUrl(paymentUrl)
                .build();
    }

}