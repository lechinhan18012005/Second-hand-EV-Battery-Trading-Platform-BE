package com.evdealer.evdealermanagement.repository;

import com.evdealer.evdealermanagement.entity.battery.BatteryTypes;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BatteryTypesRepository extends JpaRepository<BatteryTypes, String> {
    Optional<BatteryTypes> findByNameIgnoreCase(String name);
}
