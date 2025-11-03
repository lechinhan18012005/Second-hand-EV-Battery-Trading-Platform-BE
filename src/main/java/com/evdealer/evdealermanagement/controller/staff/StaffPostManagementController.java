package com.evdealer.evdealermanagement.controller.staff;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evdealer.evdealermanagement.dto.rate.ApprovalRateResponse;
import com.evdealer.evdealermanagement.service.implement.StaffService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@RestController
@RequestMapping("/staff/post")
@RequiredArgsConstructor
public class StaffPostManagementController {

    private final StaffService staffService;

    @GetMapping("/approval-rate")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<ApprovalRateResponse> getApprovalRate() {
        return ResponseEntity.ok(staffService.getApprovalRate());
    }

    @GetMapping("/approval-rate")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApprovalRateResponse> getApprovalRateByDate(@RequestParam
                                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date) {
        return ResponseEntity.ok(staffService.getApprovalRateByDate(date));
    }
}
