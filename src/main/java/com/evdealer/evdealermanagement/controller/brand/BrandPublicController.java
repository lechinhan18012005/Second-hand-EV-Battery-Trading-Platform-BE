package com.evdealer.evdealermanagement.controller.brand;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evdealer.evdealermanagement.dto.brand.BrandItemResponse;
import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.repository.BrandService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/public/brands")
@RequiredArgsConstructor
public class BrandPublicController {

    private final BrandService brandService;

    @GetMapping
    public PageResponse<BrandItemResponse> listAll(
            @PageableDefault(size = 12, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return brandService.listAllBrands(pageable);
    }
}
