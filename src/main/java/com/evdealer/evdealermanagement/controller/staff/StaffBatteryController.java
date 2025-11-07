package com.evdealer.evdealermanagement.controller.staff;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.evdealer.evdealermanagement.dto.battery.brand.BatteryBrandsResponse;
import com.evdealer.evdealermanagement.service.implement.BatteryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/staff/battery")
@RequiredArgsConstructor
public class StaffBatteryController {

    private final BatteryService batteryService;

    @PutMapping(value = "/brands/{brandId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<BatteryBrandsResponse> updateBatteryBrand(
            @PathVariable String brandId,
            @RequestPart(value = "brandName", required = false) String brandName,
            @RequestPart(value = "logo", required = false) MultipartFile logoFile) {

        BatteryBrandsResponse resp = batteryService.updateBatteryBrand(brandId, brandName, logoFile);
        return ResponseEntity.ok(resp);
    }

}
