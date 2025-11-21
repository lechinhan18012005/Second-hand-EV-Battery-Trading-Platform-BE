package com.evdealer.evdealermanagement.configurations;

import com.evdealer.evdealermanagement.dto.account.login.AccountLoginResponse;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.service.implement.FacebookLoginService;
import com.evdealer.evdealermanagement.service.implement.GoogleLoginService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final FacebookLoginService facebookLoginService;
    private final GoogleLoginService googleLoginService;


    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // FE mặc định
        String baseBridgeUrl = "http://localhost:5173/oauth2/popup-bridge";

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal(); //OAuth2User chứa email, name, picture

            // 1) xác định provider
            String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            AccountLoginResponse loginResponse;

            if ("google".equalsIgnoreCase(registrationId)) {
                loginResponse = googleLoginService.processGoogleLogin(oAuth2User);
            } else if ("facebook".equalsIgnoreCase(registrationId)) {
                loginResponse = facebookLoginService.processFacebookLogin(oAuth2User);
            } else {
                // tự ném vào catch để redirect lỗi
                throw new AppException(ErrorCode.UNSUPPORTED_OAUTH2_PROVIDER);
            }

            // 2) success → redirect kèm token & email
            //Encode giúp kí tự như = ? & biển đổi khác đi để FE ko đọc sai
            String token = URLEncoder.encode("Bearer " + loginResponse.getToken(), StandardCharsets.UTF_8);
            String email = URLEncoder.encode(loginResponse.getEmail(), StandardCharsets.UTF_8);

            String redirectUrl = baseBridgeUrl + "?token=" + token + "&email=" + email;
            response.sendRedirect(redirectUrl);

        } catch (Exception ex) {
            // 3) lỗi → vẫn redirect về FE nhưng kèm error
            String error = URLEncoder.encode("oauth_failed", StandardCharsets.UTF_8);
            // tránh để message quá nhạy cảm
            String message = URLEncoder.encode(ex.getMessage() != null ? ex.getMessage() : "OAuth2 login failed",
                    StandardCharsets.UTF_8);

            String redirectUrl = baseBridgeUrl + "?error=" + error + "&message=" + message;
            response.sendRedirect(redirectUrl);
        }
    }
}
