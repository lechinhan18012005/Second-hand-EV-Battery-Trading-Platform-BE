package com.evdealer.evdealermanagement.repository;

import com.evdealer.evdealermanagement.entity.account.AuthProvider;
import com.evdealer.evdealermanagement.entity.account.AuthProvider.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthProviderRepository extends JpaRepository<AuthProvider, String> {
    Optional<AuthProvider> findByProviderAndProviderUserId(Provider provider, String providerUserId);

    boolean existsByAccountId(String accountId);
}
