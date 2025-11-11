package com.evdealer.evdealermanagement.controller.account;

import com.evdealer.evdealermanagement.dto.account.profile.AccountProfileResponse;
import com.evdealer.evdealermanagement.dto.account.profile.AccountUpdateRequest;
import com.evdealer.evdealermanagement.dto.account.profile.ProfilePublicDto;
import com.evdealer.evdealermanagement.dto.product.detail.ProductDetail;
import com.evdealer.evdealermanagement.service.implement.ProfileService;
import com.evdealer.evdealermanagement.utils.JsonValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN') or hasRole('STAFF')")
    public AccountProfileResponse getCurrentProfile(Authentication authentication) {
        String username = authentication.getName();
        return profileService.getProfile(username);
    }

    @PatchMapping(value = "/me/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<AccountProfileResponse> updateProfile(
            @RequestPart(value = "data", required = false) String dataJson,
            @RequestPart(value = "avatarUrl", required = false) MultipartFile avatarUrl,
            Authentication authentication
    ) throws Exception {
        AccountUpdateRequest request = JsonValidationUtils.parseAndValidateJson(
                dataJson,
                AccountUpdateRequest.class,
                this,
                "updateProfile",
                String.class,
                MultipartFile.class,
                Authentication.class
        );
        String username = authentication.getName();
        return ResponseEntity.ok(
                profileService.updateProfile(username, request, avatarUrl)
        );
    }


    @GetMapping("/public")
    public ResponseEntity<ProfilePublicDto> getProfilePublic(@RequestParam String sellerId) {
        return ResponseEntity.ok(profileService.getPublicProfile(sellerId));
    }

    @GetMapping("/public/products/active")
    public ResponseEntity<List<ProductDetail>> getActiveProducts(@RequestParam String sellerId) {
        return ResponseEntity.ok(profileService.getActiveProductsOfSeller(sellerId));
    }

    @GetMapping("/public/products/sold")
    public ResponseEntity<List<ProductDetail>> getSoldProducts(@RequestParam String sellerId) {
        return ResponseEntity.ok(profileService.getSoldProductsOfSeller(sellerId));
    }
}
