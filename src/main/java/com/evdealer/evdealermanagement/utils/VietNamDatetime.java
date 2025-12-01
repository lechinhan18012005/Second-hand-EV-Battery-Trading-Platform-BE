package com.evdealer.evdealermanagement.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class VietNamDatetime {

    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static LocalDateTime nowVietNam() {
        return ZonedDateTime.now(VIETNAM_ZONE).toLocalDateTime();
    }
}
