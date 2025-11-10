package com.evdealer.evdealermanagement.service.implement;


import com.evdealer.evdealermanagement.dto.notify.NotificationResponse;
import com.evdealer.evdealermanagement.entity.account.Account;
import com.evdealer.evdealermanagement.entity.notify.Notification;
import com.evdealer.evdealermanagement.exceptions.AppException;
import com.evdealer.evdealermanagement.exceptions.ErrorCode;
import com.evdealer.evdealermanagement.mapper.notify.NotificationMapper;
import com.evdealer.evdealermanagement.repository.AccountRepository;
import com.evdealer.evdealermanagement.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepo;
    private final NotificationMapper  mapper;

    @Transactional
    public Notification create(String accountId, String title, String content, Notification.NotificationType type, String refId) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Notification n = Notification.builder()
                .account(account)
                .title(title)
                .content(content)
                .type(type)
                .refId(refId)
                .read(false)
                .createdAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                .build();
        return notificationRepository.save(n);
    }

    @Transactional
    public void createAndPush(String accountId, String title, String content,Notification.NotificationType type, String refId) {
        create(accountId, title, content, type, refId);

    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listMyNotifications(String accountId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
        return page.map(mapper::toDTO);
    }


    @Transactional
    public NotificationResponse markAsRead(String accountId, String notificationId){
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

        if (!n.isRead()) {
            n.setRead(true);
            notificationRepository.save(n);
            log.info("User {} marked notification {} as read", accountId, notificationId);
        }
        return mapper.toDTO(n);
    }

    @Transactional
    public int markAllAsRead(String accountId) {
        int updated = notificationRepository.markAllAsReadByAccountId(accountId);
        log.info("User {} marked {} notifications as read", accountId, updated);
        return updated;
    }

    @Transactional(readOnly = true)
    public long countUnread(String accountId) {
        return notificationRepository.countUnreadByAccountId(accountId);
    }

    @Transactional
    public Map<String, Object> deleteOne(String accountId, String notificationId, boolean force) {
        Notification n = notificationRepository.findById(notificationId)
                .orElse(null);

        if (n == null) {
            log.warn("User {} tried to delete non-existent notification {}", accountId, notificationId);
            return Map.of(
                    "success", false,
                    "message", "Thông báo không tồn tại hoặc đã bị xóa."
            );
        }

        // Nếu chưa đọc, cảnh báo nhưng KHÔNG xóa
        if (!n.isRead() && !force) {
            log.info("User {} tried to delete unread notification {} without confirmation", accountId, notificationId);
            return Map.of(
                    "success", false,
                    "message", "Thông báo này chưa được đọc. Vui lòng đọc hoặc xác nhận xóa.",
                    "requireConfirm", true
            );
        }

        // Nếu đã đọc, xóa bình thường
        notificationRepository.delete(n);
        log.info("User {} deleted notification {} (force={})", accountId, notificationId, force);

        return Map.of(
                "success", true,
                "message", force
                        ? "Đã xóa thông báo (kể cả khi chưa đọc)."
                        : "Đã xóa thông báo thành công."
        );
    }

    @Transactional
    public Map<String, Object> deleteAll(String accountId, boolean forceDeleteUnread) {
        long unreadCount = notificationRepository.countUnreadByAccountId(accountId);
        if(unreadCount > 0 && !forceDeleteUnread) {
            String message = "Bạn còn " + unreadCount + " thông báo chưa đọc. " +
                    "Vui lòng đọc hoặc xác nhận xóa tất cả.";
            log.warn("User {} tried to delete all notifications while {} unread remain", accountId, unreadCount);
            return Map.of(
                    "success", false,
                    "message", message,
                    "unreadCount", unreadCount
            );
        }
        int deleted = notificationRepository.deleteAllByAccountId(accountId);
        log.info("User {} deleted {} notifications (force={})", accountId, deleted, forceDeleteUnread);
        return Map.of(
                "success", true,
                "message", "Đã xóa tất cả thông báo" + (forceDeleteUnread ? " (bao gồm cả chưa đọc)" : ""),
                "deletedCount", deleted
        );
    }





}
