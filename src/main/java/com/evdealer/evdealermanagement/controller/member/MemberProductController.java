package com.evdealer.evdealermanagement.controller.member;

import com.evdealer.evdealermanagement.dto.account.custom.CustomAccountDetails;
import com.evdealer.evdealermanagement.dto.battery.update.BatteryUpdateProductRequest;
import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.dto.post.battery.BatteryPostRequest;
import com.evdealer.evdealermanagement.dto.post.battery.BatteryPostResponse;
import com.evdealer.evdealermanagement.dto.post.vehicle.VehiclePostRequest;
import com.evdealer.evdealermanagement.dto.post.vehicle.VehiclePostResponse;
import com.evdealer.evdealermanagement.dto.product.detail.ProductDetail;
import com.evdealer.evdealermanagement.dto.product.status.ProductActiveOrHiddenResponse;
import com.evdealer.evdealermanagement.dto.product.status.ProductStatusRequest;
import com.evdealer.evdealermanagement.dto.product.status.ProductStatusResponse;
import com.evdealer.evdealermanagement.dto.vehicle.update.VehicleUpdateProductRequest;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.service.implement.BatteryService;
import com.evdealer.evdealermanagement.service.implement.MemberService;
import com.evdealer.evdealermanagement.service.implement.ProductService;
import com.evdealer.evdealermanagement.service.implement.VehicleService;
import com.evdealer.evdealermanagement.utils.JsonValidationUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member/product")
public class MemberProductController {

    private final MemberService memberService;
    private final BatteryService batteryService;
    private final VehicleService vehicleService;
    private final ProductService productService;

    @GetMapping
    public List<ProductDetail> getProductsByStatus(
            Authentication authentication,
            @RequestParam Product.Status status) {
        CustomAccountDetails customAccountDetails = (CustomAccountDetails) authentication.getPrincipal();

        String sellerId = customAccountDetails.getAccountId();

        return memberService.getProductsByStatus(sellerId, status);
    }

    @PostMapping("/sold")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ProductStatusResponse> markSold(Authentication authentication,
            @RequestBody ProductStatusRequest req) {
        CustomAccountDetails customAccountDetails = (CustomAccountDetails) authentication.getPrincipal();
        String sellerId = customAccountDetails.getAccountId();

        return ResponseEntity.ok(memberService.markSold(sellerId, req.getProductId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetail> getProductDetailOfMember(Authentication authentication,
            @PathVariable("id") String productId) {
        CustomAccountDetails customAccountDetails = (CustomAccountDetails) authentication.getPrincipal();

        String sellerId = customAccountDetails.getAccountId();

        ProductDetail productDetail = memberService.getProductDetailOfMember(sellerId, productId);
        return ResponseEntity.ok(productDetail);
    }

    @PutMapping(value = "/battery/update/{productId}/draft", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MEMBER')")
    public BatteryPostResponse updateBattery(
            @PathVariable String productId,
            @RequestPart(value = "data", required = false) String dataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "imagesMeta", required = false) String imagesMetaJson,
            @AuthenticationPrincipal CustomAccountDetails user) throws Exception {
        BatteryUpdateProductRequest request = null;
        if (dataJson != null && !dataJson.isBlank()) {
            request = JsonValidationUtils.parseAndValidateJson(
                    dataJson,
                    BatteryUpdateProductRequest.class,
                    this,
                    "updateBattery",
                    String.class, // @PathVariable String productId
                    String.class, // @RequestPart("data")
                    List.class, // @RequestPart("images")
                    String.class, // @RequestPart("imagesMeta")
                    CustomAccountDetails.class // @AuthenticationPrincipal
            );
        }
        return batteryService.updateBatteryPost(user.getAccountId(), productId, request, images, imagesMetaJson);
    }

    @PutMapping(value = "/battery/update/{productId}/rejected", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MEMBER')")
    public BatteryPostResponse updateBatteryRejected(
            @PathVariable String productId,
            @RequestPart(value = "data", required = false) String dataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "imagesMeta", required = false) String imagesMetaJson,
            @AuthenticationPrincipal CustomAccountDetails user) throws Exception {
        BatteryUpdateProductRequest request = null;
        if (dataJson != null && !dataJson.isBlank()) {
            request = JsonValidationUtils.parseAndValidateJson(
                    dataJson,
                    BatteryUpdateProductRequest.class,
                    this,
                    "updateBattery",
                    String.class, // @PathVariable String productId
                    String.class, // @RequestPart("data")
                    List.class, // @RequestPart("images")
                    String.class, // @RequestPart("imagesMeta")
                    CustomAccountDetails.class // @AuthenticationPrincipal
            );
        }
        return batteryService.updateBatteryPostRejected(user.getAccountId(), productId, request, images, imagesMetaJson);
    }

    @PutMapping(value = "/vehicle/update/{productId}/draft", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MEMBER')")
    public VehiclePostResponse updateVehicle(
            @PathVariable("productId") String productId,
            @RequestPart(value = "data", required = false) String dataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "imagesMeta", required = false) String imagesMetaJson,
            @AuthenticationPrincipal CustomAccountDetails user) throws Exception {
        VehicleUpdateProductRequest request = null;

        if (dataJson != null && !dataJson.isBlank()) {
            request = JsonValidationUtils.parseAndValidateJson(
                    dataJson,
                    VehicleUpdateProductRequest.class,
                    this,
                    "updateVehicle",
                    String.class, // productId
                    String.class, // data
                    List.class, // images
                    String.class, // imagesMeta
                    CustomAccountDetails.class // user
            );
        }
        return vehicleService.updateVehiclePost(user.getAccountId(), productId, request, images, imagesMetaJson);
    }

    @PutMapping(value = "/vehicle/update/{productId}/rejected", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MEMBER')")
    public VehiclePostResponse updateVehicleRejected(
            @PathVariable("productId") String productId,
            @RequestPart(value = "data", required = false) String dataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "imagesMeta", required = false) String imagesMetaJson,
            @AuthenticationPrincipal CustomAccountDetails user) throws Exception {
        VehicleUpdateProductRequest request = null;

        if (dataJson != null && !dataJson.isBlank()) {
            request = JsonValidationUtils.parseAndValidateJson(
                    dataJson,
                    VehicleUpdateProductRequest.class,
                    this,
                    "updateVehicle",
                    String.class, // productId
                    String.class, // data
                    List.class, // images
                    String.class, // imagesMeta
                    CustomAccountDetails.class // user
            );
        }
        return vehicleService.updateVehiclePostRejected(user.getAccountId(), productId, request, images, imagesMetaJson);
    }

    @GetMapping("/seller/{sellerId}/products")
    public PageResponse<ProductDetail> listActiveProductsBySeller(
            @PathVariable("sellerId") String sellerId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return productService.listActiveOrSoldBySeller(sellerId, pageable);
    }

    @PutMapping("/hide")
    public ResponseEntity<ProductActiveOrHiddenResponse> hideProduct(
            @RequestParam("productId") String productId,
            @RequestParam("status") String status, // kỳ vọng = HIDDEN để FE “xác nhận chủ đích”
            @AuthenticationPrincipal CustomAccountDetails user) {
        ProductActiveOrHiddenResponse res = productService.hideProduct(user.getAccountId(), productId, status);
        return ResponseEntity.ok(res);
    }

    @PutMapping("/active")
    public ResponseEntity<ProductActiveOrHiddenResponse> activeProduct(
            @RequestParam("productId") String productId,
            @RequestParam("status") String status, // kỳ vọng = ACTIVE
            @AuthenticationPrincipal CustomAccountDetails user) {
        ProductActiveOrHiddenResponse res = productService.activeProduct(user.getAccountId(), productId, status);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/battery/{productId}")
    public ResponseEntity<BatteryPostResponse> getBatteryById(@PathVariable String productId) {
        return ResponseEntity.ok(batteryService.getBatteryPostById(productId));
    }

    @GetMapping("/vehicle/{productId}")
    public ResponseEntity<VehiclePostResponse> getVehicleById(@PathVariable String productId) {
        return ResponseEntity.ok(vehicleService.getVehiclePostById(productId));
    }

    @GetMapping("/bought-products")
    public ResponseEntity<Page<ProductDetail>> getBoughtProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDetail> result = memberService.getBoughtProduct(pageable);
        return ResponseEntity.ok(result);
    }
}
