package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.account.custom.CustomAccountDetails;
import com.evdealer.evdealermanagement.dto.account.login.AccountLoginResponse;
import com.evdealer.evdealermanagement.dto.account.register.AccountRegisterRequest;
import com.evdealer.evdealermanagement.dto.account.register.AccountRegisterResponse;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.mapper.account.AccountMapper;
import com.evdealer.evdealermanagement.repository.AccountRepository;
import com.evdealer.evdealermanagement.utils.Utils;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import jakarta.servlet.http.HttpServletRequest;
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
    public AccountLoginResponse login(String phone, String password, String gRecaptchaResponse,
            HttpServletRequest request) {

        if (!recaptchaService.verifyRecaptcha(gRecaptchaResponse, request)) {
            throw new AppException(ErrorCode.INVALID_CAPTCHA);
        }

        if(!Utils.validatePhoneNumber(phone)) {
            throw new AppException(ErrorCode.INVALID_PHONE);
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

        return AccountMapper.toLoginResponse(account, token);
    }

    // ======================= REGISTER =======================
    public AccountRegisterResponse register(AccountRegisterRequest request) {
        String phone = request.getPhone().trim();
        String fullName = request.getFullName().trim();
        // String email = request.getEmail().trim();

        if(!Utils.validatePhoneNumber(phone)) {
            throw new AppException(ErrorCode.INVALID_PHONE);
        }

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

        return AccountMapper.toRegisterResponse(saved);
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

        if(!Utils.validatePhoneNumber(phone)) {
            throw new AppException(ErrorCode.INVALID_PHONE);
        }

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

        return AccountMapper.toRegisterResponse(saved);
    }
}