package com.evdealer.evdealermanagement.service.implement;

import com.cloudinary.Cloudinary;
import com.evdealer.evdealermanagement.dto.account.profile.AccountProfileResponse;
import com.evdealer.evdealermanagement.dto.account.profile.AccountUpdateRequest;
import com.evdealer.evdealermanagement.dto.account.profile.ProfilePublicDto;
import com.evdealer.evdealermanagement.dto.product.detail.ProductDetail;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.mapper.account.AccountMapper;
import com.evdealer.evdealermanagement.repository.AccountRepository;
import com.evdealer.evdealermanagement.repository.AuthProviderRepository;
import com.evdealer.evdealermanagement.repository.ProductRepository;
import com.evdealer.evdealermanagement.service.contract.IAccountService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.evdealer.evdealermanagement.utils.Utils;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import jakarta.transaction.Transactional;
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
    private final ProductRepository productRepository;
    private final AuthProviderRepository authProviderRepository;

    public AccountProfileResponse getProfile(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
        return AccountMapper.mapToAccountProfileResponse(account);
    }

    @Override
    public AccountProfileResponse updateProfile(String username, AccountUpdateRequest accountRequest,
            MultipartFile avatarUrl) {

        Account existingAccount = accountRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found by username='{}'", username);
                    return new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found");
                });
        log.debug("Loaded account: id={}, email='{}', phone='{}'",
                existingAccount.getId(), existingAccount.getEmail(), existingAccount.getPhone());

        boolean hasProvider = authProviderRepository.existsByAccountId(existingAccount.getId());

<<<<<<< HEAD
        if (!hasProvider && StringUtils.hasText(accountRequest.getPhone())) {
            if (!accountRequest.getPhone().equals(existingAccount.getPhone())) {
=======
        if(!hasProvider && StringUtils.hasText(accountRequest.getPhone())) {
            if(!accountRequest.getPhone().equals(existingAccount.getPhone())) {
>>>>>>> d75908ccae366a335464a57db244f4728cdb4ccb
                throw new AppException(ErrorCode.LOCAL_CANNOT_CHANGE);
            }
        }

        if(hasProvider && StringUtils.hasText(accountRequest.getEmail())) {
            if(!accountRequest.getEmail().equals(existingAccount.getEmail())) {
                throw new AppException(ErrorCode.PROVIDER_CANNOT_CHANGE);
            }
        }

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

        if (StringUtils.hasText(accountRequest.getPhone())) {
            if (!Utils.validatePhoneNumber(accountRequest.getPhone())) {
                throw new AppException(ErrorCode.INVALID_PHONE);
            }
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

        if (StringUtils.hasText(accountRequest.getEmail())) {
            if (!Utils.isValidEmail(accountRequest.getEmail())) {
                throw new AppException(ErrorCode.INVALID_EMAIL);
            }
        }

        // Map fields từ request sang entity
        log.debug("Applying AccountMapper.updateAccountFromRequest");
        AccountMapper.updateAccountFromRequest(accountRequest, existingAccount);

        // Chỉ update avatar nếu có file
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
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

    @Transactional
    public ProfilePublicDto getPublicProfile(String sellerId) {
        Account account = accountRepository.findById(sellerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

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

    @Transactional
    public List<ProductDetail> getActiveProductsOfSeller(String sellerId) {
        log.info("Getting active products for sellerId: {}", sellerId);

        try {
            // Validate seller exists
            if (!accountRepository.existsById(sellerId)) {
                throw new AppException(ErrorCode.USER_NOT_FOUND, "User not found");
            }

            List<Product> activeProducts = productRepository.findBySellerAndStatus(sellerId, Product.Status.ACTIVE);

            if (activeProducts == null || activeProducts.isEmpty()) {
                log.info("No active products found for sellerId: {}", sellerId);
                return Collections.emptyList();
            }

            List<ProductDetail> result = activeProducts.stream()
                    .map(ProductDetail::fromEntity)
                    .toList();

            log.info("Found {} active products for sellerId: {}", result.size(), sellerId);
            return result;

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting active products for sellerId {}: {}", sellerId, e.getMessage(), e);
            throw new AppException(ErrorCode.BAD_REQUEST, "Failed to get active products");
        }
    }

    @Transactional
    public List<ProductDetail> getSoldProductsOfSeller(String sellerId) {
        log.info("Getting sold products for sellerId: {}", sellerId);

        try {
            // Validate seller exists
            if (!accountRepository.existsById(sellerId)) {
                throw new AppException(ErrorCode.USER_NOT_FOUND, "User not found");
            }

            List<Product> soldProducts = productRepository.findBySellerAndStatus(sellerId, Product.Status.SOLD);

            if (soldProducts == null || soldProducts.isEmpty()) {
                log.info("No sold products found for sellerId: {}", sellerId);
                return Collections.emptyList();
            }

            List<ProductDetail> result = soldProducts.stream()
                    .map(ProductDetail::fromEntity)
                    .toList();

            log.info("Found {} sold products for sellerId: {}", result.size(), sellerId);
            return result;

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting sold products for sellerId {}: {}", sellerId, e.getMessage(), e);
            throw new AppException(ErrorCode.BAD_REQUEST, "Failed to get sold products");
        }
    }
}