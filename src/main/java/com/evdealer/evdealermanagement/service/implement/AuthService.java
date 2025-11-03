package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.account.custom.CustomAccountDetails;
import com.evdealer.evdealermanagement.dto.account.login.AccountLoginResponse;
import com.evdealer.evdealermanagement.dto.account.register.AccountRegisterRequest;
import com.evdealer.evdealermanagement.dto.account.register.AccountRegisterResponse;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.repository.AccountRepository;
import com.evdealer.evdealermanagement.utils.Utils;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecaptchaService recaptchaService;

    public AuthService(JwtService jwtService,
                       AccountRepository accountRepository,
                       PasswordEncoder passwordEncoder, RecaptchaService recaptchaService) {
        this.jwtService = jwtService;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.recaptchaService = recaptchaService;
    }

    // ======================= LOGIN =======================
    public AccountLoginResponse login(String phone, String password, String gRecaptchaResponse, HttpServletRequest request) {

        if(!recaptchaService.verifyRecaptcha(gRecaptchaResponse, request)) {
            throw new AppException(ErrorCode.INVALID_CAPTCHA);
        }

        String username = accountRepository.findUsernameByPhone(phone);

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!Account.Status.ACTIVE.equals(account.getStatus())) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        // Step 4: Generate token
        String token = jwtService.generateToken(new CustomAccountDetails(account));

        return AccountLoginResponse.builder()
                .email(account.getEmail())
                .fullName(account.getFullName())
                .phone(account.getPhone())
                .dateOfBirth(account.getDateOfBirth())
                .gender(account.getGender())
                .role(account.getRole())
                .status(account.getStatus())
                .nationalId(account.getNationalId())
                .taxCode(account.getTaxCode())
                .createdAt(account.getCreatedAt())
                .updateAt(account.getUpdatedAt())
                .address(account.getAddress())
                .avatarUrl(account.getAvatarUrl())
                .token(token)
                .build();
    }

    // ======================= REGISTER =======================
    public AccountRegisterResponse register(AccountRegisterRequest request) {
        String phone = request.getPhone().trim();
        String fullName = request.getFullName().trim();
        //String email = request.getEmail().trim();

        // Check duplicate phone
        if (accountRepository.findByPhone(phone).isPresent()) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        // Hash password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Generate username
        String username = Utils.generateUsername(phone, fullName);

        // Create account
        Account account = Account.builder()
                .username(username)
                .phone(phone)
                .fullName(fullName)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .role(Account.Role.MEMBER)
                .status(Account.Status.ACTIVE)
                .passwordHash(hashedPassword)
                .address(request.getAddress())
                .email(null)
                .createdAt(VietNamDatetime.nowVietNam())
                .updatedAt(VietNamDatetime.nowVietNam())
                .build();

        Account saved = accountRepository.save(account);

        return AccountRegisterResponse.builder()
                .username(saved.getUsername())
                .phone(saved.getPhone())
                .fullName(saved.getFullName())
                .dateOfBirth(saved.getDateOfBirth())
                .gender(saved.getGender())
                .role(saved.getRole())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .updateAt(saved.getUpdatedAt())
                .address(saved.getAddress())
                .email(null)
                .build();
    }

    // ======================= DELETE USER =======================
    public void deleteUserById(String id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        accountRepository.delete(account);
    }

    public void deleteUserByUsername(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        accountRepository.delete(account);
    }

    public AccountRegisterResponse registerStaffAccount(AccountRegisterRequest request) {
        String phone = request.getPhone().trim();
        String fullName = request.getFullName().trim();

        if (accountRepository.findByPhone(phone).isPresent()) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        String username = Utils.generateUsername(phone, fullName);

        Account account = Account.builder()
                .username(username)
                .phone(phone)
                .fullName(fullName)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .role(Account.Role.STAFF)
                .status(Account.Status.ACTIVE)
                .passwordHash(hashedPassword)
                .address(request.getAddress())
                .email(null)
                .createdAt(VietNamDatetime.nowVietNam())
                .updatedAt(VietNamDatetime.nowVietNam())
                .build();

        Account saved = accountRepository.save(account);

        return AccountRegisterResponse.builder()
                .username(saved.getUsername())
                .phone(saved.getPhone())
                .fullName(saved.getFullName())
                .dateOfBirth(saved.getDateOfBirth())
                .gender(saved.getGender())
                .role(saved.getRole())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .updateAt(saved.getUpdatedAt())
                .address(saved.getAddress())
                .email(null)
                .build();
    }
}