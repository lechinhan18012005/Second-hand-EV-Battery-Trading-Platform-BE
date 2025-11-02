package com.evdealer.evdealermanagement.controller.admin;

import com.evdealer.evdealermanagement.dto.account.ban.BanRequest;
import com.evdealer.evdealermanagement.dto.account.custom.CustomAccountDetails;
import com.evdealer.evdealermanagement.dto.account.delete.DeleteRequest;
import com.evdealer.evdealermanagement.dto.account.register.AccountRegisterRequest;
import com.evdealer.evdealermanagement.dto.account.register.AccountRegisterResponse;
import com.evdealer.evdealermanagement.dto.account.response.ApiResponse;
import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.service.implement.AdminService;
import com.evdealer.evdealermanagement.service.implement.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAccountManagementController {

    private final AdminService adminService;
    private final AuthService authService;

    @GetMapping("/manage/account/member")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<Account>> getMemberAccount(@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.getMemberAccounts(pageable));
    }

    @GetMapping("/manage/account/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<Account>> getStaffAccount(@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.getStaffAccounts(pageable));
    }

    @DeleteMapping("/manage/account/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAccount(@PathVariable String id) {
        boolean deleted = adminService.deleteAccount(id);
        if (deleted) {
            return ResponseEntity.ok("Account deleted successfully");
        } else {
            return ResponseEntity.status(404).body("Account not found");
        }
    }

    @PutMapping("/manage/account/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> changeAccountStatus(
            @PathVariable String id,
            @RequestParam Account.Status status
    ) {
        boolean updated = adminService.changeStatusAccount(id, status);
        if (updated) {
            return ResponseEntity.ok("Account status updated successfully");
        } else {
            return ResponseEntity.status(404).body("Account not found");
        }
    }

    @PostMapping("/register/staff")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AccountRegisterResponse> registerStaffAccount(@Valid @RequestBody AccountRegisterRequest request) {
        AccountRegisterResponse response = authService.registerStaffAccount(request);
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), response);
    }

    @PutMapping("/ban/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> banAccount(@PathVariable String accountId, @RequestBody BanRequest request) {
        adminService.banAccount(accountId, request.getReason());
        return ResponseEntity.ok("Account banned with reason: " + request.getReason());
    }

    @PutMapping("/unban/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> unbanAccount(@PathVariable String accountId) {
        adminService.unBanAccount(accountId);
        return ResponseEntity.ok("Account has been unbanned");
    }

    @DeleteMapping("/delete/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAccount(@PathVariable String accountId,
                                                @RequestBody DeleteRequest request,
                                                @AuthenticationPrincipal CustomAccountDetails adminDetails) {
        String adminPassword = request.getAdminPassword();
        adminService.deleteAccountForAdmin(accountId, adminPassword, adminDetails);

        return ResponseEntity.ok("Account deleted successfully.");
    }
}
