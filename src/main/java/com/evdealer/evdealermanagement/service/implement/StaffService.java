package com.evdealer.evdealermanagement.service.implement;

import com.cloudinary.api.exceptions.ApiException;
import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.dto.post.verification.PostVerifyResponse;
import com.evdealer.evdealermanagement.dto.rate.ApprovalRateResponse;
import com.evdealer.evdealermanagement.dto.vehicle.catalog.VehicleCatalogDTO;
import com.evdealer.evdealermanagement.entity.account.Account;
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
import com.evdealer.evdealermanagement.mapper.vehicle.VehicleCatalogMapper;
import com.evdealer.evdealermanagement.repository.*;


import lombok.RequiredArgsConstructor;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;

import org.springframework.data.domain.Pageable;
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

        Account currentUser = userContextService.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized user"));

        if (currentUser.getRole() != Account.Role.STAFF && currentUser.getRole() != Account.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only STAFF or ADMIN can verify posts");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (product.getStatus() != Product.Status.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only posts in PENDING_REVIEW status can be verified or rejected");
        }

        PostPayment payment = postPaymentRepository
                .findTopByProductIdAndPaymentStatusOrderByCreatedAtDesc(
                        product.getId(), PostPayment.PaymentStatus.COMPLETED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No completed payment found for this product"));

        int elevatedDays = 0;
        if (payment.getPostPackageOption() != null) {
            Integer d = payment.getPostPackageOption().getDurationDays();
            elevatedDays = (d != null ? d : 0);
        }

        // Ghi mốc thời gian theo yêu cầu
        LocalDateTime now = nowVietNam();
        product.setFeaturedEndAt(elevatedDays > 0 ? now.plusDays(elevatedDays) : null);
        product.setExpiresAt(now.plusDays(30));
        product.setUpdatedAt(now);

        // Thay đổi status và set approver
        product.setStatus(Product.Status.ACTIVE);
        product.setRejectReason(null);
        product.setApprovedBy(currentUser);

        // Xử lý thông số kỹ thuật xe sau khi DUYỆT BÀI
        if (isVehicleProduct(product)) {
            generateAndSaveVehicleSpecs(product);
        }

        // Lưu product
        Product savedProduct = productRepository.save(product);

        log.info("Product {} approved successfully. FeaturedEndAt: {}, ExpiresAt: {}",
                savedProduct.getId(),
                savedProduct.getFeaturedEndAt(),
                savedProduct.getExpiresAt());

        return PostVerifyMapper.mapToPostVerifyResponse(savedProduct, payment);
    }

    private boolean isVehicleProduct(Product product) {
        return product.getType() != null && "VEHICLE".equals(product.getType().name());
    }

    @Transactional
    public PostVerifyResponse verifyPostReject(String productId, String rejectReason) {
        Account currentUser = userContextService.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized user"));

        if (currentUser.getRole() != Account.Role.STAFF && currentUser.getRole() != Account.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only STAFF or ADMIN can reject posts");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != Product.Status.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only posts in PENDING_REVIEW status can be rejected");
        }

        PostPayment payment = postPaymentRepository
                .findTopByProductIdAndPaymentStatusOrderByIdDesc(
                        product.getId(),
                        PostPayment.PaymentStatus.COMPLETED)
                .orElse(null);

        // Set các trường cần thiết
        LocalDateTime now = nowVietNam();
        product.setStatus(Product.Status.REJECTED);
        product.setRejectReason(rejectReason);
        product.setUpdatedAt(now);
        product.setApprovedBy(currentUser);

        Product savedProduct = productRepository.save(product);

        log.info(" Product {} rejected by {}. Reason: {}",
                savedProduct.getId(),
                currentUser.getEmail(),
                rejectReason);

        return PostVerifyMapper.mapToPostVerifyResponse(savedProduct, payment);
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
    public Page<PostVerifyResponse> getListVerifyPost(Pageable pageable) {
        Page<Product> productsPage = productRepository.findByStatus(Product.Status.PENDING_REVIEW, pageable);

        return productsPage.map(product -> {
            PostPayment payment = postPaymentRepository
                    .findTopByProductIdAndPaymentStatusOrderByIdDesc(
                            product.getId(),
                            PostPayment.PaymentStatus.COMPLETED)
                    .orElse(null);
            return PostVerifyMapper.mapToPostVerifyResponse(product, payment);
        });
    }

    @Transactional(readOnly = true)
    public Page<PostVerifyResponse> getListVerifyPostByType(Product.ProductType type, Pageable pageable) {
        Page<Product> productsPage;
        if (type != null) {
            productsPage = productRepository.findByStatusAndType(Product.Status.PENDING_REVIEW, type, pageable);
        } else {
            productsPage = productRepository.findByStatus(Product.Status.PENDING_REVIEW, pageable);
        }

        return productsPage.map(product -> {
            PostPayment payment = postPaymentRepository
                    .findTopByProductIdAndPaymentStatusOrderByIdDesc(
                            product.getId(),
                            PostPayment.PaymentStatus.COMPLETED)
                    .orElse(null);
            return PostVerifyMapper.mapToPostVerifyResponse(product, payment);
        });
    }

    @Transactional(readOnly = true)
    public ApprovalRateResponse getApprovalRate() {
        long approved = productRepository.countByStatus(Product.Status.ACTIVE);
        long rejected = productRepository.countByStatus(Product.Status.REJECTED);

        long decided = approved + rejected;
        double rate = decided == 0 ? 0.0 : ((double) approved) / decided;
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