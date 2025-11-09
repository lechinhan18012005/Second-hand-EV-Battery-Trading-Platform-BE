package com.evdealer.evdealermanagement.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evdealer.evdealermanagement.dto.brand.BrandItemResponse;
import com.evdealer.evdealermanagement.dto.common.PageResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BrandService {
        private final VehicleBrandsRepository vehicleRepo;
        private final BatteryBrandsRepository batteryRepo;

        @Transactional(readOnly = true)
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

                // Sort theo Pageable (chỉ minh họa đơn giản cho sort theo name)
                all.sort(Comparator.comparing(BrandItemResponse::getName, String.CASE_INSENSITIVE_ORDER));

                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), all.size());
                List<BrandItemResponse> content = (start <= end) ? all.subList(start, end) : List.of();

                Page<BrandItemResponse> page = new PageImpl<>(content, pageable, all.size());
                return PageResponse.fromPage(page, x -> x);
        }
}