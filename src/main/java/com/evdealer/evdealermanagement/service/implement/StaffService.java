package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.dto.post.verification.PostVerifyResponse;
import com.evdealer.evdealermanagement.dto.rate.ApprovalRateResponse;
import com.evdealer.evdealermanagement.dto.vehicle.catalog.VehicleCatalogDTO;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.entity.post.PostPackage;
import com.evdealer.evdealermanagement.entity.post.PostPayment;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.entity.transactions.ContractDocument;
import com.evdealer.evdealermanagement.entity.transactions.PurchaseRequest;
import com.evdealer.evdealermanagement.entity.transactions.TransactionsHistory;
import com.evdealer.evdealermanagement.entity.vehicle.Model;
import com.evdealer.evdealermanagement.entity.vehicle.ModelVersion;
import com.evdealer.evdealermanagement.entity.vehicle.VehicleBrands;
import com.evdealer.evdealermanagement.entity.vehicle.VehicleCatalog;
import com.evdealer.evdealermanagement.entity.vehicle.VehicleCategories;
import com.evdealer.evdealermanagement.entity.vehicle.VehicleDetails;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.mapper.post.PostVerifyMapper;
import com.evdealer.evdealermanagement.mapper.staff.ApprovalRateMapper;
import com.evdealer.evdealermanagement.mapper.vehicle.VehicleCatalogMapper;
import com.evdealer.evdealermanagement.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class StaffService {

    private final AccountRepository accountRepository;

    private final ProductRepository productRepository;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserContextService userContextService;

    private final PostPaymentRepository postPaymentRepository;

    private final VehicleCatalogRepository vehicleCatalogRepository;

    private final GeminiRestService geminiRestService;

    private final VehicleDetailsRepository vehicleDetailsRepository;

    private final PurchaseRequestRepository purchaseRequestRepository;

    private final ContractDocumentRepository contractDocumentRepository;

    private LocalDateTime nowVietNam() {
        return ZonedDateTime.now(VIETNAM_ZONE).toLocalDateTime();
    }

    @Transactional
    public PostVerifyResponse verifyPostActive(String productId) {
        log.info("Starting verifyPostActive for productId={}", productId);

        // Lấy user hiện tại
        Account currentUser = userContextService.getCurrentUser()
                .orElseThrow(() -> {
                    log.warn("Unauthorized access attempt while verifying productId={}", productId);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized user");
                });
        log.debug("Current user: id={}, role={}", currentUser.getId(), currentUser.getRole());

        // Kiểm tra quyền hạn
        if (currentUser.getRole() != Account.Role.STAFF && currentUser.getRole() != Account.Role.ADMIN) {
            log.warn("User id={} with role={} tried to verify productId={} without permission",
                    currentUser.getId(), currentUser.getRole(), productId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only STAFF or ADMIN can verify posts");
        }

        // Lấy product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found: id={}", productId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
                });
        log.debug("Loaded product: id={}, title='{}', status={}", product.getId(), product.getTitle(),
                product.getStatus());

        // Kiểm tra trạng thái bài đăng
        if (product.getStatus() != Product.Status.PENDING_REVIEW) {
            log.warn("Product id={} has invalid status={} for verification", productId, product.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only posts in PENDING_REVIEW status can be verified or rejected");
        }

        // Kiểm tra thanh toán hoàn tất
        PostPayment payment = postPaymentRepository
                .findTopByProductIdAndPaymentStatusOrderByCreatedAtDesc(
                        product.getId(), PostPayment.PaymentStatus.COMPLETED)
                .orElseThrow(() -> {
                    log.warn("No completed payment found for productId={}", productId);
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "No completed payment found for this product");
                });
        log.debug("Found completed payment: id={}, packageOptionId={}, amount={}",
                payment.getId(),
                payment.getPostPackageOption() != null ? payment.getPostPackageOption().getId() : null,
                payment.getAmount());

        // Tính thời gian hiển thị
        int elevatedDays = 0;
        if (payment.getPostPackageOption() != null) {
            Integer d = payment.getPostPackageOption().getDurationDays();
            elevatedDays = (d != null ? d : 0);
        }
        log.debug("Elevated days from package option: {}", elevatedDays);

        LocalDateTime now = nowVietNam();
        product.setFeaturedEndAt(elevatedDays > 0 ? now.plusDays(elevatedDays) : null);
        product.setExpiresAt(now.plusDays(30));
        product.setUpdatedAt(now);

        // Thay đổi status và set approver
        product.setStatus(Product.Status.ACTIVE);
        product.setRejectReason(null);
        product.setApprovedBy(currentUser);
        PostPackage postPackage = payment.getPostPackage();
        boolean isHot = false;

        if (postPackage != null) {
            if ("HOT".equalsIgnoreCase(postPackage.getBadgeLabel()) || Boolean.TRUE.equals(postPackage.getShowTopSearch())) {
                isHot = true;
            }
        }
        product.setIsHot(isHot);
        log.info("Verifying product id={} by user id={}, elevatedDays={}, expiresAt={}",
                product.getId(), currentUser.getId(), elevatedDays, product.getExpiresAt());

        // Xử lý thông số kỹ thuật xe (nếu là sản phẩm xe)
        if (isVehicleProduct(product)) {
            log.debug("Product id={} is a vehicle product -> generating vehicle specs", product.getId());
            try {
                generateAndSaveVehicleSpecs(product);
                log.debug("Vehicle specs generated successfully for product id={}", product.getId());
            } catch (Exception e) {
                log.error("Error while generating vehicle specs for product id={}: {}", product.getId(), e.getMessage(),
                        e);
                throw e;
            }
        } else {
            log.debug("Product id={} is not a vehicle product, skipping specs generation", product.getId());
        }

        // Lưu product
        Product savedProduct = productRepository.save(product);
        log.info("Product approved successfully: id={}, status={}, featuredEndAt={}, expiresAt={}, isHot: {}",
                savedProduct.getId(),
                savedProduct.getStatus(),
                savedProduct.getFeaturedEndAt(),
                savedProduct.getExpiresAt(),
                savedProduct.getIsHot());

        PostVerifyResponse response = PostVerifyMapper.mapToPostVerifyResponse(savedProduct, payment);
        log.debug("Returning PostVerifyResponse: {}", response);

        return response;
    }

    private boolean isVehicleProduct(Product product) {
        return product.getType() != null && "VEHICLE".equals(product.getType().name());
    }

    @Transactional
    public PostVerifyResponse verifyPostReject(String productId, String rejectReason) {
        log.info("Starting verifyPostReject for productId={} with rejectReason='{}'", productId, rejectReason);

        // Lấy user hiện tại
        Account currentUser = userContextService.getCurrentUser()
                .orElseThrow(() -> {
                    log.warn("Unauthorized access attempt while rejecting productId={}", productId);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized user");
                });
        log.debug("Current user: id={}, role={}", currentUser.getId(), currentUser.getRole());

        // Kiểm tra quyền hạn
        if (currentUser.getRole() != Account.Role.STAFF && currentUser.getRole() != Account.Role.ADMIN) {
            log.warn("User id={} with role={} tried to reject productId={} without permission",
                    currentUser.getId(), currentUser.getRole(), productId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only STAFF or ADMIN can reject posts");
        }

        // Lấy product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("PRODUCT_NOT_FOUND: productId={}", productId);
                    return new AppException(ErrorCode.PRODUCT_NOT_FOUND);
                });
        log.debug("Loaded product: id={}, title='{}', currentStatus={}", product.getId(), product.getTitle(),
                product.getStatus());

        // Kiểm tra trạng thái
        if (product.getStatus() != Product.Status.PENDING_REVIEW) {
            log.warn("Invalid product status={} for rejecting productId={}", product.getStatus(), productId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only posts in PENDING_REVIEW status can be rejected");
        }

        // Lấy thông tin thanh toán gần nhất (nếu có)
        PostPayment payment = postPaymentRepository
                .findTopByProductIdAndPaymentStatusOrderByIdDesc(
                        product.getId(),
                        PostPayment.PaymentStatus.COMPLETED)
                .orElse(null);

        if (payment != null) {
            log.debug("Found completed payment for productId={} (paymentId={}, amount={})",
                    product.getId(), payment.getId(), payment.getAmount());
        } else {
            log.debug("No completed payment found for productId={}", product.getId());
        }

        // Set các trường cần thiết
        LocalDateTime now = nowVietNam();
        product.setStatus(Product.Status.REJECTED);
        product.setRejectReason(rejectReason);
        product.setUpdatedAt(now);
        product.setApprovedBy(currentUser);
        log.info("Rejecting productId={} by userId={} (email={}) at {}",
                productId, currentUser.getId(), currentUser.getEmail(), now);

        // Lưu product
        Product savedProduct = productRepository.save(product);
        log.info("Product rejected successfully: id={}, status={}, reason='{}'",
                savedProduct.getId(), savedProduct.getStatus(), rejectReason);

        // Trả response
        PostVerifyResponse response = PostVerifyMapper.mapToPostVerifyResponse(savedProduct, payment);
        log.debug("Returning PostVerifyResponse: {}", response);

        return response;
    }

    // Generate và Lưu thông số kỹ thuật
    private void generateAndSaveVehicleSpecs(Product product) {
        // Lấy VehicleDetails
        VehicleDetails details = vehicleDetailsRepository.findByProductId(product.getId()).orElse(null);
        ModelVersion version = details.getVersion();

        if (version == null || version.getModel() == null) {
            log.warn(" Product ID {} is missing ModelVersion or Model. Cannot generate specs.", product.getId());
            return;
        }

        if (details == null) {
            log.warn("Product ID {} is missing VehicleDetails. Cannot link catalog.", product.getId());
            return;
        }

        // Lấy thông tin Model, Brand, Category
        Model model = version.getModel();
        VehicleBrands brand = model.getBrand();
        VehicleCategories type = model.getVehicleType();

        // Validation các trường bắt buộc
        if (type == null) {
            log.error(" Model ID {} is missing VehicleType. Cannot generate specs.", model.getId());
            return;
        }

        if (brand == null) {
            log.error(" Model ID {} is missing Brand. Cannot generate specs.", model.getId());
            return;
        }

        // Chuẩn bị dữ liệu cho Gemini
        String productName = product.getTitle();
        String modelName = model.getName();
        String brandName = brand.getName();
        String versionName = version.getName();
        Short manufactureYear = product.getManufactureYear();

        if (manufactureYear == null) {
            log.warn("Product {} missing manufacture year. Defaulting to current year.", product.getId());
            manufactureYear = (short) LocalDateTime.now().getYear();
        }

        // Kiểm tra VehicleCatalog đã có thông số cho ModelVersion này chưa
        Optional<VehicleCatalog> existingCatalog = vehicleCatalogRepository
                .findByVersionIdAndBrandIdAndModelAndYear(versionName, brandName, model, manufactureYear);

        if (existingCatalog.isEmpty()) {
            // Catalog chưa tồn tại → Generate mới bằng Gemini
            log.info("Vehicle spec not found for ModelVersion {}. Generating new specs using Gemini...",
                    version.getId());

            try {
                // Gọi Gemini để generate specs DTO
                VehicleCatalogDTO specsDto = geminiRestService.getVehicleSpecs(
                        productName, modelName, brandName, versionName, manufactureYear);

                // Ánh xạ DTO sang Entity
                VehicleCatalog newCatalog = VehicleCatalogMapper.mapFromDto(specsDto);

                // Gán các foreign key & trường bắt buộc
                newCatalog.setVersion(version);
                newCatalog.setCategory(type);
                newCatalog.setBrand(brand);
                newCatalog.setModel(model);
                newCatalog.setYear(manufactureYear);

                // Lưu catalog vào DB
                VehicleCatalog savedCatalog = vehicleCatalogRepository.save(newCatalog);
                log.info("Successfully generated and saved new VehicleCatalog ID: {} for ModelVersion {}",
                        savedCatalog.getId(), version.getId());

                // Liên kết catalog vào VehicleDetails
                details.setVehicleCatalog(savedCatalog);
                vehicleDetailsRepository.save(details);
                log.info("Successfully linked new VehicleCatalog to Product {}", product.getId());

            } catch (Exception e) {
                log.error("Failed to generate or save vehicle specs for Product ID {}: {}",
                        product.getId(), e.getMessage(), e);
            }
        } else {
            // Nếu catalog đã tồn tại, link nó vào VehicleDetails (nếu chưa link)
            VehicleCatalog catalog = existingCatalog.get();

            if (details.getVehicleCatalog() == null ||
                    !details.getVehicleCatalog().getId().equals(catalog.getId())) {

                details.setVehicleCatalog(catalog);
                vehicleDetailsRepository.save(details);
                log.info("Linked existing VehicleCatalog (ID: {}) to Product {}",
                        catalog.getId(), product.getId());
            } else {
                log.info("VehicleCatalog already linked to Product {}", product.getId());
            }
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<PostVerifyResponse> listPendingPosts(Pageable pageable) {
        log.info("Starting listPendingPosts: page={}, size={}, sort={}, direction={}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().toString(),
                pageable.getSort().stream().findFirst().map(Sort.Order::getDirection).orElse(null));

        // Lấy danh sách bài đang chờ duyệt
        Page<Product> page = productRepository.findByStatus(Product.Status.PENDING_REVIEW, pageable);
        log.debug("Fetched {} pending posts (totalElements={}, totalPages={})",
                page.getNumberOfElements(),
                page.getTotalElements(),
                page.getTotalPages());

        if (page.isEmpty()) {
            log.info("No pending posts found for current page (pageNumber={})", pageable.getPageNumber());
        }

        // Map sang PostVerifyResponse
        PageResponse<PostVerifyResponse> response = PageResponse.fromPage(page, product -> {
            log.debug("Mapping productId={} (status={}) to PostVerifyResponse", product.getId(), product.getStatus());
            PostPayment payment = postPaymentRepository
                    .findTopByProductIdAndPaymentStatusOrderByIdDesc(
                            product.getId(),
                            PostPayment.PaymentStatus.COMPLETED)
                    .orElse(null);

            if (payment != null) {
                log.debug("Found completed payment for productId={} (paymentId={}, amount={})",
                        product.getId(), payment.getId(), payment.getAmount());
            } else {
                log.debug("No completed payment found for productId={}", product.getId());
            }

            return PostVerifyMapper.mapToPostVerifyResponse(product, payment);
        });

        log.info("Listed pending posts successfully: returned {} items (page {} of {})",
                response.getItems().size(),
                pageable.getPageNumber(),
                page.getTotalPages());

        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostVerifyResponse> listPendingPostsByType(Product.ProductType type, Pageable pageable) {
        log.info("Starting listPendingPostsByType: type={}, page={}, size={}, sort={}, direction={}",
                type != null ? type : "ALL",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().toString(),
                pageable.getSort().stream().findFirst().map(Sort.Order::getDirection).orElse(null));

        // Lấy danh sách bài pending theo loại
        Page<Product> page = (type != null)
                ? productRepository.findByStatusAndType(Product.Status.PENDING_REVIEW, type, pageable)
                : productRepository.findByStatus(Product.Status.PENDING_REVIEW, pageable);

        log.debug("Fetched {} pending posts (type={}, totalElements={}, totalPages={})",
                page.getNumberOfElements(),
                type != null ? type : "ALL",
                page.getTotalElements(),
                page.getTotalPages());

        if (page.isEmpty()) {
            log.info("No pending posts found for type={} on page {}.", type, pageable.getPageNumber());
        }

        // Map sang PostVerifyResponse
        PageResponse<PostVerifyResponse> response = PageResponse.fromPage(page, product -> {
            log.debug("Mapping productId={} (status={}, type={}) to PostVerifyResponse",
                    product.getId(), product.getStatus(), product.getType());

            PostPayment payment = postPaymentRepository
                    .findTopByProductIdAndPaymentStatusOrderByIdDesc(
                            product.getId(),
                            PostPayment.PaymentStatus.COMPLETED)
                    .orElse(null);

            if (payment != null) {
                log.debug("Found completed payment for productId={} (paymentId={}, amount={})",
                        product.getId(), payment.getId(), payment.getAmount());
            } else {
                log.debug("No completed payment found for productId={}", product.getId());
            }

            return PostVerifyMapper.mapToPostVerifyResponse(product, payment);
        });

        log.info("Listed pending posts by type successfully: type={}, returned {} items (page {} of {})",
                type != null ? type : "ALL",
                response.getItems().size(),
                pageable.getPageNumber(),
                page.getTotalPages());

        return response;
    }

    @Transactional(readOnly = true)
    public ApprovalRateResponse getApprovalRate() {
        log.info("=== [START] Calculating product approval rate ===");

        // Đếm số lượng sản phẩm theo trạng thái
        long approved = productRepository.countByStatus(Product.Status.ACTIVE);
        long rejected = productRepository.countByStatus(Product.Status.REJECTED);
        log.debug("Counted products -> approved: {}, rejected: {}", approved, rejected);

        long decided = approved + rejected;
        double rate = decided == 0 ? 0.0 : ((double) approved) / decided;
        String rateText = String.format(Locale.US, "%.2f%%", rate * 100.0);

        log.info("Approval rate calculated -> decided: {}, rate: {}, rateText: {}", decided, rate, rateText);

        ApprovalRateResponse response = ApprovalRateMapper.mapToApprovalRateResponse(
                approved, rejected, decided, rate, rateText);

        log.debug("Mapped ApprovalRateResponse: {}", response);
        log.info("=== [END] getApprovalRate completed successfully ===");

        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionsHistory> getAllTransactionHistory(Pageable pageable) {
        Page<ContractDocument> page = contractDocumentRepository.findAll(pageable);

        return PageResponse.fromPage(page, this::mapToHistoryDTO);
    }

    public ApprovalRateResponse getApprovalRateByDate(LocalDate date) {
        if (date == null) {
            throw new AppException(ErrorCode.DATE_MUST_REQUIRED);
        }

        LocalDateTime startOfDay = date.atStartOfDay(VIETNAM_ZONE).toLocalDateTime();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX).atZone(VIETNAM_ZONE).toLocalDateTime();

        long approved = productRepository.countByStatusAndUpdatedAtBetween(Product.Status.ACTIVE, startOfDay, endOfDay);
        long rejected = productRepository.countByStatusAndUpdatedAtBetween(Product.Status.REJECTED, startOfDay,
                endOfDay);

        long decided = approved + rejected;

        double rate = decided == 0 ? 0.0 : (double) approved / decided;
        String rateText = String.format(Locale.US, "%.2f%%", rate * 100.0);

        return ApprovalRateResponse.builder()
                .approved(approved)
                .rejected(rejected)
                .total(decided)
                .rate(rate)
                .rateText(rateText)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TransactionsHistory> getAllTransactionHistory() {
        // Lấy tất cả ContractDocument
        List<ContractDocument> contractDocumentList = contractDocumentRepository.findAll();

        // Chuyển đổi (map) từ List<ContractDocument> sang List<TransactionsHistory>
        return contractDocumentList.stream()
                .map(this::mapToHistoryDTO) // Gọi hàm helper để chuyển đổi
                .collect(Collectors.toList());
    }

    private TransactionsHistory mapToHistoryDTO(ContractDocument contractDocument) {
        TransactionsHistory history = new TransactionsHistory();

        history.setTitle(contractDocument.getTitle());
        history.setSignedAt(contractDocument.getSignedAt());
        history.setContractUrl(contractDocument.getPdfUrl());

        PurchaseRequest request = contractDocument.getPurchaseRequest();

        if (request != null) {
            // Lấy tên người bán
            if (request.getSeller() != null) {
                history.setSellerName(request.getSeller().getFullName());
            } else {
                history.setSellerName("N/A");
            }

            // Lấy tên người mua
            if (request.getBuyer() != null) {
                history.setBuyerName(request.getBuyer().getFullName());
            } else {
                history.setBuyerName("N/A");
            }
        } else {
            // Fallback nếu không có PurchaseRequest liên kết
            log.warn("ContractDocument ID {} không có PurchaseRequest liên kết.", contractDocument.getId());
            history.setSellerName("N/A");
            history.setBuyerName("N/A");
        }

        return history;
    }
}