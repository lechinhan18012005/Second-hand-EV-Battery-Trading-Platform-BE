package com.evdealer.evdealermanagement.service.implement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evdealer.evdealermanagement.dto.payment.MomoRequest;
import com.evdealer.evdealermanagement.dto.payment.VnpayRequest;
import com.evdealer.evdealermanagement.dto.product.renewal.ProductRenewalRequest;
import com.evdealer.evdealermanagement.dto.product.renewal.ProductRenewalResponse;
import com.evdealer.evdealermanagement.entity.post.PostPackage;
import com.evdealer.evdealermanagement.entity.post.PostPackageOption;
import com.evdealer.evdealermanagement.entity.post.PostPayment;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.mapper.product.ProductRenewalMapper;
import com.evdealer.evdealermanagement.repository.PostPackageOptionRepository;
import com.evdealer.evdealermanagement.repository.PostPackageRepository;
import com.evdealer.evdealermanagement.repository.PostPaymentRepository;
import com.evdealer.evdealermanagement.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductRenewalService {

    private final ProductRepository productRepository;
    private final PostPackageRepository packageRepo;
    private final PostPackageOptionRepository optionRepo;
    private final PostPaymentRepository postPaymentRepository;
    private final VnpayService vnpayService;
    private final MomoService momoService;

    @Transactional
    public ProductRenewalResponse renewalProduct(String productId, ProductRenewalRequest req) {
        log.info("=== [START] Renewal process for productId: {} ===", productId);

        // ===== Fetch product =====
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        log.debug("Fetched product: id={}, status={}", product.getId(), product.getStatus());

        // ===== Validate product status =====
        if (product.getStatus() != Product.Status.ACTIVE && product.getStatus() != Product.Status.EXPIRED) {
            log.warn("Invalid product status for renewal: {}", product.getStatus());
            throw new AppException(ErrorCode.PACKAGE_INVALID_STATUS,
                    "Only renewal when the post is ACTIVE or EXPIRED");
        }

        // ===== Validate package selection =====
        if ((req.getStandardPackageId() == null || req.getStandardPackageId().isBlank()) &&
                (req.getAddonPackageId() == null || req.getAddonPackageId().isBlank())) {
            log.warn("Renewal request missing package selection");
            throw new AppException(ErrorCode.BAD_REQUEST, "Phải chọn ít nhất một gói để gia hạn");
        }

        BigDecimal totalPayable = BigDecimal.ZERO;
        PostPackage standardPkg = null;
        PostPackage addonPkg = null;
        PostPackageOption addonOpt = null;

        Integer standardDays = null;
        Integer addonDays = null;

        // ===== STANDARD package handling =====
        if (req.getStandardPackageId() != null && !req.getStandardPackageId().isBlank()) {
            log.debug("Processing STANDARD package: {}", req.getStandardPackageId());
            standardPkg = packageRepo.findById(req.getStandardPackageId())
                    .orElseThrow(() -> new AppException(ErrorCode.PACKAGE_NOT_FOUND));

            if (!"STANDARD".equalsIgnoreCase(standardPkg.getCode())) {
                log.error("Invalid standard package code: {}", standardPkg.getCode());
                throw new AppException(ErrorCode.INVALID_ID_PACKAGE,
                        "standardPackageId must be the STANDARD package");
            }

            if (standardPkg.getStatus() != PostPackage.Status.ACTIVE) {
                log.error("Standard package is inactive: {}", standardPkg.getId());
                throw new AppException(ErrorCode.PACKAGE_INACTIVE);
            }

            BigDecimal pkgPrice = standardPkg.getPrice() != null ? standardPkg.getPrice() : BigDecimal.ZERO;
            totalPayable = totalPayable.add(pkgPrice);
            standardDays = (standardPkg.getBaseDurationDays() != null && standardPkg.getBaseDurationDays() > 0)
                    ? standardPkg.getBaseDurationDays()
                    : 30;
            log.info("Standard package validated: id={}, price={}, days={}", standardPkg.getId(), pkgPrice,
                    standardDays);
        }

        // ===== ADDON package handling =====
        if (req.getAddonPackageId() != null && !req.getAddonPackageId().isBlank()) {
            log.debug("Processing ADDON package: {}", req.getAddonPackageId());
            addonPkg = packageRepo.findById(req.getAddonPackageId())
                    .orElseThrow(() -> new AppException(ErrorCode.PACKAGE_NOT_FOUND));

            if (addonPkg.getStatus() != PostPackage.Status.ACTIVE) {
                log.error("Addon package is inactive: {}", addonPkg.getId());
                throw new AppException(ErrorCode.PACKAGE_INACTIVE);
            }

            if (req.getOptionId() == null || req.getOptionId().isBlank()) {
                log.error("Addon package missing optionId");
                throw new AppException(ErrorCode.PACKAGE_OPTION_REQUIRED);
            }

            addonOpt = optionRepo.findById(req.getOptionId())
                    .orElseThrow(() -> new AppException(ErrorCode.PACKAGE_OPTION_NOT_FOUND));

            if (!addonOpt.getPostPackage().getId().equals(addonPkg.getId())) {
                log.error("Addon option {} does not belong to package {}", addonOpt.getId(), addonPkg.getId());
                throw new AppException(ErrorCode.PACKAGE_OPTION_NOT_BELONG_TO_PACKAGE);
            }

            BigDecimal addonPrice = addonOpt.getPrice() != null ? addonOpt.getPrice() : BigDecimal.ZERO;
            totalPayable = totalPayable.add(addonPrice);
            addonDays = (addonOpt.getDurationDays() != null ? addonOpt.getDurationDays() : 0);
            log.info("Addon package validated: id={}, optionId={}, price={}, days={}",
                    addonPkg.getId(), addonOpt.getId(), addonPrice, addonDays);
        }

        // ===== Validate days relationship =====
        if (standardDays != null && addonDays != null && addonDays >= standardDays) {
            log.error("Addon days ({}) >= Standard days ({})", addonDays, standardDays);
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "The number of days of the priority/special package must be less than the STANDARD package");
        }

        // ===== Prepare payment record =====
        PostPackage paymentPkg = (standardPkg != null) ? standardPkg : addonPkg;
        log.debug("Creating PostPayment for accountId={}, totalPayable={}", product.getSeller().getId(), totalPayable);

        PostPayment payment = PostPayment.builder()
                .accountId(product.getSeller().getId())
                .product(product)
                .postPackage(paymentPkg)
                .postPackageOption(addonOpt)
                .amount(totalPayable)
                .paymentMethod(req.getPaymentMethod() != null
                        ? PostPayment.PaymentMethod.valueOf(req.getPaymentMethod().toUpperCase())
                        : null)
                .paymentStatus(totalPayable.signum() == 0
                        ? PostPayment.PaymentStatus.COMPLETED
                        : PostPayment.PaymentStatus.PENDING)
                .createdAt(VietNamDatetime.nowVietNam())
                .build();

        postPaymentRepository.save(payment);
        log.info("Saved PostPayment: id={}, status={}, method={}",
                payment.getId(), payment.getPaymentStatus(), payment.getPaymentMethod());

        // ===== Payment gateway integration =====
        String paymentUrl = null;
        if (totalPayable.signum() > 0 && req.getPaymentMethod() != null) {
            long amountVND = totalPayable.setScale(0, RoundingMode.HALF_UP).longValue();
            String method = req.getPaymentMethod().toUpperCase();
            log.info("Initiating payment via {} for amount: {}", method, amountVND);

            try {
                if ("VNPAY".equals(method)) {
                    paymentUrl = vnpayService.createPayment(
                            new VnpayRequest(payment.getId(), String.valueOf(amountVND))).getPaymentUrl();
                } else if ("MOMO".equals(method)) {
                    paymentUrl = momoService.createPaymentRequest(
                            new MomoRequest(payment.getId(), String.valueOf(amountVND))).getPayUrl();
                } else {
                    log.error("Unsupported payment method: {}", req.getPaymentMethod());
                    throw new IllegalArgumentException("Unsupported payment method: " + req.getPaymentMethod());
                }
                log.info("Payment URL created successfully: {}", paymentUrl);
            } catch (Exception e) {
                log.error("Payment creation failed via {}: {}", method, e.getMessage(), e);
                throw e;
            }
        }

        log.info("=== [END] Renewal completed for productId: {} | totalPayable={} ===", productId, totalPayable);
        return ProductRenewalMapper.mapToProductRenewalResponse(product, totalPayable, paymentUrl);
    }

    @Transactional
    public void handlePaymentCallbackFromRenewal(String paymentId, boolean success) {
        log.info("🔄 Processing payment callback - PaymentId: {}, Success: {}", paymentId, success);

        // ===== Lấy payment & product =====
        PostPayment payment = postPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        Product product = productRepository.findById(payment.getProduct().getId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // ===== Kiểm tra trạng thái thanh toán =====
        if (payment.getPaymentStatus() == PostPayment.PaymentStatus.COMPLETED ||
                payment.getPaymentStatus() == PostPayment.PaymentStatus.FAILED) {
            log.warn("Payment already processed with status: {}", payment.getPaymentStatus());
            return;
        }

        // ===== Nếu thanh toán thất bại =====
        if (!success) {
            payment.setPaymentStatus(PostPayment.PaymentStatus.FAILED);
            if (product.getStatus() == Product.Status.ACTIVE) {
                product.setStatus(Product.Status.ACTIVE);
            } else {
                product.setStatus(Product.Status.EXPIRED);
            }
            postPaymentRepository.save(payment);
            productRepository.save(product);
            log.warn("Payment failed - reverted to DRAFT if new post");
            return;
        }

        // ===== Thanh toán thành công =====
        payment.setPaymentStatus(PostPayment.PaymentStatus.COMPLETED);
        product.setPostingFee(product.getPostingFee() == null
                ? payment.getAmount()
                : product.getPostingFee().add(payment.getAmount()));

        LocalDateTime now = VietNamDatetime.nowVietNam();

        int featuredDays = 0;

        // Nếu có option → cộng thêm ngày featured
        if (payment.getPostPackageOption() != null) {
            Integer d = payment.getPostPackageOption().getDurationDays();
            featuredDays = (d != null ? d : 0);
        }

        // ===== Cập nhật hạn featured & hạn đăng tin =====
        LocalDateTime currentFeatured = product.getFeaturedEndAt();
        LocalDateTime baseFeatured = (currentFeatured != null && currentFeatured.isAfter(now))
                ? currentFeatured
                : now;

        if (featuredDays > 0) {
            product.setFeaturedEndAt(baseFeatured.plusDays(featuredDays));
        }

        boolean extendExpire = shouldExtendExpire(payment); // true nếu STANDARD

        if (extendExpire) {
            // ====== CASE STANDARD (có gia hạn chu kỳ) ======
            LocalDateTime currentExpire = product.getExpiresAt();
            LocalDateTime renewalStart = (currentExpire != null && currentExpire.isAfter(now))
                    ? currentExpire
                    : now;

            product.setStartRenewalAt(renewalStart);
            product.setExpiresAt(renewalStart.plusDays(30)); // vẫn cộng 30d như bạn yêu cầu
            // Không đụng updatedAt ngay; scheduler sẽ đẩy top tại renewalStart
            log.info("STANDARD renew: startRenewalAt={}, new expiresAt={}",
                    product.getStartRenewalAt(), product.getExpiresAt());
        } else {
            // ====== CASE ADDON-ONLY (PRIORITY/SPECIAL) ======
            // Không gia hạn expiresAt, không dùng startRenewalAt
            product.setStartRenewalAt(null);

            // Đẩy top NGAY lập tức vì mục tiêu là ưu tiên hiển thị hiện thời
            product.setUpdatedAt(now);

            log.info("ADDON-only renew: bumped updatedAt immediately to {}", product.getUpdatedAt());
        }

        // ===== Cập nhật trạng thái bài đăng =====
        if (product.getStatus() == Product.Status.ACTIVE ||
                product.getStatus() == Product.Status.EXPIRED) {
            product.setStatus(Product.Status.ACTIVE);
        }

        // ===== Lưu thay đổi =====
        postPaymentRepository.save(payment);
        productRepository.save(product);

        log.info("""
                   Payment COMPLETED:
                 - Product: {}
                 - New Status: {}
                 - Expires At: {}
                 - Featured Until: {}
                 - Total Fee: {}
                """,
                product.getId(), product.getStatus(),
                product.getExpiresAt(), product.getFeaturedEndAt(),
                product.getPostingFee());
    }

    private boolean shouldExtendExpire(PostPayment p) {
        PostPackage pkg = p.getPostPackage();
        if (pkg == null)
            return false;
        return "STANDARD".equalsIgnoreCase(pkg.getCode());
    }
}
