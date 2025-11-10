package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.account.custom.CustomAccountDetails;
import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.dto.product.detail.ProductDetail;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.entity.product.Product;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.mapper.product.ProductMapper;
import com.evdealer.evdealermanagement.repository.AccountRepository;
import com.evdealer.evdealermanagement.repository.PostPaymentRepository;
import com.evdealer.evdealermanagement.repository.ProductRepository;
import com.evdealer.evdealermanagement.utils.PriceSerializer;
import com.evdealer.evdealermanagement.utils.VietNamDatetime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    @Autowired
    public ProductRepository productRepository;

    private final PostPaymentRepository postPaymentRepository;

    @Autowired
    public AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<ProductDetail> getAllProducts(Pageable pageable) {
        log.debug("Fetching products with pageable: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

<<<<<<< Updated upstream
        Page<Product> page = productRepository.findAll(pageable);

        return PageResponse.fromPage(page, ProductMapper::toDetailDto);
=======
            List<ProductDetail> sortedList = new ArrayList<>(list);
            sortedList.sort(Comparator.comparing(ProductDetail::getUpdatedAt));
            return sortedList;
        } catch (Exception e) {
            log.error("Error fetching all products", e);
            return List.of();
        }
>>>>>>> Stashed changes
    }

    public PageResponse<Account> getMemberAccounts(Pageable pageable) {
        return getAccountsByRole(Account.Role.MEMBER, pageable);
    }

    public PageResponse<Account> getStaffAccounts(Pageable pageable) {
        return getAccountsByRole(Account.Role.STAFF, pageable);
    }

    public PageResponse<Account> getAccountsByRole(Account.Role role, Pageable pageable) {
        try {
            Page<Account> accountList = accountRepository.findByRole(role, pageable);

            List<Account> sortedList = new ArrayList<>(accountList.getContent());
            sortedList.sort(Comparator.comparing(Account::getCreatedAt));

            PageResponse<Account> pageResponse = PageResponse.of(sortedList, accountList);

            log.debug("Fetching all accounts with role: {}", role);
            return pageResponse;
        } catch (Exception e) {
            log.error("Error fetching accounts with role: {}", role, e);
            return PageResponse.of(List.of(), 0, 0, 0);
        }
    }

    public boolean deleteAccount(String id) {
        try {
            log.debug("Deleting account with id: {}", id);
            if (accountRepository.existsById(id)) {
                accountRepository.deleteById(id);
                return true;
            } else {
                log.warn("Account with id: {} not found", id);
                return false;
            }
        } catch (Exception e) {
            log.error("Error deleting account with id: {}", id, e);
            return false;
        }
    }

    public boolean changeStatusAccount(String id, Account.Status status) {
        Account account = accountRepository.findById(id).orElse(null);
        if (account != null) {
            log.warn("Account with id: {}", id);
            account.setStatus(status);
            account.setUpdatedAt(VietNamDatetime.nowVietNam());
            accountRepository.save(account);
            return true;
        } else {
            log.warn("Account with id: {} not found", id);
            return false;
        }
    }

    public String getTotalFee() {
        try {
            List<Product> productList = productRepository.findAll();

            BigDecimal totalFee = productList.stream()
                    .map(Product::getPostingFee)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.debug("Total import fee calculated: " + totalFee);
            return PriceSerializer.formatPrice(totalFee);

        } catch (Exception e) {
            log.error("Error calculating total import fee", e);
            return "0";
        }
    }

    public void banAccount(String accountId, String reason) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        account.setStatus(Account.Status.BANNED);
        account.setBanReason(reason);
        account.setUpdatedAt(VietNamDatetime.nowVietNam());
        accountRepository.save(account);
    }

    public void unBanAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        account.setStatus(Account.Status.ACTIVE);
        account.setBanReason(null);
        account.setUpdatedAt(VietNamDatetime.nowVietNam());
        accountRepository.save(account);
    }

    public void deleteAccountForAdmin(String accountId, String adminPassword, CustomAccountDetails adminDetails) {

        String adminId = adminDetails.getAccountId();
        String adminUsername = adminDetails.getUsername();

        log.info("Admin {} attempting to delete account {}", adminUsername, accountId);

        Account admin = accountRepository.findById(adminId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(adminPassword, admin.getPasswordHash())) {
            log.warn("Admin {} entered wrong password", adminUsername);
            throw new RuntimeException("Wrong password");
        }

        if (adminId.equals(accountId)) {
            throw new RuntimeException("Cannot delete your own account");
        }

        Account target = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        accountRepository.delete(target);
        log.info("Account {} deleted successfully by admin {}", target.getUsername(), adminUsername);
    }

}
