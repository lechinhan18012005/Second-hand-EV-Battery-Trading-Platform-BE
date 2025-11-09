package com.evdealer.evdealermanagement.controller.account;

import com.evdealer.evdealermanagement.dto.account.profile.AccountProfileResponse;
import com.evdealer.evdealermanagement.dto.account.profile.AccountUpdateRequest;
import com.evdealer.evdealermanagement.dto.account.profile.ProfilePublicDto;
import com.evdealer.evdealermanagement.service.implement.ProfileService;
import com.evdealer.evdealermanagement.utils.JsonValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    public ResponseEntity<ProfilePublicDto> getProfilePublic(@RequestParam String username) {
        return ResponseEntity.ok(profileService.getPublicProfile(username));
    }

}
