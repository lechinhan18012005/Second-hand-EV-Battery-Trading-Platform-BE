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
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/product/search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductService productService;

    /**
     * Lấy tất cả sản phẩm có trạng thái ACTIVE
     */

    /**
     * Find Product By ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetail> getProductById(@PathVariable String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                log.warn("Invalid product ID");
                return ResponseEntity.badRequest().build();
            }

            log.info("Request → Get product by ID: {}", id);
            Optional<ProductDetail> product = productService.getProductById(id);

            return product.map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        log.info("Product not found with ID: {}", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            log.error("Error getting product by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Tìm sản phẩm theo tên
     */
    @GetMapping("/by-name")
    public ResponseEntity<PageResponse<ProductDetail>> getProductsByName(@RequestParam String name,
                                                                         @RequestParam(required = false) String city,
                                                                         @RequestParam(required = false) BigDecimal minPrice,
                                                                         @RequestParam(required = false) BigDecimal maxPrice,
                                                                         @RequestParam(required = false) Integer yearFrom,
                                                                         @RequestParam(required = false) Integer yearTo,
                                                                         @PageableDefault(page = 0, size = 20, sort = {"isHot", "updatedAt"}, direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            if (name == null || name.trim().isEmpty()) {
                log.warn("Invalid name parameter");
                return ResponseEntity.badRequest().build();
            }

            log.info("Request → Search products by name: {}", name);
            PageResponse<ProductDetail> products = productService.getProductByName(name.trim(),
                    city, minPrice, maxPrice, yearFrom, yearTo, pageable);

            if (products == null || products.getItems() == null || products.getItems().isEmpty()) {
                log.info("No products found with name: {}", name);
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error searching products by name: {}", name, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
