package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.account.profile.AccountProfileResponse;
import com.evdealer.evdealermanagement.dto.account.profile.AccountUpdateRequest;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.mapper.account.AccountMapper;
import com.evdealer.evdealermanagement.repository.AccountRepository;
import com.evdealer.evdealermanagement.service.contract.IAccountService;

import java.time.LocalDateTime;

import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService implements IAccountService {

    private final AccountRepository accountRepository;

    public ProfileService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

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

        if (accountRequest.getPhone() != null &&
                accountRepository.existsByPhoneAndIdNot(accountRequest.getPhone(), existingAccount.getId())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "Phone already used");
        }

        if (accountRequest.getEmail() != null &&
                accountRepository.existsByEmailAndIdNot(accountRequest.getEmail(), existingAccount.getId())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "Email already used");
        }

        AccountMapper.updateAccountFromRequest(accountRequest, existingAccount);
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
}