package com.evdealer.evdealermanagement.controller.post;

import com.evdealer.evdealermanagement.dto.post.packages.PackageRequest;
import com.evdealer.evdealermanagement.dto.post.packages.PackageResponse;
import com.evdealer.evdealermanagement.dto.post.packages.PostPackageResponse;
import com.evdealer.evdealermanagement.service.implement.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/post/payments")
@RestController
@RequiredArgsConstructor
public class PostPackageController {
    private final PaymentService paymentService;

    @PutMapping("/{productId}/package")
    public ResponseEntity<PackageResponse> choosePackage(@PathVariable String productId,
            @RequestBody PackageRequest packageRequest) {
        PackageResponse packageResponse = paymentService.choosePackage(productId, packageRequest);
        return ResponseEntity.ok(packageResponse);
    }

    @PostMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestParam String paymentId, @RequestParam boolean success) {
        paymentService.handlePaymentCallback(paymentId, success);
        return ResponseEntity.ok("success");
    }

    @GetMapping("/show")
    public List<PostPackageResponse> getAllPackages() {
        return paymentService.getAllPackages();
    }

    @PostMapping("/retry/{paymentId}")
    public ResponseEntity<PackageResponse> retryPayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.retryPackagePayment(paymentId));
    }
}
