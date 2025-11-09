package com.evdealer.evdealermanagement.dto.battery.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatteryTypeResponse {
    private String id;
    private String name;
}
