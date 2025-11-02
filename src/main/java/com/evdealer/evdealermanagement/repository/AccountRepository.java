package com.evdealer.evdealermanagement.repository;

import com.evdealer.evdealermanagement.entity.account.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByUsername(String username);

    Optional<Account> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, String id); // Changed Long to String

    boolean existsByPhoneAndIdNot(String phone, String id); // Changed Long to String

    Optional<Account> findByEmail(String email);

    boolean existsByEmailAndIdNot(String email, String id);

    @Query("SELECT a.username FROM Account a WHERE a.phone = :phone")
    String findUsernameByPhone(String phone);

    @EntityGraph(attributePaths = { "role" }) // nếu có quan hệ role
    Optional<Account> findWithDetailsByEmail(String email);

    Optional<Account> findByUsernameOrPhoneOrEmail(String username, String phone, String email);

    @Query("SELECT a.phone FROM  Account  a WHERE  a.id = :accountId")
    String getPhone(@Param("accountId") String accountId);

    Page<Account> findByRole(Account.Role role, Pageable pageable);

    long countByRole(Account.Role role);

}