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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleLoginService {

    private final AccountRepository accountRepo;
    private final AuthProviderRepository authProviderRepo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AccountLoginResponse processGoogleLogin(OAuth2User u) throws Exception {

        log.info("Google Attributes: {}", u.getAttributes());
        String sub = u.getAttribute("sub"); //ID duy nhất của Google (không đổi, dùng để nhận diện user này).
        String email = u.getAttribute("email");
        String finalEmail = email;
        String name = Optional.ofNullable(u.getAttribute("name")).orElseGet(() -> finalEmail != null ? finalEmail.split("@")[0] : "Google User").toString();
        String avatar = u.getAttribute("picture");

        //dự phòng nếu email trên ko lấy đc
        if(email == null && sub != null) {
            email = sub + "@google.com";
        }

        if(!Utils.isValidEmail(email)) {
            throw new AppException(ErrorCode.INVALID_EMAIL);
        }

        // Tìm kiếm trong bảng AuthProvider xem đã  có bản ghi liên kết nào với Provider=GOOGLE và ProviderUserId=sub này chưa.
        var provider = authProviderRepo.findByProviderAndProviderUserId(AuthProvider.Provider.GOOGLE, sub);

        Account acc;
        if(provider.isPresent()) {
            acc = provider.get().getAccount();
        } else { //(chưa liên kết nhưng có tài khoản)tìm xem đã có tài khoản nào tồn tại với địa chỉ email này chưa
            var accOpt = (email != null) ? accountRepo.findByEmail(email) : Optional.<Account>empty();
            if(accOpt.isPresent()) {
                acc = accOpt.get();
            } else { // hoàn toàn mới
                String dummyPassword = passwordEncoder.encode("social_login:" + UUID.randomUUID().toString());
                String usernameBase = (email != null) ? email.split("@")[0] : "google_user";
                String username = usernameBase;
                //logic check trùng username
                int i = 1;
                while (accountRepo.existsByUsername(username)) {
                    username = usernameBase + "_" + i;
                    i ++;
                }

                acc = Account.builder()
                        .email(email)
                        .passwordHash(dummyPassword)
                        .username(username)
                        .fullName(name)
                        .avatarUrl(avatar)
                        .role(Account.Role.MEMBER)
                        .status(Account.Status.ACTIVE)
                        .build();

                accountRepo.save(acc);
            }
            //link provider
            var link = AuthProvider.builder()
                    .account(acc)
                    .provider(AuthProvider.Provider.GOOGLE)
                    .providerUserId(sub)
                    .build();

            authProviderRepo.save(link);
        }

        CustomAccountDetails accountDetails = new CustomAccountDetails(acc);
        String token = jwtService.generateToken(accountDetails);

        return AccountLoginResponse.builder()
                .token(token)
                .email(email)
                .fullName(acc.getFullName())
                .avatarUrl(acc.getAvatarUrl())
                .role(acc.getRole())
                .build();

    }

}
