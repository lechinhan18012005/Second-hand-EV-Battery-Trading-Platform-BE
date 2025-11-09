package com.evdealer.evdealermanagement.repository;

import com.evdealer.evdealermanagement.entity.battery.BatteryBrands;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BatteryBrandsRepository extends JpaRepository<BatteryBrands, String> {
    boolean existsByNameIgnoreCase(String name);

    List<BatteryBrands> findAllByOrderByNameAsc();

    Optional<BatteryBrands> findByNameIgnoreCase(String name);
}
