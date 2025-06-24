package com.vinaacademy.platform.feature.notification.repository;

import com.vinaacademy.platform.feature.notification.entity.Notification;
import com.vinaacademy.platform.feature.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByRecipientId(UUID userId, Pageable pageable);

    Page<Notification> findByRecipientIdAndType(UUID userId, NotificationType type, Pageable pageable);

    Page<Notification> findByRecipientIdAndIsRead(UUID userId, Boolean isRead, Pageable pageable);

    Page<Notification> findByRecipientIdAndTypeAndIsRead(UUID userId, NotificationType type, Boolean isRead, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.recipientId = :userId AND n.isRead = false")
    int markUnreadAsRead(UUID userId);

    List<Notification> findByIsReadAndRecipientId(Boolean isRead, UUID recipientId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE " +
            "n.id IN :notifications AND n.isRead = false")
    int markRead(List<Notification> notifications);

}