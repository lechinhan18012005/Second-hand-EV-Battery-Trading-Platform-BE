package com.evdealer.evdealermanagement.service.implement;

import java.time.LocalDateTime;

import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.evdealer.evdealermanagement.dto.account.password.PasswordResponse;
import com.evdealer.evdealermanagement.dto.account.password.ChangePasswordRequest;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.repository.AccountRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChangePasswordService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PasswordResponse changePassword(String username, ChangePasswordRequest req) {

        if (!req.getNewPassword().equals(req.getConfirmNewPassword())) {
            return PasswordResponse.builder()
                    .success(false)
                    .message("New password and confirmation do not match.")
                    .build();
        }

        Account acc = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User Not Found"));

        // 1) Xác thực mật khẩu hiện tại
        if (!passwordEncoder.matches(req.getCurrentPassword(), acc.getPasswordHash())) {
            return PasswordResponse.builder()
                    .success(false)
                    .message("The current password is incorrect.")
                    .build();
        }

        // 2) Chặn trùng mật khẩu cũ
        if (passwordEncoder.matches(req.getNewPassword(), acc.getPasswordHash())) {
            return PasswordResponse.builder()
                    .success(false)
                    .message("The new password cannot be the same as the current password.")
                    .build();
        }

        // 3) Cập nhật hash
        String newHash = passwordEncoder.encode(req.getNewPassword());
        acc.setPasswordHash(newHash);
        acc.setUpdatedAt(VietNamDatetime.nowVietNam());
        accountRepository.save(acc);

        return PasswordResponse.builder()
                .success(true)
                .message("Password changed successfully. Please login again.")
                .build();
    }
}
