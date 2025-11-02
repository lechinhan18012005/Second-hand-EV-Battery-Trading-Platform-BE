package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.entity.notify.Notification;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.repository.AccountRepository;
import com.evdealer.evdealermanagement.repository.NotificationRepository;
import com.evdealer.evdealermanagement.repository.ProductRepository;
import com.evdealer.evdealermanagement.repository.PurchaseRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductExpireReminderJobService {

    private final EmailService emailService;
    private final NotificationService notificationService;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void remindBefore2Days() {
        ZonedDateTime nowVN = ZonedDateTime.now(VN);

        //Tính thời gian hết hạn
        LocalDate targetExpiryDate = nowVN.plusDays(2).toLocalDate();

        // Khoảng thời gian: từ 00:00:00 đến 23:59:59 của ngày (hôm nay + 2)
        LocalDateTime start = targetExpiryDate.atStartOfDay();
        LocalDateTime end = targetExpiryDate.atTime(23, 59, 59, 999999999);

        log.info("=== Product Expiry Reminder Job Started ===");
        log.info("Current time (VN): {}", nowVN.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        log.info("Looking for products expiring on: {} (2 days from now)", targetExpiryDate.format(DATE_FMT));

        //Tìm các products hết hạn trong khoảng thời gian và chưa gửi thông báo
        List<Product> productToRemind = productRepository.findExpiringBetweenAndNotReminded(start, end);

        log.info("=== Product Expiring Reminder Job Completed ===");
        log.info("Found {} products to remind", productToRemind.size());

        int successCount = 0;
        int failCount = 0;

        for (Product p : productToRemind) {
            try {
                //Tính số giờ còn lại
                long hoursLeft = Duration.between(nowVN.toLocalDateTime(), p.getExpiresAt()).toHours();
                long daysLeft = hoursLeft / 24;

                //Lấy email của seller
                String sellerEmail = String.valueOf(accountRepository.findById(p.getSeller().getId()).map(Account::getEmail).orElse(""));
                String expiryDateStr = p.getExpiresAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                //1. Gửi thông báo trong web
                notificationService.createAndPush(p.getSeller().getId(),
                        "Tin đăng sắp hết hạn",
                        String.format("Tin đăng %s sẽ hết hạn vào %s (còn %d ngày.) " + "Vui lòng gia hạn để tiếp tục hiển thị.", p.getTitle(), expiryDateStr, daysLeft),
                        Notification.NotificationType.PRODUCT_EXPIRE_SOON, p.getId());

                //2. Gửi email
                if(sellerEmail != null && !sellerEmail.isBlank()) {
                    emailService.sendProductExpireSoon(sellerEmail, p.getTitle(), p.getExpiresAt());
                }
                log.debug("Email sent to: {} for product: {}", sellerEmail, p.getTitle());

                //3. Đánh giá đã gửi thong báo
                 p.setRemindBefore2Sent(true);
                 productRepository.save(p);
                 successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("Failed to send reminder for product {} (ID: {}): {}",
                        p.getTitle(), p.getId(), e.getMessage(), e);
            }
        }
        log.info("=== Job Completed: Success={}, Failed={} ===", successCount, failCount);

    }




    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void hideExpiredProducts() {
        LocalDateTime now = LocalDateTime.now(VN);
        log.info("=== Hide Expired Products Job Started ===");
        log.info("Current time (VN): {}", now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

        //Tìm các products đã hết hạn và đang active
        List<Product> expiredProducts = productRepository.findExpiredAndActive(now);
        log.info("Found {} expired products to hide", expiredProducts.size());
        for (Product p : expiredProducts) {
            try {
                p.setStatus(Product.Status.EXPIRED);
                productRepository.save(p);
                log.info("Hidden expired product: {} (ID: {})", p.getTitle(), p.getId());
            } catch (Exception e) {
                log.error("Failed to hide product {}: {}", p.getId(), e.getMessage());
            }
        }
        log.info("=== Hide Job Completed ===");
    }



}
