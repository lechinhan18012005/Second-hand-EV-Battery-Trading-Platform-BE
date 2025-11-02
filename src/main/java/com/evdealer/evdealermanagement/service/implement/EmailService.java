package com.evdealer.evdealermanagement.service.implement;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.objects.Email;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;


@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Value("${APP_BASE_URL:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${SENDGRID_API_KEY:}")
    private String sendGridApiKey;

    @Value("${SENDGRID_FROM:nhanhuynh7115@gmail.com}")
    private String sendGridFrom;

    @Value("${SENDGRID_FROM_NAME:Eco Green}")
    private String sendGridFromName;

    // ==============================================
    // INIT
    // ==============================================
    @PostConstruct
    public void init() {
        log.info("╔════════════════════════════════════════════");
        if (sendGridApiKey != null && !sendGridApiKey.isBlank()) {
            log.info("║ EMAIL SERVICE MODE: SENDGRID");
        } else {
            log.info("║ EMAIL SERVICE MODE: GMAIL SMTP (JavaMailSender)");
        }
        log.info("╠════════════════════════════════════════════");
        log.info("║ FROM: {} ({})", sendGridFromName, sendGridFrom);
        log.info("║ APP_BASE_URL: {}", appBaseUrl);
        log.info("╚════════════════════════════════════════════");
    }

    // ==============================================
    // 1️⃣ PURCHASE REQUEST → SELLER
    // ==============================================
    @Async
    public void sendPurchaseRequestNotification(String sellerEmail, String buyerName, String productTitle, BigDecimal offeredPrice, String requestId) {

        String viewRequestUrl = "http://localhost:5173/seller/purchase-requests/" + requestId;

        String respondEndpoint = appBaseUrl + "/member/purchase-request/respond/email?";
        String acceptUrl = respondEndpoint + "requestId=" + requestId + "&accept=true";
        String rejectUrl = respondEndpoint + "requestId=" + requestId + "&accept=false";

        Context context = new Context();
        context.setVariable("buyerName", buyerName);
        context.setVariable("productTitle", productTitle);
        context.setVariable("offeredPrice", formatCurrency(offeredPrice));
        context.setVariable("acceptUrl", acceptUrl);
        context.setVariable("rejectUrl", rejectUrl);
        context.setVariable("viewRequestUrl", viewRequestUrl); // <-- thêm dòng này

        String htmlContent = templateEngine.process("email/purchase-request-notification", context);
        sendEmail(sellerEmail, "Có người muốn mua sản phẩm của bạn!", htmlContent);
        log.info("📩 Purchase request notification sent to seller: {}", sellerEmail);
    }

    // ==============================================
    // 2️⃣ PURCHASE ACCEPTED → BUYER
    // ==============================================
    @Async
    public void sendPurchaseAcceptedNotification(String buyerEmail, String sellerName, String productTitle, String contractUrl) {

        Context context = new Context();
        context.setVariable("sellerName", sellerName);
        context.setVariable("productTitle", productTitle);
        context.setVariable("contractUrl", contractUrl);

        String htmlContent = templateEngine.process("email/purchase-accepted", context);
        sendEmail(buyerEmail, "Yêu cầu mua hàng được chấp nhận", htmlContent);
        log.info("📩 Purchase accepted notification sent to buyer: {}", buyerEmail);
    }

    // ==============================================
    // 3️⃣ PURCHASE REJECTED → BUYER
    // ==============================================
    @Async
    public void sendPurchaseRejectedNotification(String buyerEmail, String sellerName, String productTitle, String rejectReason) {

        Context context = new Context();
        context.setVariable("sellerName", sellerName);
        context.setVariable("productTitle", productTitle);
        context.setVariable("rejectReason", rejectReason);

        String htmlContent = templateEngine.process("email/purchase-rejected", context);
        sendEmail(buyerEmail, "Yêu cầu mua hàng bị từ chối", htmlContent);
        log.info("📩 Purchase rejected notification sent to: {}", buyerEmail);
    }

    // ==============================================
    // 4️⃣ CONTRACT → BUYER
    // ==============================================
    @Async
    public void sendContractToBuyer(String buyerEmail, String buyerName, String sellerName, String productTitle, String buyerSignUrl) {

        Context context = new Context();
        context.setVariable("buyerName", buyerName);
        context.setVariable("sellerName", sellerName);
        context.setVariable("productTitle", productTitle);
        context.setVariable("signUrl", buyerSignUrl);

        String htmlContent = templateEngine.process("email/contract-signing", context);
        sendEmail(buyerEmail, "Hợp đồng mua bán đã sẵn sàng - Vui lòng ký điện tử", htmlContent);
        log.info("📩 Contract signing email sent to buyer: {}", buyerEmail);
    }

    // ==============================================
    // 5️⃣ CONTRACT → SELLER
    // ==============================================
    @Async
    public void sendContractToSeller(String sellerEmail, String sellerName, String buyerName, String productTitle, String sellerSignUrl) {

        Context context = new Context();
        context.setVariable("sellerName", sellerName);
        context.setVariable("buyerName", buyerName);
        context.setVariable("productTitle", productTitle);
        context.setVariable("signUrl", sellerSignUrl);

        String htmlContent = templateEngine.process("email/contract-signing", context);
        sendEmail(sellerEmail, "Hợp đồng bán hàng đã sẵn sàng - Vui lòng ký điện tử", htmlContent);
        log.info("📩 Contract signing email sent to seller: {}", sellerEmail);
    }

    // ==============================================
    // 6️⃣ CONTRACT COMPLETED
    // ==============================================
    @Async
    public void sendContractCompletedNotification(String buyerEmail, String sellerEmail, String productTitle) {

        Context context = new Context();
        context.setVariable("productTitle", productTitle);
        String htmlContent = templateEngine.process("email/contract-completed", context);

        sendEmail(buyerEmail, "🎉 Hợp đồng đã hoàn tất!", htmlContent);
        sendEmail(sellerEmail, "🎉 Hợp đồng đã hoàn tất!", htmlContent);
        log.info("📩 Contract completed notifications sent.");
    }

    // ==============================================
    // 7️⃣ PRODUCT EXPIRE SOON
    // ==============================================
    @Async
    public void sendProductExpireSoon(String to, String productTitle, LocalDateTime expiresAt) {

            Context context = new Context();
            context.setVariable("productTitle", productTitle);

            ZonedDateTime vnTime = expiresAt.atZone(VN);
            context.setVariable("expiryDate", vnTime.toLocalDateTime().format(DATE_FMT));
            context.setVariable("expiryTime", vnTime.toLocalDateTime().format(TIME_FMT));

            long daysLeft = Duration.between(LocalDateTime.now(), expiresAt).toDays();
            context.setVariable("daysLeft", daysLeft);

            String htmlContent = templateEngine.process("email/product-expire-soon", context);
            sendEmail(to, "Nhắc nhở: Sản phẩm sắp hết hạn", htmlContent);
            log.info("📩 Product expire reminder sent to {}", to);
    }

    // ==============================================
    // 8️⃣ PASSWORD RESET OTP
    // ==============================================
    @Async
    public void sendPasswordResetOtp(String email, String phone, String otp) {
        log.info("Attempting to send password reset OTP to: {}", email);

        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("phone", phone);
        context.setVariable("otp", otp);

        String htmlContent = templateEngine.process("password/password-reset-otp", context);
        sendEmail(email, "Mã khôi phục mật khẩu Eco Green của bạn", htmlContent);

        log.info("📩 Password reset OTP sent successfully to: {}", email);
    }

    // ==============================================
    // CORE SEND EMAIL
    // ==============================================
    private void sendEmail(String to, String subject, String htmlContent) {
        if (sendGridApiKey != null && !sendGridApiKey.isBlank()) {
            sendViaSendGrid(to, subject, htmlContent);
        } else {
            sendViaJavaMail(to, subject, htmlContent);
        }
    }

    private void sendViaSendGrid(String to, String subject, String htmlContent) {
        try {
            com.sendgrid.helpers.mail.objects.Email from = new com.sendgrid.helpers.mail.objects.Email(sendGridFrom, sendGridFromName);
            com.sendgrid.helpers.mail.objects.Email toEmail = new com.sendgrid.helpers.mail.objects.Email(to);

            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(from, subject, toEmail, content);

            // ✅ Thêm dòng này để set Reply-To
            mail.setReplyTo(new Email("nhanhuynh7115@gmail.com"));

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);
            log.info("✅ SendGrid → status={}, body={}", response.getStatusCode(), response.getBody());
        } catch (IOException e) {
            log.error("❌ SendGrid IOException: {}", e.getMessage());
        }
    }

    private void sendViaJavaMail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(sendGridFrom, sendGridFromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("✅ JavaMailSender → sent to {}", to);
        } catch (Exception e) {
            log.error("❌ JavaMailSender failed: {}", e.getMessage());
        }
    }

    // ==============================================
    // HELPER: FORMAT VND
    // ==============================================
    private String formatCurrency(Object amount) {
        if (amount == null) return "0 VND";
        BigDecimal value;
        if (amount instanceof Number) {
            value = new BigDecimal(((Number) amount).doubleValue());
        } else if (amount instanceof String) {
            try {
                value = new BigDecimal((String) amount);
            } catch (NumberFormatException e) {
                return "0 VND";
            }
        } else {
            return "0 VND";
        }

        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(value) + " VND";
    }
}
