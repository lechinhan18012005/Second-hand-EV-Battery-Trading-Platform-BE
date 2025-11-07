package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.battery.detail.BatteryDetailResponse;
import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.dto.post.verification.PostVerifyResponse;
import com.evdealer.evdealermanagement.dto.product.compare.ProductCompareResponse;
import com.evdealer.evdealermanagement.dto.product.compare.ProductSuggestionResponse;
import com.evdealer.evdealermanagement.dto.product.detail.ProductDetail;
import com.evdealer.evdealermanagement.dto.product.moderation.ProductPendingResponse;
import com.evdealer.evdealermanagement.dto.vehicle.detail.VehicleDetailResponse;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.entity.battery.BatteryDetails;
import com.evdealer.evdealermanagement.entity.post.PostPayment;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.entity.vehicle.VehicleDetails;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.mapper.post.PostVerifyMapper;
import com.evdealer.evdealermanagement.mapper.product.ProductMapper;
import com.evdealer.evdealermanagement.repository.BatteryDetailRepository;
import com.evdealer.evdealermanagement.repository.PostPaymentRepository;
import com.evdealer.evdealermanagement.repository.ProductRepository;
import com.evdealer.evdealermanagement.repository.VehicleDetailsRepository;
import com.evdealer.evdealermanagement.service.contract.IProductService;
import com.evdealer.evdealermanagement.utils.ProductSpecs;
import com.evdealer.evdealermanagement.utils.SecurityUtils;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final ProductRepository productRepository;
    private final VehicleService vehicleService;
    private final BatteryService batteryService;
    private final WishlistService wishlistService;
    private final PostPaymentRepository postPaymentRepository;
    private final VehicleDetailsRepository vehicleDetailsRepository;
    private final BatteryDetailRepository batteryDetailRepository;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostVerifyResponse> getAllProductsWithStatus(String status, Pageable pageable) {
        try {
            log.info("=== START getAllProductsWithStatus===");
            pageable = capPageSize(pageable);

            // Validate status
            Product.Status statusEnum = validateAndParseStatus(status);

            Specification<Product> specification = Specification.where(ProductSpecs.hasStatus(statusEnum));
            Page<Product> products = productRepository.findAll(specification, pageable);

            List<PostVerifyResponse> content = products.getContent().stream().map(
                    product -> {
                        PostPayment payments = postPaymentRepository.findFirstByProductIdOrderByCreatedAtDesc(product.getId());
                        return PostVerifyMapper.mapToPostVerifyResponse(product, payments);
                    })
                    .toList();
            return PageResponse.of(content, products);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", status);
            throw new IllegalArgumentException("Invalid status: " + status + ". Valid values: " +
                    Arrays.toString(Product.Status.values()));
        } catch (Exception e) {
            log.error("FATAL ERROR in getAllProductsWithStatus", e);
            throw new RuntimeException("Failed to get all products by status: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostVerifyResponse> getAllProductsWithStatusAll(Pageable pageable) {
        pageable = capPageSize(pageable);
        Specification<Product> spec = Specification.where(ProductSpecs.all());
        log.info("=== START getAllProductsWithStatusAll==");
        log.info("spec =  {}", spec);
        Page<Product> products = productRepository.findAll(spec, pageable);
        log.info("products =  {}", products);
        List<PostVerifyResponse> content = products.getContent().stream().map(
                        product -> {
                            PostPayment payments = postPaymentRepository.findByProductId(product.getId());
                            return PostVerifyMapper.mapToPostVerifyResponse(product, payments);
                        })
                .toList();
        return PageResponse.of(content, products);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<ProductDetail> getProductById(String id) {
        if (id == null || id.trim().isEmpty()) {
            log.warn("Invalid product ID: null or empty");
            return Optional.empty();
        }

        try {
            log.info("Fetching product by ID: {}", id);

            String accountId = SecurityUtils.getCurrentAccountId();
            Optional<Product> productOpt = productRepository.findById(id);

            if (productOpt.isEmpty()) {
                log.info("Product not found with ID: {}", id);
                return Optional.empty();
            }

            ProductDetail productDetail = ProductMapper.toDetailDto(productOpt.get());

            // If user logged in, check if this product is in wishlist
            if (accountId != null) {
                try {
                    boolean isWishlisted = wishlistService.isProductInWishlist(accountId, id);
                    productDetail.setIsWishlisted(isWishlisted);
                } catch (Exception e) {
                    log.error("Error checking wishlist status for product {}", id, e);
                    productDetail.setIsWishlisted(false);
                }
            }

            return Optional.of(productDetail);

        } catch (Exception e) {
            log.error("Error fetching product by ID: {}", id, e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductDetail> getProductByName(String name,
                                                        String city,
                                                        BigDecimal minPrice,
                                                        BigDecimal maxPrice,
                                                        Integer yearFrom,
                                                        Integer yearTo,
                                                        Pageable pageable) {

        pageable = capPageSize(pageable);
        validateFilters(minPrice, maxPrice, yearFrom, yearTo);
        if (name == null || name.trim().isEmpty()) {
            log.warn("Product name is null or empty");
            return PageResponse.<ProductDetail>builder()
                    .items(Collections.emptyList())
                    .page(0)
                    .size(0)
                    .totalElements(0)
                    .totalPages(0)
                    .hasNextPage(false)
                    .hasPreviousPage(false)
                    .build();
        }

        Sort sort = Sort.by(
                Sort.Order.desc("isHot"),
                Sort.Order.desc("createdAt")
        );
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Specification<Product> spec = Specification
                .where(ProductSpecs.hasStatus(Product.Status.ACTIVE))
                .and(ProductSpecs.titleLike(name))
                .and(ProductSpecs.cityEq(city))
                .and(ProductSpecs.priceGte(minPrice))
                .and(ProductSpecs.priceLte(maxPrice))
                .and(ProductSpecs.yearGte(yearFrom))
                .and(ProductSpecs.yearLte(yearTo));
        Page<Product> products = productRepository.findAll(spec, pageable);

        List<ProductDetail> content = toDetailsWithWishlist(products.getContent());

        return PageResponse.of(content, products);


    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDetail> getProductByType(String type) {
        if (type == null || type.trim().isEmpty()) {
            log.warn("Product type is null or empty");
            return List.of();
        }

        try {
            log.info("Fetching products by type: {}", type);

            Product.ProductType enumType;
            try {
                enumType = Product.ProductType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid product type: {}", type);
                return List.of();
            }

            String accountId = SecurityUtils.getCurrentAccountId();
            List<Product> products = productRepository.findByType(enumType);

            if (products.isEmpty()) {
                log.info("No products found with type: {}", type);
                return List.of();
            }

            List<ProductDetail> result;
            try {
                result = wishlistService.attachWishlistFlag(
                        accountId,
                        products,
                        ProductMapper::toDetailDto,
                        ProductDetail::setIsWishlisted);
            } catch (Exception e) {
                log.error("Error attaching wishlist flags, using basic mapping", e);
                result = products.stream()
                        .map(ProductMapper::toDetailDto)
                        .collect(Collectors.toList());
            }

            log.info("Found {} products with type: {}", result.size(), type);
            return result;

        } catch (Exception e) {
            log.error("Error fetching products by type: {}", type, e);
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDetail> getProductByBrand(String brand) {
        if (brand == null || brand.trim().isEmpty()) {
            log.warn("Brand name is null or empty");
            return List.of();
        }

        try {
            log.info("Fetching products by brand: {}", brand);

            List<String> allProductIds = getProductIdsByBrand(brand);

            if (allProductIds.isEmpty()) {
                log.info("No products found for brand: {}", brand);
                return List.of();
            }

            // Filter ACTIVE and sort by createdAt
            List<Product> products = productRepository.findAllById(allProductIds)
                    .stream()
                    .filter(product -> product.getStatus() == Product.Status.ACTIVE)
                    .collect(Collectors.toList());

            if (products.isEmpty()) {
                log.info("No ACTIVE products found for brand: {}", brand);
                return List.of();
            }

            String accountId = SecurityUtils.getCurrentAccountId();
            List<ProductDetail> result;

            try {
                result = wishlistService.attachWishlistFlag(
                        accountId,
                        products,
                        ProductMapper::toDetailDto,
                        ProductDetail::setIsWishlisted);
            } catch (Exception e) {
                log.error("Error attaching wishlist flags, using basic mapping", e);
                result = products.stream()
                        .map(ProductMapper::toDetailDto)
                        .collect(Collectors.toList());
            }

            // Sort by createdAt with null-safe comparator
            result.sort(Comparator.comparing(
                    ProductDetail::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())));

            log.info("Found {} products for brand: {}", result.size(), brand);
            return result;

        } catch (Exception e) {
            log.error("Error fetching products by brand: {}", brand, e);
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDetail> getNewProducts() {
        try {
            log.info("=== START getNewProducts ===");

            // Lấy nhiều hơn 12 để có buffer
            List<Product> products = productRepository.findActiveFeaturedSorted(
                    Product.Status.ACTIVE,
                    VietNamDatetime.nowVietNam(),
                    PageRequest.of(0,120) // Lấy 120 để đảm bảo sau khi filter vẫn còn đủ
            );

            log.info("Found {} products from DB", products.size());

            List<ProductDetail> result;
            try {
                result = toDetailsWithWishlist(products);
            } catch (Exception e) {
                log.error("Error attaching wishlist flags, using basic mapping", e);
                result = products.stream()
                        .map(ProductMapper::toDetailDto)
                        .collect(Collectors.toList());
            }

            // Chỉ lấy 120 cái đầu
            result = result.stream()
                    .limit(120)
                    .collect(Collectors.toList());

            log.info("=== END getNewProducts: {} products (requested 120) ===", result.size());

            // Warning nếu không đủ
            if (result.size() < 120) {
                log.warn("Only found {} products, less than requested 120", result.size());
            }

            return result;

        } catch (Exception e) {
            log.error("FATAL ERROR in getNewProducts", e);
            throw new RuntimeException("Failed to get new products: " + e.getMessage(), e);
        }
    }

    // ============================================
    // NEW METHOD: Filter with multiple filters
    // ============================================
    @Transactional(readOnly = true)
    public PageResponse<ProductDetail> filterProducts(String name, String brand, String type, String city,
            String district, BigDecimal minPrice, BigDecimal maxPrice, Integer yearFrom, Integer yearTo,
            Pageable pageable) {
        validateFilters(minPrice, maxPrice, yearFrom, yearTo);
        pageable = capPageSize(pageable);

        Product.ProductType emunType = parseTypeOrNull(type);

        Specification<Product> spec = Specification
                .where(ProductSpecs.hasStatus(Product.Status.ACTIVE))
                .and(ProductSpecs.titleLike(name))
                .and(ProductSpecs.hasType(emunType))
                .and(emunType == Product.ProductType.VEHICLE ? ProductSpecs.hasVehicleBrandId(brand)
                        : ProductSpecs.hasBatteryBrandId(brand))
                .and(ProductSpecs.cityEq(city))
                .and(ProductSpecs.districtEq(district))
                .and(ProductSpecs.priceGte(minPrice))
                .and(ProductSpecs.priceLte(maxPrice))
                .and(ProductSpecs.yearGte(yearFrom))
                .and(ProductSpecs.yearLte(yearTo));

        Page<Product> page = productRepository.findAll(spec, pageable);

        String accountId = SecurityUtils.getCurrentAccountId(); // có thể null nếu chưa đăng nhập
        List<ProductDetail> content;
        try {
            content = wishlistService.attachWishlistFlag(
                    accountId,
                    page.getContent(),
                    ProductMapper::toDetailDto, // Product -> ProductDetail
                    ProductDetail::setIsWishlisted);
        } catch (Exception e) {
            // Không để toàn API fail vì wishlist thất bại
            content = page.getContent().stream().map(ProductMapper::toDetailDto).toList();
        }

        // Lưu ý: thứ tự trả về đã theo pageable.sort (không cần sort lại ở đây)
        return PageResponse.of(content, page);
    }

    // ============================================
    // HELPER METHOD: Get product IDs by brand
    // ============================================
    private List<String> getProductIdsByBrand(String brand) {
        try {
            log.debug("Getting product IDs for brand: {}", brand);

            // Get vehicle product IDs
            List<String> vehicleProductIds = vehicleService.getVehicleIdByBrand(brand)
                    .stream()
                    .map(String::valueOf)
                    .toList();

            // Get battery product IDs
            List<String> batteryProductIds = batteryService.getBatteryIdByBrand(brand);

            // Merge and remove duplicates
            List<String> allProductIds = Stream.concat(
                    vehicleProductIds.stream(),
                    batteryProductIds.stream()).distinct().collect(Collectors.toList());

            log.debug("Found {} product IDs for brand '{}' (vehicles: {}, batteries: {})",
                    allProductIds.size(), brand, vehicleProductIds.size(), batteryProductIds.size());

            return allProductIds;

        } catch (Exception e) {
            log.error("Error getting product IDs for brand: {}", brand, e);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<ProductPendingResponse> getPendingProducts() {
        List<Product> products = productRepository.findByStatus(Product.Status.PENDING_REVIEW);
        List<ProductPendingResponse> result = new ArrayList<>();

        if (products.isEmpty()) {
            return result;
        }

        for (Product p : products) {
            Account seller = p.getSeller();
            String imageUrl = null;

            if (p.getImages() != null && !p.getImages().isEmpty()) {
                imageUrl = p.getImages().get(0).getImageUrl();
            }

            ProductPendingResponse dto = new ProductPendingResponse();
            dto.setId(p.getId());
            dto.setTitle(p.getTitle());
            dto.setType(p.getType() != null ? p.getType().name() : null);
            dto.setPrice(p.getPrice());
            dto.setImageUrl(imageUrl);

            if (seller != null) {
                dto.setSellerId(seller.getId());
                dto.setSellerName(seller.getFullName());
                dto.setSellerPhone(seller.getPhone());
            }
            dto.setCreatedAt(p.getCreatedAt());
            result.add(dto);
        }

        return result;
    }

    public List<String> getAllStatuses() {
        List<String> statuses = new ArrayList<>();
        for (Product.Status status : Product.Status.values()) {
            statuses.add(status.name());
        }
        return statuses;
    }

    private void validateFilters(BigDecimal minPrice, BigDecimal maxPrice, Integer yearFrom, Integer yearTo) {
        if (minPrice != null && minPrice.signum() < 0) {
            throw new IllegalArgumentException("minPrice must be >= 0");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }
        if (yearFrom != null && yearTo != null && yearFrom > yearTo) {
            throw new IllegalArgumentException("yearFrom cannot be greater than yearTo");
        }
    }

    private Pageable capPageSize(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        return pageable;
    }

    private Product.ProductType parseTypeOrNull(String type) {
        if (type == null || type.isBlank())
            return null;
        try {
            return Product.ProductType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid product type: " + type);
        }
    }

    private List<ProductDetail> toDetailsWithWishlist(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        String accontId = SecurityUtils.getCurrentAccountId();
        try {
            return wishlistService.attachWishlistFlag(
                    accontId,
                    products,
                    ProductMapper::toDetailDto,
                    ProductDetail::setIsWishlisted
            );
        } catch (Exception e) {
            log.warn("Attach wishlist failed, fallback basic mapping", e);
            return products.stream().map(ProductMapper::toDetailDto).toList();
        }
    }

    public Product.Status validateAndParseStatus(String status) {
        if(status == null || status.isBlank()) {
            throw new IllegalArgumentException("status cannot be null or blank");
        }
        try {
            return Product.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid product status: " + status);
        }
    }

    @Transactional
    public List<ProductSuggestionResponse> suggestProducts(String productId) {
        Product current = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        BigDecimal price = current.getPrice();
        BigDecimal difference = price.multiply(BigDecimal.valueOf(0.15));

        if(current.getType() == Product.ProductType.VEHICLE) {
            VehicleDetails v = vehicleDetailsRepository.findByProductId(productId)
                    .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

            return vehicleDetailsRepository.findByBrandAndPriceBetween(
                    v.getBrand(), price.subtract(difference), price.add(difference))
                    .stream()
                    .filter(x -> !x.getProduct().getId().equals(productId))
                    .distinct()
                    .map(x -> ProductSuggestionResponse.builder()
                            .id(x.getProduct().getId())
                            .title(x.getProduct().getTitle())
                            .price(x.getProduct().getPrice())
                            .brand(x.getBrand() != null ? x.getBrand().getName() : null)
                            .model(x.getModel() != null ? x.getModel().getName() : null)
                            .version(x.getVersion() != null ? x.getVersion().getName() : null)
                            .build())
                    .toList();
        } else if(current.getType() == Product.ProductType.BATTERY) {
            BatteryDetails b = batteryDetailRepository.findByProductsId(productId)
                    .orElseThrow(() -> new AppException(ErrorCode.BATTERY_NOT_FOUND));

            return batteryDetailRepository.findByBatteryTypeAndPriceBetween(
                            b.getBatteryType(), price.subtract(difference), price.add(difference))
                    .stream()
                    .filter(x -> !x.getProduct().getId().equals(productId))
                    .distinct()
                    .map(x -> ProductSuggestionResponse.builder()
                            .id(x.getProduct().getId())
                            .title(x.getProduct().getTitle())
                            .price(x.getProduct().getPrice())
                            .brand(x.getBrand() != null ? x.getBrand().getName() : null)
                            .batteryType(x.getBatteryType() != null ? x.getBatteryType().getName() : null)
                            .build())
                    .toList();
        }
        return Collections.emptyList();
    }

    @Transactional
    public ProductCompareResponse compareProducts(String currentProductId, String targetProductId) {
        Product current = productRepository.findById(currentProductId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Product target = productRepository.findById(targetProductId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if(current.getType() != target.getType()) {
            throw new AppException(ErrorCode.INVALID_COMPARE);
        }

        if(current.getType() == Product.ProductType.VEHICLE) {
            VehicleDetailResponse currentDetail = vehicleService.getVehicleDetailsInfo(currentProductId);
            VehicleDetailResponse targetDetail = vehicleService.getVehicleDetailsInfo(targetProductId);
            return ProductCompareResponse.builder()
                    .currentVehicle(currentDetail)
                    .targetVehicle(targetDetail)
                    .currentBattery(null)
                    .targetBattery(null)
                    .build();
        } else if(current.getType() == Product.ProductType.BATTERY) {
            BatteryDetailResponse currentDetail = batteryService.getBatteryDetail(currentProductId);
            BatteryDetailResponse targetDetail = batteryService.getBatteryDetail(targetProductId);
            return ProductCompareResponse.builder()
                    .currentVehicle(null)
                    .targetVehicle(null)
                    .currentBattery(currentDetail)
                    .targetBattery(targetDetail)
                    .build();
        }
        throw new AppException(ErrorCode.INVALID_REQUEST);
    }

}