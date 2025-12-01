package com.evdealer.evdealermanagement.utils;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RenewalActivationScheduler {

    private final ProductRepository productRepository;

    // Chạy lúc 00:00 mỗi ngày
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void activateRenewalProducts() {
        LocalDateTime now = VietNamDatetime.nowVietNam();

        List<Product> productsToActivate = productRepository.findByStartRenewalAtBeforeAndStartRenewalAtNotNull(now);

        for (Product product : productsToActivate) {
            // Cập nhật updatedAt theo đúng mốc startRenewalAt để "đẩy top"
            product.setUpdatedAt(product.getStartRenewalAt());
            product.setStartRenewalAt(null); // Clear flag
            productRepository.save(product);

            log.info("Activated renewal for product: {}, new updatedAt: {}",
                    product.getId(), product.getUpdatedAt());
        }
    }
}
