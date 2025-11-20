package com.evdealer.evdealermanagement.utils;

import com.evdealer.evdealermanagement.entity.product.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductSpecs {

    public static Specification<Product> hasStatus(Product.Status status) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Product> titleLike(String name) {
        if(isBlank(name)) {
            return null;
        }
        String like = "%" + name.trim().toLowerCase() + "%";
        return (r, q, cb) -> cb.like(cb.lower(r.get("title")), like);
    }

    public static Specification<Product> hasType(Product.ProductType type) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("type"), type);
    }

    public static Specification<Product> hasVehicleBrandId(String brandId) {
        return (root, query, cb) -> {
            if (isBlank(brandId)) return null;
            query.distinct(true); // tránh trùng dòng khi JOIN
            //JOIN product -> VehicleDeatails
            Join<Object, Object> vehicle = root.join("vehicleDetails", JoinType.LEFT);
            //JOIN VehicleDetails -> VehicleBrands
            Join<Object, Object> brand = vehicle.join("brand", JoinType.LEFT);
            return cb.equal(brand.get("id"), brandId.trim());
        };
    }

    public static Specification<Product> hasBatteryBrandId(String brandId) {
        return (root, query, cb) -> {
            if (isBlank(brandId)) return null;
            query.distinct(true);
            Join<Object, Object> battery = root.join("batteryDetails", JoinType.LEFT);
            Join<Object, Object> brand = battery.join("brand", JoinType.LEFT);
            return cb.equal(brand.get("id"), brandId.trim());
        };
    }

    public static Specification<Product> hasAnyBrand(String brandId) {
        return (root, query, cb) -> {
            if (brandId == null || brandId.trim().isEmpty()) {
                return null; // không filter brand nếu FE không gửi
            }
            query.distinct(true);

            // join 2 nhánh
            var vehicleJoin = root.join("vehicleDetails", JoinType.LEFT);
            var vehicleBrand = vehicleJoin.join("brand", JoinType.LEFT);

            var batteryJoin = root.join("batteryDetails", JoinType.LEFT);
            var batteryBrand = batteryJoin.join("brand", JoinType.LEFT);

            // coalesce(vBrand.id, bBrand.id) = :brandId
            return cb.equal(
                    cb.coalesce(vehicleBrand.get("id"), batteryBrand.get("id")),
                    brandId.trim() // coalesce giúp lấy ra cái ko null
            );
        };
    }


    public static Specification<Product> cityEq(String city) {
        return (r, q, cb) -> isBlank(city) ? null : cb.equal(cb.lower(r.get("city")), city.trim().toLowerCase());
    }

    public static Specification<Product> districtEq(String district) {
        return (r, q, cb) -> isBlank(district) ? null : cb.equal(cb.lower(r.get("district")), district.trim().toLowerCase());
    }

    public static Specification<Product> priceGte(BigDecimal min) {
        return (r, q, cb) -> min == null ? null : cb.greaterThanOrEqualTo(r.get("price"), min);
    }

    public static Specification<Product> priceLte(BigDecimal max) {
        return (r, q, cb) -> max == null ? null : cb.lessThanOrEqualTo(r.get("price"), max);
    }

    public static Specification<Product> yearGte(Integer from) {
        return (r, q, cb) -> from == null ? null : cb.greaterThanOrEqualTo(r.get("manufactureYear"), from.shortValue());
    }

    public static Specification<Product> yearLte(Integer to) {
        return (r, q, cb) -> to == null ? null : cb.lessThanOrEqualTo(r.get("manufactureYear"), to.shortValue());
    }

    public static Specification<Product> all() {
        return ((root, query, criteriaBuilder) -> criteriaBuilder.conjunction());
    }


    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
