package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.battery.brand.BatteryBrandsResponse;
import com.evdealer.evdealermanagement.dto.battery.brand.BatteryTypesResponse;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.evdealer.evdealermanagement.dto.battery.brand.BatteryBrandsRequest;
import com.evdealer.evdealermanagement.dto.battery.detail.BatteryDetailResponse;
import com.evdealer.evdealermanagement.dto.battery.type.BatteryTypeResponse;
import com.evdealer.evdealermanagement.dto.battery.type.CreateBatteryTypeRequest;
import com.evdealer.evdealermanagement.dto.post.battery.BatteryPostRequest;
import com.evdealer.evdealermanagement.dto.post.battery.BatteryPostResponse;
import com.evdealer.evdealermanagement.dto.post.common.ProductImageResponse;
import com.evdealer.evdealermanagement.dto.product.similar.SimilarProductResponse;
import com.evdealer.evdealermanagement.entity.battery.BatteryBrands;
import com.evdealer.evdealermanagement.entity.battery.BatteryDetails;
import com.evdealer.evdealermanagement.entity.battery.BatteryTypes;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.entity.product.ProductImages;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.mapper.battery.BatteryBrandMapper;
import com.evdealer.evdealermanagement.mapper.battery.BatteryDetailsMapper;
import com.evdealer.evdealermanagement.mapper.battery.BatteryMapper;
import com.evdealer.evdealermanagement.repository.*;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatteryService {

    private final BatteryDetailRepository batteryDetailRepository;
    private final BatteryBrandsRepository batteryBrandsRepository;
    private final BatteryTypesRepository batteryTypesRepository;
    private final ProductRepository productRepository;
    private final PostService postService;
    private final ProductImagesRepository productImagesRepository;
    private final Cloudinary cloudinary;
    private final BatteryTypesRepository batteryTypeRepository;

    /**
     * Lấy danh sách Battery Product IDs theo tên sản phẩm
     */
    public List<String> getBatteryIdByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            log.warn("Battery name is null or empty");
            return List.of();
        }

        try {
            log.debug("Getting battery IDs by name: {}", name);
            return batteryDetailRepository.findProductIdsByProductTitle(name);
        } catch (Exception e) {
            log.error("Error getting battery IDs by name: {}", name, e);
            return List.of();
        }
    }

    /**
     * Lấy danh sách Battery Product IDs theo tên hãng
     */
    public List<String> getBatteryIdByBrand(String brand) {
        if (brand == null || brand.trim().isEmpty()) {
            log.warn("Battery brand is null or empty");
            return List.of();
        }

        try {
            log.debug("Getting battery IDs by brand: {}", brand);
            return batteryDetailRepository.findProductIdsByBrandName(brand);
        } catch (Exception e) {
            log.error("Error getting battery IDs by brand: {}", brand, e);
            return List.of();
        }
    }

    /**
     * Lấy BatteryDetails theo tên sản phẩm
     */
    public List<BatteryDetails> getBatteryDetailsByProductName(String name) {
        if (name == null || name.trim().isEmpty()) {
            log.warn("Product name is null or empty");
            return List.of();
        }

        try {
            log.debug("Getting battery details by product name: {}", name);
            return batteryDetailRepository.findByProductTitleLikeIgnoreCase(name);
        } catch (Exception e) {
            log.error("Error getting battery details by product name: {}", name, e);
            return List.of();
        }
    }

    /**
     * Lấy BatteryDetails theo ID
     */
    public Optional<BatteryDetails> getBatteryDetailsById(String id) {
        if (id == null || id.trim().isEmpty()) {
            log.warn("Invalid battery ID: {}", id);
            return Optional.empty();
        }

        try {
            UUID.fromString(id); // Validate UUID format
            log.debug("Getting battery details by ID: {}", id);
            return batteryDetailRepository.findById(id);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for battery ID: {}", id);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting battery details by ID: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * Lấy tất cả BatteryDetails
     */
    public List<BatteryDetails> getAllBatteryDetails() {
        try {
            log.debug("Getting all battery details");
            return batteryDetailRepository.findAll();
        } catch (Exception e) {
            log.error("Error getting all battery details", e);
            return List.of();
        }
    }

    /**
     * Lấy pin theo battery type
     */
    public List<BatteryDetails> getBatteryDetailsByType(String type) {
        if (type == null || type.trim().isEmpty()) {
            log.warn("Battery type is null or empty");
            return List.of();
        }

        try {
            log.debug("Getting batteries by type: {}", type);
            return batteryDetailRepository.findByBatteryTypeNameLikeIgnoreCase(type);
        } catch (Exception e) {
            log.error("Error getting batteries by type: {}", type, e);
            return List.of();
        }
    }

    /**
     * Lấy pin theo capacity range
     */
    public List<BatteryDetails> getBatteriesByCapacityRange(Double minCapacity, Double maxCapacity) {
        if (minCapacity == null || maxCapacity == null || minCapacity < 0 || maxCapacity < minCapacity) {
            log.warn("Invalid capacity range: {} - {}", minCapacity, maxCapacity);
            return List.of();
        }

        try {
            log.debug("Getting batteries by capacity range: {} - {}", minCapacity, maxCapacity);
            return batteryDetailRepository.findByCapacityKwhBetween(BigDecimal.valueOf(minCapacity),
                    BigDecimal.valueOf(maxCapacity));
        } catch (Exception e) {
            log.error("Error getting batteries by capacity range: {} - {}", minCapacity, maxCapacity, e);
            return List.of();
        }
    }

    /**
     * Lấy pin theo danh sách các hãng
     */
    public List<BatteryDetails> getBatteriesByBrands(List<String> brandNames) {
        if (brandNames == null || brandNames.isEmpty()) {
            log.warn("Brand names list is null or empty");
            return List.of();
        }

        try {
            log.debug("Getting batteries by brands: {}", brandNames);
            return batteryDetailRepository.findByBrandNameIn(brandNames);
        } catch (Exception e) {
            log.error("Error getting batteries by brands: {}", brandNames, e);
            return List.of();
        }
    }

    /**
     * Lấy pin của các hãng phổ biến (Panasonic, Samsung SDI, LG Energy)
     */
    public List<BatteryDetails> getPopularBrandBatteries() {
        List<String> popularBrands = List.of("Panasonic", "Samsung SDI", "LG Energy");
        return getBatteriesByBrands(popularBrands);
    }

    public List<BatteryTypesResponse> listAllBatteryTypesSorted() {
        var all = batteryTypesRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        return all.stream().map(b -> new BatteryTypesResponse(b.getId(), b.getName())).collect(Collectors.toList());
    }

    public List<BatteryBrandsResponse> listAllBatteryBrandsSorted() {
        var all = batteryBrandsRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        return all.stream().map(b -> BatteryBrandsResponse.builder()
                .brandId(b.getId())
                .brandName(b.getName())
                .build())
                .collect(Collectors.toList());
    }

    public List<BatteryBrandsResponse> listAllBatteryNameAndLogo() {
        var all = batteryBrandsRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        return all.stream().map(b -> BatteryBrandsResponse.builder()
                .brandName(b.getName())
                .logoUrl(b.getLogoUrl())
                .build())
                .collect(Collectors.toList());
    }

    public BatteryBrandsResponse addNewBatteryBrand(BatteryBrandsRequest req) {

        String brandName = req.getBrandName().trim();
        if (batteryBrandsRepository.existsByNameIgnoreCase(brandName)) {
            throw new AppException(ErrorCode.BRAND_EXISTS, "Brand name already exists");
        }

        BatteryBrands e = new BatteryBrands();
        e.setName(brandName);
        e.setLogoUrl(req.getLogoUrl());
        e = batteryBrandsRepository.save(e);
        return BatteryMapper.mapToBatteryBrandsResponse(e);
    }

    @Transactional
    public BatteryPostResponse updateBatteryPost(String productId, BatteryPostRequest request,
            List<MultipartFile> images, String imagesMetaJson) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != Product.Status.DRAFT) {
            throw new AppException(ErrorCode.PRODUCT_NOT_DRAFT);
        }

        BatteryDetails details = batteryDetailRepository.findByProductId(product.getId());
        if (details == null) {
            throw new AppException(ErrorCode.BATTERY_NOT_FOUND);
        }

        if (request != null) {
            product.setTitle(request.getTitle());
            product.setDescription(request.getDescription());
            product.setPrice(request.getPrice());
            product.setCity(request.getCity());
            product.setDistrict(request.getDistrict());
            product.setWard(request.getWard());
            product.setAddressDetail(request.getAddressDetail());
            product.setUpdatedAt(VietNamDatetime.nowVietNam());

            details.setProduct(product);
            details.setBrand(batteryBrandsRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND)));
            details.setBatteryType(batteryTypesRepository.findById(request.getBatteryTypeId())
                    .orElseThrow(() -> new AppException(ErrorCode.TYPE_NOT_FOUND)));
            details.setCapacityKwh(request.getCapacityKwh());
            details.setVoltageV(request.getVoltageV());
            details.setHealthPercent(request.getHealthPercent());

            batteryDetailRepository.save(details);
        }

        if (images != null) {
            images = images.stream()
                    .filter(file -> file != null && !file.isEmpty())
                    .toList();
        }

        List<ProductImageResponse> imageDtos = null;

        if (images != null && !images.isEmpty()) {

            productImagesRepository.deleteAllByProduct(product);
            productImagesRepository.flush();

            imageDtos = postService.uploadAndSaveImages(product, images, imagesMetaJson);

            product.getImages().clear();

            List<ProductImages> newImages = imageDtos.stream()
                    .map(dto -> ProductImages.builder()
                            .product(product)
                            .imageUrl(dto.getUrl())
                            .isPrimary(dto.isPrimary())
                            .build())
                    .collect(Collectors.toList());
            product.getImages().addAll(newImages);
        }

        productRepository.save(product);
        batteryDetailRepository.save(details);

        return BatteryDetailsMapper.toBatteryPostResponse(product, details, request, imageDtos);
    }

    @Transactional
    public List<SimilarProductResponse> getSimilarBatteries(String productId) {

        BatteryDetails details = batteryDetailRepository.findByProductsId(productId)
                .orElseThrow(() -> new AppException(ErrorCode.BATTERY_NOT_FOUND));

        String batteryTypeId = details.getBatteryType().getId();
        String brandId = details.getBrand().getId();

        List<Product> similar = new ArrayList<>(
                batteryDetailRepository.findSimilarBatteriesByType(batteryTypeId, productId));
        List<Product> similarBrand = batteryDetailRepository.findSimilarBatteriesByBrand(brandId, batteryTypeId,
                productId);

        for (Product p : similarBrand) {
            boolean alreadyExisted = similar.stream()
                    .anyMatch(sp -> sp.getId().equals(p.getId()));
            if (!alreadyExisted) {
                similar.add(p);
            }
        }

        return similar.stream()
                .map(p -> {
                    BatteryDetails b = batteryDetailRepository.findByProductsId(p.getId())
                            .orElse(null);

                    return SimilarProductResponse.builder()
                            .productId(p.getId())
                            .tittle(p.getTitle())
                            .price(p.getPrice())
                            .brandName(b != null && b.getBrand() != null ? b.getBrand().getName() : null)
                            .modelName(b != null && b.getBatteryType() != null ? b.getBatteryType().getName() : null)
                            .images(
                                    p.getImages() != null && !p.getImages().isEmpty()
                                            ? p.getImages().get(0).getImageUrl()
                                            : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BatteryDetailResponse getBatteryDetail(String productId) {
        BatteryDetails b = batteryDetailRepository.findByProductId(productId);
        if (b == null)
            throw new AppException(ErrorCode.BATTERY_NOT_FOUND);

        return BatteryDetailsMapper.toBatteryDetailResponse(b);
    }

    @Transactional
    public BatteryBrandsResponse updateBatteryBrand(String brandId, String brandName, MultipartFile logoFile) {
        log.info("Starting updateBatteryBrand with brandId={}, brandName='{}', hasLogoFile={}",
                brandId, brandName, logoFile != null);

        BatteryBrands brand = batteryBrandsRepository.findById(brandId)
                .orElseThrow(() -> {
                    log.warn("Battery brand not found with id={}", brandId);
                    return new AppException(ErrorCode.BATTERY_BRAND_NOT_FOUND);
                });

        boolean changed = false;

        // đổi tên (nếu có)
        if (brandName != null) {
            String newName = normalize(brandName);
            log.debug("Requested new battery brand name='{}' (normalized='{}')", brandName, newName);

            if (newName.isBlank()) {
                log.warn("Battery brand name is blank after normalization");
                throw new AppException(ErrorCode.BRAND_NAME_REQUIRED);
            }

            // chống trùng tên với brand khác
            batteryBrandsRepository.findByNameIgnoreCase(newName).ifPresent(other -> {
                if (!other.getId().equals(brand.getId())) {
                    log.warn("Duplicate battery brand name '{}' found (existing brandId={})", newName, other.getId());
                    throw new AppException(ErrorCode.BRAND_EXISTS);
                }
            });

            if (!newName.equalsIgnoreCase(brand.getName())) {
                log.info("Updating battery brand name from '{}' to '{}'", brand.getName(), newName);
                brand.setName(newName);
                changed = true;
            } else {
                log.debug("Battery brand name '{}' unchanged", brand.getName());
            }
        } else {
            log.debug("No brandName provided for battery brand update");
        }

        // đổi logo (nếu có)
        if (logoFile != null && !logoFile.isEmpty()) {
            log.info("Updating battery brand logo for brandId={}", brandId);
            validateLogo(logoFile);
            try {
                String url = uploadToCloudinary(logoFile, "eco-green/brands/battery");
                brand.setLogoUrl(url);
                changed = true;
                log.debug("Uploaded new battery brand logo successfully: {}", url);
            } catch (Exception e) {
                log.error("Battery logo upload failed for brandId={} - {}", brandId, e.getMessage(), e);
                throw e;
            }
        } else {
            log.debug("No logo file provided for battery brand update");
        }

        if (changed) {
            batteryBrandsRepository.save(brand);
            log.info("Battery brand updated successfully: id={}, name='{}', logo='{}'",
                    brand.getId(), brand.getName(), brand.getLogoUrl());
        } else {
            log.info("No changes detected for battery brandId={}", brandId);
        }

        BatteryBrandsResponse response = BatteryBrandMapper.mapToBatteryBrandsResponse(brand);
        log.debug("Returning BatteryBrandsResponse: {}", response);
        return response;
    }

    private String normalize(String s) {
        if (s == null)
            return "";
        String t = s.trim().replaceAll("\\s+", " ");
        return java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFC);
    }

    private void validateLogo(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new AppException(ErrorCode.MIN_1_IMAGE);
        }
        if (image.getSize() > 6L * 1024 * 1024) {
            throw new AppException(ErrorCode.IMAGE_TOO_LARGE);
        }
        String ct = image.getContentType() == null ? "" : image.getContentType();
        if (!(ct.equals("image/jpeg") || ct.equals("image/png"))) {
            throw new AppException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
    }

    private String uploadToCloudinary(MultipartFile file, String folder) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> up = (Map<String, Object>) cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "overwrite", true,
                            "unique_filename", true));
            return (String) up.get("secure_url");
        } catch (Exception e) {
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    @Transactional
    public BatteryBrandsResponse createBatteryBrand(String brandName, MultipartFile logoFile) {
        String name = normalize(brandName);
        log.info("Starting createBatteryBrand name='{}'", name);

        if (name == null || name.isBlank()) {
            log.warn("Battery brand name is blank");
            throw new AppException(ErrorCode.BRAND_NAME_REQUIRED);
        }

        // Chống trùng tên
        batteryBrandsRepository.findByNameIgnoreCase(name).ifPresent(b -> {
            log.warn("Battery brand '{}' already exists (id={})", name, b.getId());
            throw new AppException(ErrorCode.BRAND_EXISTS);
        });

        // Bắt buộc logo khi tạo mới
        validateLogo(logoFile);

        String secureUrl;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> up = (Map<String, Object>) cloudinary.uploader().upload(
                    logoFile.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "eco-green/brands/battery",
                            "resource_type", "image",
                            "overwrite", true,
                            "unique_filename", true));
            secureUrl = (String) up.get("secure_url");
            log.debug("Uploaded battery brand logo OK: {}", secureUrl);
        } catch (Exception e) {
            log.error("Cloudinary upload error: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        BatteryBrands entity = new BatteryBrands();
        entity.setName(name);
        entity.setLogoUrl(secureUrl);
        BatteryBrands saved = batteryBrandsRepository.save(entity);

        log.info("Created battery brand id={} name='{}'", saved.getId(), saved.getName());
        return BatteryMapper.toBrandRes(saved);
    }

    @Transactional
    public BatteryTypeResponse createBatteryType(CreateBatteryTypeRequest req) {
        String typeName = normalize(req.getName());
        log.info("Starting createBatteryType name='{}'", typeName);

        if (typeName == null || typeName.isBlank()) {
            log.warn("Battery type name blank");
            throw new AppException(ErrorCode.BATTERY_TYPE_NAME_REQUIRED);
        }

        // Chống trùng tên
        batteryTypeRepository.findByNameIgnoreCase(typeName).ifPresent(t -> {
            log.warn("Battery type '{}' already exists (id={})", typeName, t.getId());
            throw new AppException(ErrorCode.BRAND_EXISTS); // hoặc tạo ErrorCode riêng: BATTERY_TYPE_EXISTS
        });

        BatteryTypes entity = new BatteryTypes();
        entity.setName(typeName);
        BatteryTypes saved = batteryTypeRepository.save(entity);

        log.info("Created battery type id={} name='{}'", saved.getId(), saved.getName());
        return BatteryMapper.toTypeRes(saved);
    }

}