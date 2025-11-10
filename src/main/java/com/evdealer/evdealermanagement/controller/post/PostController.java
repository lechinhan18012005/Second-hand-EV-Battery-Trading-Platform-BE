package com.evdealer.evdealermanagement.controller.post;

import com.evdealer.evdealermanagement.dto.account.custom.CustomAccountDetails;
import com.evdealer.evdealermanagement.dto.post.battery.BatteryPostRequest;
import com.evdealer.evdealermanagement.dto.post.battery.BatteryPostResponse;
import com.evdealer.evdealermanagement.dto.post.vehicle.VehiclePostRequest;
import com.evdealer.evdealermanagement.dto.post.vehicle.VehiclePostResponse;
import com.evdealer.evdealermanagement.service.implement.PostService;
import com.evdealer.evdealermanagement.utils.JsonValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/post/products")
public class PostController {

    private final PostService postService;

    @PostMapping(value = "/battery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BatteryPostResponse postBattery(
            @RequestPart("data") String dataJson,
            @RequestPart("images") List<MultipartFile> images,
            @RequestPart(value = "imagesMeta", required = false) String imagesMetaJson,
            @AuthenticationPrincipal CustomAccountDetails user) throws Exception {
        String sellerId = user !=  null ? user.getAccountId() : null;

        BatteryPostRequest request = JsonValidationUtils.parseAndValidateJson(
                dataJson,
                BatteryPostRequest.class,
                this,
                "postBattery",
                String.class, List.class, String.class, CustomAccountDetails.class
        );


        return postService.createBatteryPost(sellerId, request, images, imagesMetaJson);
    }

    @PostMapping(value = "/vehicle", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VehiclePostResponse postVehicle(
            @RequestPart("data") String dataJson,
            @RequestPart("images") List<MultipartFile> images,
            @RequestPart(value = "imagesMeta", required = false) String imagesMetaJson,
            @AuthenticationPrincipal CustomAccountDetails user) throws Exception {
        String sellerId = user !=  null ? user.getAccountId() : null;

        VehiclePostRequest request = JsonValidationUtils.parseAndValidateJson(
                dataJson,
                VehiclePostRequest.class,
                this,
                "postVehicle",
                String.class, List.class, String.class, CustomAccountDetails.class
        );

        return postService.createVehiclePost(sellerId, request, images, imagesMetaJson);
    }
}
