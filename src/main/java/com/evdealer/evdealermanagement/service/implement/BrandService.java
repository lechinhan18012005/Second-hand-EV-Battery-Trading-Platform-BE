package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.brand.BrandItemResponse;
import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.entity.battery.BatteryBrands;
import com.evdealer.evdealermanagement.entity.vehicle.VehicleBrands;
import com.evdealer.evdealermanagement.exceptions.ResourceNotFoundException;
import com.evdealer.evdealermanagement.repository.BatteryBrandsRepository;
import com.evdealer.evdealermanagement.repository.BatteryDetailsRepository;
import com.evdealer.evdealermanagement.repository.VehicleBrandsRepository;
import com.evdealer.evdealermanagement.repository.VehicleDetailsRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {

    private final VehicleBrandsRepository vehicleRepo;
    private final BatteryBrandsRepository batteryRepo;
    private final BatteryDetailsRepository batteryDetailsRepository;
    private final VehicleDetailsRepository vehicleDetailsRepository;

    // --- Lấy tất cả brand ---
    public List<BrandItemResponse> listAllBrands() {
        List<BrandItemResponse> result = new ArrayList<>();

        result.addAll(
                vehicleRepo.findAllByOrderByNameAsc()
                        .stream()
                        .map(b -> BrandItemResponse.builder()
                                .id(b.getId())
                                .name(b.getName())
                                .logoUrl(b.getLogoUrl())
                                .type("VEHICLE")
                                .build())
                        .toList());

        result.addAll(
                batteryRepo.findAllByOrderByNameAsc()
                        .stream()
                        .map(b -> BrandItemResponse.builder()
                                .id(b.getId())
                                .name(b.getName())
                                .logoUrl(b.getLogoUrl())
                                .type("BATTERY")
                                .build())
                        .toList());

        result.sort(Comparator.comparing(BrandItemResponse::getName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    // --- Thêm brand ---
    @Transactional
    public BrandItemResponse createBrand(String name, String logoUrl, String type) {
        if ("VEHICLE".equalsIgnoreCase(type)) {
            VehicleBrands brand = new VehicleBrands();
            brand.setName(name);
            brand.setLogoUrl(logoUrl);
            vehicleRepo.save(brand);
            return BrandItemResponse.builder()
                    .id(brand.getId())
                    .name(brand.getName())
                    .logoUrl(brand.getLogoUrl())
                    .type("VEHICLE")
                    .build();
        } else if ("BATTERY".equalsIgnoreCase(type)) {
            BatteryBrands brand = new BatteryBrands();
            brand.setName(name);
            brand.setLogoUrl(logoUrl);
            batteryRepo.save(brand);
            return BrandItemResponse.builder()
                    .id(brand.getId())
                    .name(brand.getName())
                    .logoUrl(brand.getLogoUrl())
                    .type("BATTERY")
                    .build();
        } else {
            throw new IllegalArgumentException("Invalid brand type: " + type);
        }
    }

    // --- Cập nhật brand ---
    @Transactional
    public BrandItemResponse updateBrand(String id, String name, String logoUrl, String type) {
        System.out.println(">>> Updating brand id=" + id + ", type=" + type);

        if (type == null) {
            System.out.println(">>> Auto detect type");
            if (vehicleRepo.existsById(id)) {
                type = "VEHICLE";
            } else if (batteryRepo.existsById(id)) {
                type = "BATTERY";
            } else {
                throw new ResourceNotFoundException("Không tìm thấy brand với ID: " + id);
            }
        }

        if ("VEHICLE".equalsIgnoreCase(type)) {
            System.out.println(">>> Type: VEHICLE");
            var brand = vehicleRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle brand not found"));
            if (name != null)
                brand.setName(name);
            if (logoUrl != null)
                brand.setLogoUrl(logoUrl);
            vehicleRepo.save(brand);
            return BrandItemResponse.builder()
                    .id(brand.getId())
                    .name(brand.getName())
                    .logoUrl(brand.getLogoUrl())
                    .type("VEHICLE")
                    .build();
        } else if ("BATTERY".equalsIgnoreCase(type)) {
            System.out.println(">>> Type: BATTERY");
            var brand = batteryRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Battery brand not found"));
            if (name != null)
                brand.setName(name);
            if (logoUrl != null)
                brand.setLogoUrl(logoUrl);
            batteryRepo.save(brand);
            return BrandItemResponse.builder()
                    .id(brand.getId())
                    .name(brand.getName())
                    .logoUrl(brand.getLogoUrl())
                    .type("BATTERY")
                    .build();
        } else {
            throw new IllegalArgumentException("Invalid brand type: " + type);
        }
    }

    // --- Xóa brand ---
    @Transactional
    public boolean deleteBrand(String brandId, String type) {

        if ("VEHICLE".equalsIgnoreCase(type)) {
            boolean hasVehicle = vehicleDetailsRepository.existsByBrand_Id(brandId);
            if (hasVehicle) {
                throw new IllegalStateException("Không thể xóa brand vì đang được sử dụng trong VehicleDetails");
            }
            if (vehicleRepo.existsById(brandId)) {
                vehicleRepo.deleteById(brandId);
                return true;
            }

        } else if ("BATTERY".equalsIgnoreCase(type)) {
            boolean hasBattery = batteryDetailsRepository.existsByBrand_Id(brandId);
            if (hasBattery) {
                throw new IllegalStateException("Không thể xóa brand vì đang được sử dụng trong BatteryDetails");
            }
            if (batteryRepo.existsById(brandId)) {
                batteryRepo.deleteById(brandId);
                return true;
            }
        }

        return false;
    }

    public PageResponse<BrandItemResponse> listAllBrands(Pageable pageable) {
        List<BrandItemResponse> all = new ArrayList<>();

        all.addAll(vehicleRepo.findAll().stream()
                .map(v -> BrandItemResponse.builder()
                        .id(v.getId()).name(v.getName())
                        .logoUrl(v.getLogoUrl()).type("VEHICLE").build())
                .toList());

        all.addAll(batteryRepo.findAll().stream()
                .map(b -> BrandItemResponse.builder()
                        .id(b.getId()).name(b.getName())
                        .logoUrl(b.getLogoUrl()).type("BATTERY").build())
                .toList());

        all.sort(Comparator.comparing(BrandItemResponse::getName, String.CASE_INSENSITIVE_ORDER));

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<BrandItemResponse> content = (start <= end) ? all.subList(start, end) : List.of();

        Page<BrandItemResponse> page = new PageImpl<>(content, pageable, all.size());
        return PageResponse.fromPage(page, x -> x);
    }
}
