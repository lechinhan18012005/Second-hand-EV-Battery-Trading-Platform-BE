package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.account.custom.CustomAccountDetails;
import com.evdealer.evdealermanagement.dto.account.login.AccountLoginResponse;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.entity.account.AuthProvider;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.repository.AccountRepository;
import com.evdealer.evdealermanagement.repository.AuthProviderRepository;
import com.evdealer.evdealermanagement.utils.Utils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class FacebookLoginService {
    private final AccountRepository accountRepository;
    private final AuthProviderRepository providerRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public FacebookLoginService(AccountRepository accountRepository,
                                AuthProviderRepository providerRepository,
                                JwtService jwtService,
                                PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.providerRepository = providerRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AccountLoginResponse processFacebookLogin(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("id"); // Facebook UserId
        String avatar = extractAvatar(oAuth2User);

        // Nếu email null -> fallback bằng id
        if (email == null) {
            email = providerId + "@facebook.com";
        }

        if(!Utils.isValidEmail(email)) {
            throw new AppException(ErrorCode.INVALID_EMAIL);
        }

        // Nếu name null thì để trống
        if (name == null) {
            name = "Facebook User";
        }

        // 1. Check provider trước
        Optional<AuthProvider> providerOpt = providerRepository.findByProviderAndProviderUserId(AuthProvider.Provider.FACEBOOK, providerId);

        Account account;
        if (providerOpt.isPresent()) {
            account = providerOpt.get().getAccount();
        } else {
            // 2. Nếu chưa có provider → check bằng email
            Optional<Account> accOpt = accountRepository.findByEmail(email);

            if (accOpt.isPresent()) {
                account = accOpt.get();
            } else {
                // 3. Nếu chưa có email trong DB → tạo account mới
                // Fix: Set dummy passwordHash
                String dummyPassword = passwordEncoder.encode("social_login");
                String baseUsername = email.split("@")[0];
                String username = baseUsername + "_" + UUID.randomUUID().toString().substring(0, 6);

                Account newAcc = Account.builder()
                        .username(username)
                        .passwordHash(dummyPassword)
                        .email(email)
                        .fullName(name)
                        .avatarUrl(avatar)
                        .role(Account.Role.MEMBER)
                        .status(Account.Status.ACTIVE)
                        .build();
                account = accountRepository.save(newAcc);
            }

            // 4. Gắn provider cho account
            AuthProvider provider = AuthProvider.builder().account(account).provider(AuthProvider.Provider.FACEBOOK).providerUserId(providerId).build();
            providerRepository.save(provider);
        }

        CustomAccountDetails userDetails = new CustomAccountDetails(account);
        String token = jwtService.generateToken(userDetails);

        return AccountLoginResponse.builder().email(account.getEmail()).fullName(account.getFullName()).avatarUrl(account.getAvatarUrl()).role(account.getRole()).token(token).build();
    }

    private String extractAvatar(OAuth2User oAuth2User) {
        Object pictureObj = oAuth2User.getAttribute("picture");
        if (pictureObj instanceof String s) return s;
        if (pictureObj instanceof Map<?, ?> picMap) {
            Object data = picMap.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                return (String) dataMap.get("url");
            }
        }
        return null;
    }
}