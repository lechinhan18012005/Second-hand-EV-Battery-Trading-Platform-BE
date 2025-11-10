package com.evdealer.evdealermanagement.controller.product;

import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.dto.product.detail.ProductDetail;
import com.evdealer.evdealermanagement.service.implement.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/product/filter")
@RequiredArgsConstructor
public class ProductFilterController {

    private final ProductService productService;

    /**
     * Lấy danh sách sản phẩm mới nhất
     * GET /product/filter/new
     */
    @GetMapping("/new")
    public ResponseEntity<List<ProductDetail>> getNewProducts() {
        try {
            log.info("Request → Get new products");
            List<ProductDetail> newProducts = productService.getNewProducts();

            if (newProducts.isEmpty()) {
                log.info("No new products found");
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(newProducts);
        } catch (Exception e) {
            log.error("Error getting new products", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lọc sản phẩm với multiple filters (có thể kết hợp name, brand, type)
     * GET /product/filter?name=xxx&brand=xxx&type=xxx
     *
     * Examples:
     * - /product/filter (get all active products)
     * - /product/filter?name=LG (search by name)
     * - /product/filter?brand=LG (filter by brand)
     * - /product/filter?type=BATTERY (filter by type)
     * - /product/filter?brand=LG&type=BATTERY (combine filters)
     * - /product/filter?name=RESU&brand=LG&type=BATTERY (all filters)
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProductDetail>> filterProducts(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,


            @RequestParam(required = false) String name,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String type,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable

    ) {
        PageResponse<ProductDetail> response = productService.filterProducts(name, brand, type, city, district, minPrice, maxPrice, yearFrom, yearTo, pageable);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/brand")
    public ResponseEntity<PageResponse<ProductDetail>> findProductsByBrand(

            @RequestParam(required = false) String brand,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable

    ) {
        PageResponse<ProductDetail> response = productService.findProductsByBrand(brand, pageable);
        return ResponseEntity.ok(response);
    }
}