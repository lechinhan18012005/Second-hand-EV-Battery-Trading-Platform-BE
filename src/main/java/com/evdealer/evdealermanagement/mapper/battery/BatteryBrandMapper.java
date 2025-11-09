package com.evdealer.evdealermanagement.mapper.battery;

import com.evdealer.evdealermanagement.dto.battery.brand.BatteryBrandsResponse;
import com.evdealer.evdealermanagement.entity.battery.BatteryBrands;

public class BatteryBrandMapper {
    private BatteryBrandMapper() {
    }

    public static BatteryBrandsResponse mapToBatteryBrandsResponse(BatteryBrands b) {
        return BatteryBrandsResponse.builder()
                .brandId(b.getId())
                .brandName(b.getName())
                .logoUrl(b.getLogoUrl())
                .build();
    }
}
