package com.evdealer.evdealermanagement.service.contract;

import com.evdealer.evdealermanagement.dto.account.profile.AccountProfileResponse;
import com.evdealer.evdealermanagement.dto.account.profile.AccountUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

public interface IAccountService {

    // Xem profile
    AccountProfileResponse getProfile(String username);

    // Cập nhật profile
    AccountProfileResponse updateProfile(String userId, AccountUpdateRequest request,  MultipartFile avatarUrl);

    // Xóa account
    void deleteAccount(String userId);
}
