package com.evdealer.evdealermanagement.service.implement;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.evdealer.evdealermanagement.dto.post.battery.BatteryPostRequest;
import com.evdealer.evdealermanagement.dto.post.battery.BatteryPostResponse;
import com.evdealer.evdealermanagement.dto.post.common.ImageMeta;
import com.evdealer.evdealermanagement.dto.post.common.ProductImageResponse;
import com.evdealer.evdealermanagement.dto.post.vehicle.VehiclePostRequest;
import com.evdealer.evdealermanagement.dto.post.vehicle.VehiclePostResponse;
import com.evdealer.evdealermanagement.entity.battery.BatteryDetails;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.entity.product.ProductImages;
import com.evdealer.evdealermanagement.entity.vehicle.VehicleDetails;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.mapper.product.ProductMapper;
import com.evdealer.evdealermanagement.repository.*;
import com.evdealer.evdealermanagement.service.contract.IProductPostService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostService implements IProductPostService {

    private final ProductRepository productRepository;
    private final VehicleDetailsRepository vehicleDetailsRepository;
    private final BatteryDetailRepository batteryDetailRepository;
    private final ProductImagesRepository productImagesRepository;
    private final BatteryBrandsRepository batteryBrandsRepository;
    private final BatteryTypesRepository batteryTypesRepository;
    private final VehicleBrandsRepository veBrandsRepo;
    private final VehicleCategoryRepository veCateRepo;
    private final Cloudinary cloudinary;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AccountRepository accountRepository;
    private final VehicleModelVersionRepository vmvRepo;
    private final VehicleModelRepository vehicleModelRepository;

    @Override
    public BatteryPostResponse createBatteryPost(String sellerId, BatteryPostRequest request,
            List<MultipartFile> images, String imagesMetaJson) {
        validateImages(images);

        Product product = productRepository.save(Product.builder()
                .seller(accountRepository.findById(sellerId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)))
                .type(Product.ProductType.BATTERY)
                .status(Product.Status.DRAFT)
                .conditionType(Product.ConditionType.USED)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .sellerPhone(accountRepository.getPhone(sellerId))
                .city(request.getCity())
                .district(request.getDistrict())
                .ward(request.getWard())
                .addressDetail(request.getAddressDetail())
                .build());

        BatteryDetails bd = batteryDetailRepository.save(BatteryDetails.builder()
                .product(product)
                .batteryType(batteryTypesRepository.findById(request.getBatteryTypeId())
                        .orElseThrow(() -> new AppException(ErrorCode.BATT_NOT_FOUND)))
                .brand(batteryBrandsRepository.findById(request.getBrandId())
                        .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND)))
                .capacityKwh(request.getCapacityKwh())
                .healthPercent(request.getHealthPercent())
                .voltageV(request.getVoltageV())
                .build());

        List<ProductImageResponse> imageDtos = uploadAndSaveImages(product, images, imagesMetaJson);
        return BatteryPostResponse.builder()
                .productId(product.getId())
                .status(String.valueOf(product.getStatus()))
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .sellerPhone(product.getSellerPhone())
                .city(product.getCity())
                .district(product.getDistrict())
                .ward(product.getWard())
                .addressDetail(product.getAddressDetail())
                .createdAt(product.getCreatedAt())
                .brandId(bd.getBrand().getId())
                .brandName(bd.getBrand().getName())
                .batteryTypeName(bd.getBatteryType().getName())
                .capacityKwh(bd.getCapacityKwh())
                .healthPercent(bd.getHealthPercent())
                .voltageV(bd.getVoltageV())
                .images(imageDtos)
                .build();

    }

    @Override
    public VehiclePostResponse createVehiclePost(String sellerId, VehiclePostRequest request,
            List<MultipartFile> images, String imagesMetaJson) {
        Product product = productRepository.save(Product.builder()
                .seller(accountRepository.findById(sellerId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)))
                .type(Product.ProductType.VEHICLE)
                .status(Product.Status.DRAFT)
                .conditionType(Product.ConditionType.USED)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .sellerPhone(accountRepository.getPhone(sellerId))
                .city(request.getCity())
                .district(request.getDistrict())
                .ward(request.getWard())
                .addressDetail(request.getAddressDetail())
                .manufactureYear(request.getYear())
                .build());

        VehicleDetails vd = vehicleDetailsRepository.save(VehicleDetails.builder()
                .product(product)
                .category(veCateRepo.findById(request.getCategoryId())
                        .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_CATE_NOT_FOUND)))
                .brand(veBrandsRepo.findById(request.getBrandId())
                        .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND)))
                .batteryHealthPercent(request.getBatteryHealthPercent())
                .mileageKm(request.getMileageKm())
                .model(vehicleModelRepository.findById(request.getModelId()).orElse(null))
                .version(request.getVersionId() != null ? vmvRepo.findById(request.getVersionId())
                        .orElseThrow(() -> new AppException(ErrorCode.VERSION_NOT_FOUND)) : null)
                .build());

        List<ProductImageResponse> imageDtos = uploadAndSaveImages(product, images, imagesMetaJson);
        return VehiclePostResponse.builder()
                .productId(product.getId())
                .status(String.valueOf(product.getStatus()))
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .sellerPhone(product.getSellerPhone())
                .city(product.getCity())
                .district(product.getDistrict())
                .ward(product.getWard())
                .addressDetail(product.getAddressDetail())
                .createdAt(product.getCreatedAt())
                .categoryId(vd.getCategory().getId())
                .brandId(vd.getBrand().getId())
                .categoryName(vd.getCategory().getName())
                .brandName(vd.getBrand().getName())
                .batteryHealthPercent(vd.getBatteryHealthPercent())
                .mileageKm(vd.getMileageKm())
                .modelName(vd.getModel().getName())
                .warrantyMonths(vd.getWarrantyMonths())
                .hasInsurance(vd.getHasInsurance())
                .hasRegistration(vd.getHasRegistration())
                .images(imageDtos)
                .build();

    }

    // Support Method
    public void validateImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw new AppException(ErrorCode.MIN_1_IMAGE);
        }
        if (images.size() > 10) {
            throw new AppException(ErrorCode.MAX_10_IMAGES);
        }
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                throw new AppException(ErrorCode.MIN_1_IMAGE);
            }
            long maxBytes = 15L * 1024 * 1024;
            if (image.getSize() > maxBytes) {
                throw new AppException(ErrorCode.IMAGE_TOO_LARGE);
            }
            String ct = image.getContentType() == null ? "" : image.getContentType();
            if (!ct.startsWith("image/")) {
                throw new AppException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
            }
        }
    }

    public Map<Integer, ImageMeta> parseMeta(String json) {
        if (json == null || json.isBlank())
            return Map.of();
        try {
            var list = objectMapper.readValue(json, new TypeReference<List<ImageMeta>>() {
            });
            Map<Integer, ImageMeta> map = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                map.put(i, list.get(i));
            }
            return map;
        } catch (Exception e) {
            return Map.of();
        }

    }

    public List<ProductImageResponse> uploadAndSaveImages(Product product, List<MultipartFile> files, String metaJson) {
        Map<Integer, ImageMeta> meta = parseMeta(metaJson);

        // check if meta specifies primary
        boolean hasExplicitPrimary = meta.values().stream()
                .anyMatch(imageMeta -> imageMeta != null && Boolean.TRUE.equals(imageMeta.getIsPrimary()));

        boolean primarySet = false;
        int i = 0;
        List<ProductImageResponse> dtos = new ArrayList<>();

        Map<String, Object> res;
        for (MultipartFile file : files) {
            log.info("img name={}, size={}, ct={}", file.getOriginalFilename(), file.getSize(), file.getContentType());

            try {

                @SuppressWarnings("unchecked")
                Map<String, Object> uploaded = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "eco-green/products/" + product.getId(),
                                "resource_type", "image",
                                "overwrite", true,
                                "unique_filename", true));
                res = uploaded;
            } catch (Exception e) {
                log.error("Cloudinary upload error: {}", e.getMessage(), e);
                throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }

            var m = meta.get(i);
            boolean isPrimary = false;
            if (m != null && Boolean.TRUE.equals(m.getIsPrimary())) {
                if (!primarySet) {
                    isPrimary = true;
                    primarySet = true;
                } else {
                    isPrimary = false;
                }
            } else if (!hasExplicitPrimary && !primarySet && i == 0) {
                isPrimary = true;
                primarySet = true;
            }

            ProductImages saved = productImagesRepository.save(ProductImages.builder()
                    .product(product)
                    .imageUrl((String) res.get("secure_url"))
                    .publicId((String) res.get("public_id"))
                    .width((Integer) res.get("width"))
                    .height((Integer) res.get("height"))
                    .bytes((Integer) res.get("bytes"))
                    .format((String) res.get("format"))
                    .position(m != null && m.getPosition() != null ? m.getPosition() : i)
                    .isPrimary(isPrimary)
                    .build());

            ProductImageResponse dto = ProductMapper.toMapDto(saved);
            dtos.add(dto);
            i++;
        }
        return dtos;
    }
}
