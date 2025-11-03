package com.evdealer.evdealermanagement.controller.member;

import com.evdealer.evdealermanagement.repository.AccountRepository;
import com.evdealer.evdealermanagement.service.implement.MemberService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.evdealer.evdealermanagement.service.implement.ProfileService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberAccountController {

    private final ProfileService profileService;
    private final MemberService memberService;

    @DeleteMapping("/profile/me/delete")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Void> deleteUser(Authentication authentication) {
        profileService.deleteAccount(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/new/count")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> countAccountMemberInPeriod(@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date) {
        long count = memberService.countAccountMemberInPeriod(date);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("date", (date != null) ? date : LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString());
        body.put("count", count);
        return ResponseEntity.ok(body);
    }
}
