package com.evdealer.evdealermanagement.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evdealer.evdealermanagement.entity.vehicle.Model;
import com.evdealer.evdealermanagement.entity.vehicle.VehicleCatalog;

public interface VehicleCatalogRepository extends JpaRepository<VehicleCatalog, String> {
    Optional<VehicleCatalog> findByVersionId(String versionId);

    Optional<VehicleCatalog> findByVersion_Id(String versionId);

    Optional<VehicleCatalog> findByVersionIdAndBrandIdAndModelAndYear(
            String versionId,
            String brandId,
            Model model,
            Short year);

    Optional<VehicleCatalog> findByModelAndYear(Model model, Short manufactureYear);
}
