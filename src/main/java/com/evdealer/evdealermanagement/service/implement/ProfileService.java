package com.evdealer.evdealermanagement.service.implement;

import com.cloudinary.Cloudinary;
import com.evdealer.evdealermanagement.dto.account.profile.AccountProfileResponse;
import com.evdealer.evdealermanagement.dto.account.profile.AccountUpdateRequest;
import com.evdealer.evdealermanagement.dto.account.profile.ProfilePublicDto;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.mapper.account.AccountMapper;
import com.evdealer.evdealermanagement.repository.AccountRepository;
import com.evdealer.evdealermanagement.service.contract.IAccountService;

import java.time.LocalDate;
import java.util.Map;

import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.yaml.snakeyaml.util.EnumUtils;

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
    public AccountProfileResponse updateProfile(String username, AccountUpdateRequest accountRequest, MultipartFile avatarUrl) {
        Account existingAccount = accountRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found by username='{}'", username);
                    return new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found");
                });
        log.debug("Loaded account: id={}, email='{}', phone='{}'",
                existingAccount.getId(), existingAccount.getEmail(), existingAccount.getPhone());

        // Validate phone duplicate - sử dụng StringUtils.hasText
        if (StringUtils.hasText(accountRequest.getPhone())) {
            String trimmedPhone = trimToNull(accountRequest.getPhone());
            log.debug("Validating phone duplication: raw='{}', trimmed='{}'",
                    accountRequest.getPhone(), trimmedPhone);

            if (trimmedPhone != null &&
                    accountRepository.existsByPhoneAndIdNot(trimmedPhone, existingAccount.getId())) {
                log.warn("Duplicate phone detected for phone='{}' (other user exists)", trimmedPhone);
                throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "Phone already used");
            }
        } else {
            log.debug("No phone provided to update");
        }

        // Validate email duplicate
        if (StringUtils.hasText(accountRequest.getEmail())) {
            String trimmedEmail = trimToNull(accountRequest.getEmail());
            log.debug("Validating email duplication: raw='{}', trimmed='{}'",
                    accountRequest.getEmail(), trimmedEmail);

            if (trimmedEmail != null &&
                    accountRepository.existsByEmailAndIdNot(trimmedEmail, existingAccount.getId())) {
                log.warn("Duplicate email detected for email='{}' (other user exists)", trimmedEmail);
                throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "Email already used");
            }
        } else {
            log.debug("No email provided to update");
        }

        // Map fields từ request sang entity
        log.debug("Applying AccountMapper.updateAccountFromRequest");
        AccountMapper.updateAccountFromRequest(accountRequest, existingAccount);

        //Chỉ update avatar nếu có file
        if(avatarUrl != null && !avatarUrl.isEmpty()) {
            validateAvatar(avatarUrl);
            String avatar = uploadAvatarToCloudinary(avatarUrl, existingAccount.getId());
            existingAccount.setAvatarUrl(avatar);
        }

        existingAccount.setUpdatedAt(VietNamDatetime.nowVietNam());
        log.debug("Set updatedAt for accountId={}", existingAccount.getId());

        Account saved = accountRepository.save(existingAccount);
        log.info("Account updated successfully: id={}, username='{}'", saved.getId(), username);

        AccountProfileResponse resp = AccountMapper.mapToAccountProfileResponse(saved);
        log.debug("Returning AccountProfileResponse: {}", resp);
        return resp;
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
                            "folder", "eco-green/avatars/" + accountId, // tách thư mục riêng
                            "public_id", "avatar_" + accountId, // để sau này update thì ghi đè
                            "overwrite", true,
                            "resource_type", "image"));
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

        // 5 MB
        long maxBytes = 5L * 1024 * 1024;
        if (avatar.getSize() > maxBytes) {
            throw new AppException(ErrorCode.IMAGE_TOO_LARGE);
        }

        String ct = avatar.getContentType() == null ? "" : avatar.getContentType();
        if (!(ct.equals("image/jpeg") || ct.equals("image/png"))) {
            throw new AppException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
    }


    public ProfilePublicDto getPublicProfile(String username){
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        if (account != null) {
            return ProfilePublicDto.builder()
                    .id(account.getId())
                    .email(account.getEmail())
                    .phone(account.getPhone())
                    .fullName(account.getFullName())
                    .address(account.getAddress())
                    .dateOfBirth(account.getDateOfBirth())
                    .gender(account.getGender())
                    .taxCode(account.getTaxCode())
                    .nationalId(account.getNationalId())
                    .createdAt(LocalDate.from(account.getCreatedAt()))
                    .updatedAt(LocalDate.from(account.getUpdatedAt()))
                    .avatarUrl(account.getAvatarUrl())
                    .status(account.getStatus())
                    .role(account.getRole())
                    .build();
        }
        return null;
    }
}