package com.evdealer.evdealermanagement.mapper.account;

import com.evdealer.evdealermanagement.dto.account.profile.AccountProfileResponse;
import com.evdealer.evdealermanagement.dto.account.profile.AccountUpdateRequest;
import com.evdealer.evdealermanagement.entity.account.Account;

import org.springframework.util.StringUtils;

public final class AccountMapper {

    private AccountMapper() {
    }

    public static void updateAccountFromRequest(AccountUpdateRequest req, Account account) {
        if (req == null || account == null)
            return;

        if (StringUtils.hasText(req.getFullName()))
            account.setFullName(trimToNull(req.getFullName()));
        if (StringUtils.hasText(req.getPhone()))
            account.setPhone(trimToNull(req.getPhone()));
        if (StringUtils.hasText(req.getAddress()))
            account.setAddress(trimToNull(req.getAddress()));
        if (StringUtils.hasText(req.getEmail()))
            account.setEmail(trimToNull(req.getEmail()));
        if (StringUtils.hasText(req.getNationalId()))
            account.setNationalId(trimToNull(req.getNationalId()));
        if (StringUtils.hasText(req.getTaxCode()))
            account.setTaxCode(trimToNull(req.getTaxCode()));
        if (req.getGender() != null) {
            try {
                account.setGender(Account.Gender.valueOf(req.getGender().name()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (req.getDateOfBirth() != null) {
            account.setDateOfBirth(req.getDateOfBirth());
        }
    }

    private static String trimToNull(String s) {
        if (s == null)
            return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static AccountProfileResponse mapToAccountProfileResponse(Account account) {
        if (account == null)
            return null;

        return AccountProfileResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .phone(account.getPhone())
                .role(account.getRole())
                .status(account.getStatus())
                .dateOfBirth(account.getDateOfBirth())
                .address(account.getAddress())
                .nationalId(account.getNationalId())
                .taxCode(account.getTaxCode())
                .avatarUrl(account.getAvatarUrl())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .gender(account.getGender())
                .build();
    }
}