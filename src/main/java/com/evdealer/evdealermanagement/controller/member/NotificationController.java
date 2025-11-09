package com.evdealer.evdealermanagement.controller.member;

import com.evdealer.evdealermanagement.dto.account.custom.CustomAccountDetails;
import com.evdealer.evdealermanagement.dto.notify.NotificationResponse;
import com.evdealer.evdealermanagement.service.implement.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/member/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // Lấy danh sách thông báo của user hiện tại
    @GetMapping
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Page<NotificationResponse>> list(@AuthenticationPrincipal CustomAccountDetails user,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        String accountId = user.getAccountId();
        Page<NotificationResponse> notifications = notificationService.listMyNotifications(accountId, pageable);
        log.debug("User {} fetched {} notifications (page {})",
                accountId, notifications.getNumberOfElements(), pageable.getPageNumber());
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Map<String, Object>> markAsRead(@AuthenticationPrincipal CustomAccountDetails user,
            @PathVariable String id) {
        String accountId = user.getAccountId();
        NotificationResponse res    = notificationService.markAsRead(accountId, id);
        return ResponseEntity.ok(Map.of(
                "message", res.isRead() ? "Đã đánh dấu là đã đọc" : "Thông báo đã được đọc trước đó",
                "notification", res));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal CustomAccountDetails user) {

        String accountId = user.getAccountId();
        int updatedCount = notificationService.markAllAsRead(accountId);

        return ResponseEntity.ok(Map.of(
                "message", "Đã đánh dấu tất cả thông báo là đã đọc",
                "updatedCount", updatedCount));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal CustomAccountDetails user) {

        String accountId = user.getAccountId();
        long unread = notificationService.countUnread(accountId);
        return ResponseEntity.ok(Map.of("unreadCount", unread));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Map<String, Object>> delete(
            @AuthenticationPrincipal CustomAccountDetails user,
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean force) {

        String accountId = user.getAccountId();
        Map<String, Object> result = notificationService.deleteOne(accountId, id, force);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/all")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Map<String, Object>> deleteAll(
            @AuthenticationPrincipal CustomAccountDetails user,
            @RequestParam(defaultValue = "false") boolean force) {

        String accountId = user.getAccountId();
        Map<String, Object> result = notificationService.deleteAll(accountId, force);
        return ResponseEntity.ok(result);
    }

}
