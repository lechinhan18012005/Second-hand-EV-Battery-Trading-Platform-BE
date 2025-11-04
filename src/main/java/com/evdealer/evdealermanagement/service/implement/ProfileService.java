package com.evdealer.evdealermanagement.service.implement;

import com.cloudinary.Cloudinary;
import com.evdealer.evdealermanagement.dto.account.profile.AccountProfileResponse;
import com.evdealer.evdealermanagement.dto.account.profile.AccountUpdateRequest;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.mapper.account.AccountMapper;
import com.evdealer.evdealermanagement.repository.AccountRepository;
import com.evdealer.evdealermanagement.service.contract.IAccountService;


import java.util.Map;

import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.apache.logging.log4j.util.Strings.trimToNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService implements IAccountService {

    private final AccountRepository accountRepository;
    private final Cloudinary cloudinary;


    @Override
    public AccountProfileResponse getProfile(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
        return AccountMapper.mapToAccountProfileResponse(account);
    }

    @Override
    public AccountProfileResponse updateProfile(String username, AccountUpdateRequest accountRequest) {

        Account existingAccount = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        // Validate phone duplicate - sử dụng StringUtils.hasText
        if (StringUtils.hasText(accountRequest.getPhone())) {
            String trimmedPhone = trimToNull(accountRequest.getPhone());
            if (trimmedPhone != null &&
                    accountRepository.existsByPhoneAndIdNot(trimmedPhone, existingAccount.getId())) {
                throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "Phone already used");
            }
        }

        // Validate email duplicate
        if (StringUtils.hasText(accountRequest.getEmail())) {
            String trimmedEmail = trimToNull(accountRequest.getEmail());
            if (trimmedEmail != null &&
                    accountRepository.existsByEmailAndIdNot(trimmedEmail, existingAccount.getId())) {
                throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "Email already used");
            }
        }

        AccountMapper.updateAccountFromRequest(accountRequest, existingAccount);
        //Chỉ update avatar nếu có file
        if(accountRequest.getAvatarUrl() != null && !accountRequest.getAvatarUrl().isEmpty()) {
            validateAvatar(accountRequest.getAvatarUrl());
            String avatarUrl = uploadAvatarToCloudinary(accountRequest.getAvatarUrl(), existingAccount.getId());
            existingAccount.setAvatarUrl(avatarUrl);
        }
        existingAccount.setUpdatedAt(VietNamDatetime.nowVietNam());

        Account saved = accountRepository.save(existingAccount);
        return AccountMapper.mapToAccountProfileResponse(saved);
    }

    @Override
    public void deleteAccount(String username) {
        Account acc = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        accountRepository.delete(acc);
    }

    @SuppressWarnings("unchecked")
    private String uploadAvatarToCloudinary(MultipartFile avatar, String accountId) {
        try {
            Map<String, Object> uploaded = (Map<String, Object>) cloudinary.uploader().upload(
                    avatar.getBytes(),
                    com.cloudinary.utils.ObjectUtils.asMap(
                            "folder", "eco-green/avatars/" + accountId,  // tách thư mục riêng
                            "public_id", "avatar_" + accountId,          // để sau này update thì ghi đè
                            "overwrite", true,
                            "resource_type", "image"
                    )
            );
            return (String) uploaded.get("secure_url");
        } catch (Exception e) {
            log.error("Cloudinary upload avatar error: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }


    private void validateAvatar(MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new AppException(ErrorCode.MIN_1_IMAGE); // có thể tạo ErrorCode riêng như AVATAR_REQUIRED
        }

        //5 MB
        long maxBytes = 5L * 1024 * 1024;
        if (avatar.getSize() > maxBytes) {
            throw new AppException(ErrorCode.IMAGE_TOO_LARGE);
        }

        String ct = avatar.getContentType() == null ? "" : avatar.getContentType();
        if (!(ct.equals("image/jpeg") || ct.equals("image/png"))) {
            throw new AppException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
    }
}