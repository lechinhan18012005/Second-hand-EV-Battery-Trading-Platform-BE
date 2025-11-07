package com.evdealer.evdealermanagement.controller.member;

import com.evdealer.evdealermanagement.dto.account.custom.CustomAccountDetails;
import com.evdealer.evdealermanagement.dto.post.battery.BatteryPostRequest;
import com.evdealer.evdealermanagement.dto.post.battery.BatteryPostResponse;
import com.evdealer.evdealermanagement.dto.post.vehicle.VehiclePostRequest;
import com.evdealer.evdealermanagement.dto.post.vehicle.VehiclePostResponse;
import com.evdealer.evdealermanagement.dto.product.detail.ProductDetail;
import com.evdealer.evdealermanagement.dto.product.status.ProductStatusRequest;
import com.evdealer.evdealermanagement.dto.product.status.ProductStatusResponse;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.service.implement.BatteryService;
import com.evdealer.evdealermanagement.service.implement.MemberService;
import com.evdealer.evdealermanagement.service.implement.VehicleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
            @AuthenticationPrincipal CustomAccountDetails user) throws JsonProcessingException {
        BatteryPostRequest request = null;
        if (dataJson != null && !dataJson.isBlank()) {
            request = new ObjectMapper().readValue(dataJson, BatteryPostRequest.class);
        }
        return batteryService.updateBatteryPost(productId, request, images, imagesMetaJson);
    }

    @PutMapping(value = "/vehicle/update/{productId}/draft", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MEMBER')")
    public VehiclePostResponse updateVehicle(
            @PathVariable("productId") String productId,
            @RequestPart(value = "data", required = false) String dataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "imagesMeta", required = false) String imagesMetaJson,
            @AuthenticationPrincipal CustomAccountDetails user) throws JsonProcessingException {
        VehiclePostRequest request = null;
        if (dataJson != null && !dataJson.isBlank()) {
            request = new ObjectMapper().readValue(dataJson, VehiclePostRequest.class);
        }
        return vehicleService.updateVehiclePost(productId, request, images, imagesMetaJson);
    }

}
