package com.evdealer.evdealermanagement.controller.vehicle;

import com.evdealer.evdealermanagement.dto.product.similar.SimilarProductResponse;
import com.evdealer.evdealermanagement.dto.vehicle.brand.VehicleBrandsResponse;
import com.evdealer.evdealermanagement.dto.vehicle.brand.VehicleCategoriesResponse;
import com.evdealer.evdealermanagement.dto.vehicle.detail.VehicleDetailResponse;
import com.evdealer.evdealermanagement.dto.vehicle.model.VehicleModelRequest;
import com.evdealer.evdealermanagement.dto.vehicle.model.VehicleModelResponse;
import com.evdealer.evdealermanagement.dto.vehicle.model.VehicleModelVersionRequest;
import com.evdealer.evdealermanagement.dto.vehicle.model.VehicleModelVersionResponse;

import com.evdealer.evdealermanagement.service.implement.GeminiRestService;
import com.evdealer.evdealermanagement.service.implement.VehicleService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/vehicle")
public class VehicleController {

    private final VehicleService vehicleService;
    private final GeminiRestService geminiRestService;

    @GetMapping("/brands/all")
    public List<VehicleBrandsResponse> getAllBrands() {
        return vehicleService.listAllVehicleBrandsSorted();
    }

    @GetMapping("/categories/all")
    public List<VehicleCategoriesResponse> getAllCategories() {
        return vehicleService.listAllVehicleCategoriesSorted();
    }

    @GetMapping("/brands/logoName")
    public List<VehicleBrandsResponse> getAllBrandsLogoName() {
        return vehicleService.listAllVehicleNameAndLogo();
    }

    @PostMapping("/models/all")
    public List<VehicleModelResponse> getAllModels(@RequestBody VehicleModelRequest request) {
        return vehicleService.listAllVehicleModelsSorted(request);
    }

    @PostMapping("/model/versions")
    public List<VehicleModelVersionResponse> getAllModelVersions(@RequestBody VehicleModelVersionRequest request) {
        return vehicleService.listAllVehicleModelVersionsSorted(request);
    }

    @GetMapping("/catalog/{id}")
    public VehicleDetailResponse getVehicleDetailResponse(@PathVariable String id) {
        return vehicleService.getVehicleDetailsInfo(id);
    }

    @GetMapping("/{productId}/similar")
    public ResponseEntity<List<SimilarProductResponse>> getSimilarVehicles(@PathVariable String productId) {
        List<SimilarProductResponse> result = vehicleService.getSimilarVehicles(productId);
        return ResponseEntity.ok(result);
    }
}
