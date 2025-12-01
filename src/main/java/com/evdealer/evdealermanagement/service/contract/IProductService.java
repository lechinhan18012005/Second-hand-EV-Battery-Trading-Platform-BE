package com.evdealer.evdealermanagement.service.contract;

import com.evdealer.evdealermanagement.dto.common.PageResponse;
import com.evdealer.evdealermanagement.dto.post.verification.PostVerifyResponse;
import com.evdealer.evdealermanagement.dto.product.detail.ProductDetail;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing products including vehicles and batteries
 */
public interface IProductService {

    /**
     * Retrieves all available products
     * @return List of all product details, empty list if no products found or error occurs
     */
    PageResponse<PostVerifyResponse> getAllProductsWithStatus(String status, Pageable pageable);

    PageResponse<PostVerifyResponse> getAllProductsWithAllStatus(Pageable pageable);
    /**
     * Retrieves a product by its unique identifier
     * @param id the product ID to search for
     * @return Optional containing the product detail if found, empty Optional otherwise
     * @throws IllegalArgumentException if id is null or invalid
     */
    Optional<ProductDetail> getProductById(String id);

    /**
     * Searches for products by name (supports partial matching)
     * Searches in both vehicle and battery products
     * @param name the product name to search for
     * @return List of matching product details, empty list if none found
     * @throws IllegalArgumentException if name is null or empty
     */
    PageResponse<ProductDetail> getProductByName(String name,
                                                 String city,
                                                 BigDecimal minPrice,
                                                 BigDecimal maxPrice,
                                                 Integer yearFrom,
                                                 Integer yearTo,
                                                 Pageable pageable);

    /**
     * Retrieves products by their type/category
     * @param type the product type to filter by
     * @return List of products matching the specified type
     * @throws IllegalArgumentException if type is null or empty
     * @throws RuntimeException if database error occurs
     */
    List<ProductDetail> getProductByType(String type);

    /**
     * Retrieves products by brand name (supports partial matching)
     * Searches in both vehicle and battery brands
     * @param brand the brand name to search for
     * @return List of products from the specified brand
     * @throws IllegalArgumentException if brand is null or empty
     * @throws RuntimeException if database error occurs
     */
    List<ProductDetail> getProductByBrand(String brand);

    /**
     * Searches for products using multiple criteria
     * @param name product name (optional)
     * @param type product type (optional)
     * @param brand brand name (optional)
     * @param minPrice minimum price (optional)
     * @param maxPrice maximum price (optional)
     * @return List of products matching the criteria
     */
    default List<ProductDetail> searchProducts(String name, String type, String brand,
                                               Double minPrice, Double maxPrice) {
        // Default implementation can be overridden
        throw new UnsupportedOperationException("Advanced search not implemented");
    }

    /**
     * Checks if a product exists by ID
     * @param id the product ID to check
     * @return true if product exists, false otherwise
     */
    default boolean existsById(String id) {
        return getProductById(id).isPresent();
    }

    /**
     * Retrieves all available products
     * @return List of all product details, empty list if no products found or error occurs
     */
    List<ProductDetail> getNewProducts();
}