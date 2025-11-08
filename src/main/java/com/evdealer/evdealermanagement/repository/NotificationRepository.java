package com.evdealer.evdealermanagement.repository;

import com.evdealer.evdealermanagement.entity.notify.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    // Tìm all thông báo của 1 user
    Page<Notification> findByAccountId(String accountId, Pageable pageable);

    // Đếm số thông báo chưa đọc của 1 user
    @Query("SELECT COUNT (n) FROM  Notification n WHERE n.account.id = :accountId AND n.read = false ")
    long countUnreadByAccountId(@Param("accountId") String accountId);

    // Đếm số tb chưa đọc của 1 ng
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.account.id = :accountId AND n.read = false ")
    int markAllAsReadByAccountId(@Param("accountId") String accountId);

    // Xóa tb đã đọc
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.account.id = :accountId")
    int deleteAllByAccountId(@Param("accountId") String accountId);

    // tìm thông báo chưa đọc
    @Query("SELECT n FROM Notification n WHERE n.account.id = :accountId AND n.read = false ORDER BY n.createdAt DESC ")
    Page<Notification> findUnreadByAccountId(@Param("accountId") String accountId, Pageable pageable);

    Page<Notification> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable pageable);
}
