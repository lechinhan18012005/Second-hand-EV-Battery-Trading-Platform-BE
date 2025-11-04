package com.evdealer.evdealermanagement.controller.account;

import com.evdealer.evdealermanagement.dto.account.profile.AccountProfileResponse;
import com.evdealer.evdealermanagement.dto.account.profile.AccountUpdateRequest;
import com.evdealer.evdealermanagement.service.implement.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("/me/update")
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<AccountProfileResponse> updateProfile(
            @Valid @ModelAttribute AccountUpdateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(profileService.updateProfile(username, request));
    }

}
