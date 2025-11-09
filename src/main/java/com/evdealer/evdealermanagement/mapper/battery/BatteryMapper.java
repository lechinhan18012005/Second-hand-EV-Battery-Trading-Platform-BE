package com.evdealer.evdealermanagement.mapper.battery;

import com.evdealer.evdealermanagement.dto.battery.brand.BatteryBrandsResponse;
import com.evdealer.evdealermanagement.dto.battery.type.BatteryTypeResponse;
import com.evdealer.evdealermanagement.entity.battery.BatteryBrands;
import com.evdealer.evdealermanagement.entity.battery.BatteryTypes;

public class BatteryMapper {
    public static BatteryBrandsResponse mapToBatteryBrandsResponse(BatteryBrands e) {
        return BatteryBrandsResponse.builder()
                .brandId(e.getId())
                .brandName(e.getName())
                .logoUrl(e.getLogoUrl())
                .build();
    }

    public static BatteryBrandsResponse toBrandRes(BatteryBrands b) {
        return BatteryBrandsResponse.builder()
                .brandId(b.getId())
                .brandName(b.getName())
                .logoUrl(b.getLogoUrl())
                .build();
    }

    public static BatteryTypeResponse toTypeRes(BatteryTypes t) {
        return BatteryTypeResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .build();
    }
}
