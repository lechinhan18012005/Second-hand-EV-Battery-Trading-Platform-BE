package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.configurations.VnpayConfig;
import com.evdealer.evdealermanagement.configurations.vnpay.VnpayProperties;
import com.evdealer.evdealermanagement.dto.payment.VnpayRequest;
import com.evdealer.evdealermanagement.dto.payment.VnpayResponse;
import com.evdealer.evdealermanagement.dto.payment.VnpayVerifyResponse;
import com.evdealer.evdealermanagement.entity.post.PostPayment;
import com.evdealer.evdealermanagement.repository.PostPaymentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnpayService {

    private final VnpayProperties vnpayProperties;
    private final PostPaymentRepository paymentRepository;

    @PostConstruct
    public void checkConfig() {
        log.info("=== VNPay Configuration ===");
        log.info("TMN Code: {}", vnpayProperties.getTmnCode());
        log.info("Secret Key: {}", vnpayProperties.getSecretKey());
        log.info("Pay URL: {}", vnpayProperties.getPayUrl());
        log.info("Return URL: {}", vnpayProperties.getReturnUrl());
        log.info("===========================");
    }

    public VnpayResponse createPayment(VnpayRequest request) {
        try {
            if (request.getAmount() == null || request.getAmount().isEmpty()) {
                throw new IllegalArgumentException("Số tiền không hợp lệ");
            }

            String transactionId = request.getId() != null ? request.getId() : UUID.randomUUID().toString();

            ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
            ZonedDateTime now = ZonedDateTime.now(vietnamZone);
            ZonedDateTime expireTime = now.plusMinutes(15);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String createDate = now.format(formatter);
            String expireDate = expireTime.format(formatter);

            Map<String, String> vnpParams = new TreeMap<>();
            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", vnpayProperties.getTmnCode());
            vnpParams.put("vnp_Amount", String.valueOf(Integer.parseInt(request.getAmount()) * 100));
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", transactionId);
            vnpParams.put("vnp_OrderInfo", "Thanh toan don hang: " + transactionId);
            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_IpAddr", "127.0.0.1");
            vnpParams.put("vnp_CreateDate", createDate);
            vnpParams.put("vnp_ExpireDate", expireDate);
            vnpParams.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl());

            String hashData = buildHashData(vnpParams);
            String secureHash = VnpayConfig.hmacSHA512(vnpayProperties.getSecretKey(), hashData);

            String queryUrl = buildQueryUrl(vnpParams);
            String paymentUrl = vnpayProperties.getPayUrl() + "?" + queryUrl + "&vnp_SecureHash=" + secureHash;

            log.info("Payment URL created for transaction [{}]", transactionId);
            log.info("CreateDate: {}, ExpireDate: {}", createDate, expireDate);

            return VnpayResponse.builder()
                    .paymentUrl(paymentUrl)
                    .transactionId(transactionId)
                    .message("Tạo thanh toán thành công")
                    .build();

        } catch (Exception e) {
            log.error("Error creating payment", e);
            throw new RuntimeException("Không thể tạo thanh toán: " + e.getMessage());
        }
    }

    public boolean verifyPaymentSignature(Map<String, String> params) {
        try {
            String receivedHash = params.get("vnp_SecureHash");
            if (receivedHash == null || receivedHash.isEmpty()) {
                log.error("vnp_SecureHash is missing");
                return false;
            }

            Map<String, String> paramsToHash = new TreeMap<>(params);
            paramsToHash.remove("vnp_SecureHash");
            paramsToHash.remove("vnp_SecureHashType");

            String hashData = buildHashData(paramsToHash);
            String calculatedHash = VnpayConfig.hmacSHA512(vnpayProperties.getSecretKey(), hashData);

            boolean isValid = receivedHash.equalsIgnoreCase(calculatedHash);
            if (isValid) {
                log.info("Signature verification SUCCESS");
            } else {
                log.error("Signature verification FAILED");
                log.error("Received hash: {}", receivedHash);
                log.error("Calculated hash: {}", calculatedHash);
                log.error("Hash data: {}", hashData);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error verifying signature", e);
            return false;
        }
    }

    public VnpayVerifyResponse verifyReturn(String productId, String rawQuery, String clientIp) {
        try {
            Map<String, String> params = parseRawQuery(rawQuery);

            if(!verifyPaymentSignature(params)) {
                return VnpayVerifyResponse.builder()
                        .success(false).status("FAILED").message("Invalid signature")
                        .vnpTxnRef(params.get("vnp_TxnRef"))
                        .vnpTransactionNo(params.get("vnp_TransactionNo"))
                        .vnpResponseCode(params.get("vnp_ResponseCode"))
                        .build();
            }

            if(!Objects.equals(params.get("vnp_TmnCode"), vnpayProperties.getTmnCode())) {
                return VnpayVerifyResponse.builder()
                        .success(false).status("FAILED").message("Mismatched TMN code")
                        .vnpTxnRef(params.get("vnp_TxnRef"))
                        .vnpTransactionNo(params.get("vnp_TransactionNo"))
                        .vnpResponseCode(params.get("vnp_ResponseCode"))
                        .build();
            }

            BigDecimal vnpAmount = new BigDecimal(params.getOrDefault("vnp_Amount", "0")).divide(BigDecimal.valueOf(100));
            BigDecimal expectedAmount = resolveExpectedAmountFor(productId);
            if(expectedAmount != null && expectedAmount.compareTo(vnpAmount) != 0) {
                return VnpayVerifyResponse.builder()
                        .success(false).status("FAILED").message("Mismatched amount")
                        .vnpTxnRef(params.get("vnp_TxnRef"))
                        .vnpTransactionNo(params.get("vnp_TransactionNo"))
                        .vnpResponseCode(params.get("vnp_ResponseCode"))
                        .amount(vnpAmount)
                        .build();
            }

            String response = params.get("vnp_ResponseCode");
            boolean ok = "00".equals(response);

            return VnpayVerifyResponse.builder()
                    .success(ok)
                    .status(ok ? "PAID" : "FAILED")
                    .message(ok ? "Payment verified" : "Payment failed")
                    .vnpTxnRef(params.get("vnp_TxnRef"))
                    .vnpTransactionNo(params.get("vnp_TransactionNo"))
                    .vnpResponseCode(response)
                    .amount(vnpAmount)
                    .bankCode(params.get("vnp_BankCode"))
                    .payDate(parseVnpPayDate(params.get("vnp_PayDate")))
                    .build();

        } catch (Exception e) {
            log.error("VNPay verify error", e);
            throw new RuntimeException("VNPay verify error: " + e.getMessage(), e);
        }
    }

    private OffsetDateTime parseVnpPayDate(String vnpPayDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return OffsetDateTime.of(LocalDateTime.parse(vnpPayDate, formatter), ZoneOffset.ofHours(7));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, String> parseRawQuery(String rawQuery) {
        Map<String, String> result = new TreeMap<>();
        if(rawQuery == null) return result;
        String q = rawQuery.startsWith("?") ? rawQuery.substring(1) : rawQuery;
        for (String pair : q.trim().split("&")) {
            if(pair.isBlank()) continue;
            int i = pair.indexOf("=");
            String k = (i>=0) ? pair.substring(0, i) : pair;
            String v = (i>=0 && i + 1 < pair.length()) ? pair.substring(i + 1) : "";
            result.put(urlDecode(k), urlDecode(v));
        }
        return result;
    }

    private BigDecimal resolveExpectedAmountFor(String productId) {
        return paymentRepository.findFirstByProductIdAndPaymentStatusOrderByCreatedAtDesc(productId, PostPayment.PaymentStatus.PENDING).map(PostPayment::getAmount).orElse(null);
    }

    private String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private String buildHashData(Map<String, String> params) throws UnsupportedEncodingException {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            if (value != null && !value.isEmpty()) {
                sb.append(key).append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8.toString()));
                if (i < keys.size() - 1) sb.append("&");
            }
        }
        return sb.toString();
    }

    private String buildQueryUrl(Map<String, String> params) throws UnsupportedEncodingException {
        List<String> keys = new ArrayList<>(params.keySet());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            if (value != null && !value.isEmpty()) {
                sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8.toString()))
                        .append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8.toString()));
                if (i < keys.size() - 1) sb.append("&");
            }
        }
        return sb.toString();
    }
}