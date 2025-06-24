package com.vinaacademy.platform.feature.notification.service.impl;

import com.vinaacademy.platform.client.UserClient;
import com.vinaacademy.platform.client.dto.UserDto;
import com.vinaacademy.platform.exception.BadRequestException;
import com.vinaacademy.platform.exception.UnauthorizedException;
import com.vinaacademy.platform.feature.notification.dto.NotificationCreateDTO;
import com.vinaacademy.platform.feature.notification.dto.NotificationDTO;
import com.vinaacademy.platform.feature.notification.entity.Notification;
import com.vinaacademy.platform.feature.notification.enums.NotificationType;
import com.vinaacademy.platform.feature.notification.mapper.NotificationMapper;
import com.vinaacademy.platform.feature.notification.observer.NotificationAction;
import com.vinaacademy.platform.feature.notification.observer.NotificationSubject;
import com.vinaacademy.platform.feature.notification.repository.NotificationRepository;
import com.vinaacademy.platform.feature.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.vinaacademy.common.security.SecurityContextHelper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    @Autowired
    private UserClient userClient;
    private final SecurityContextHelper securityContextHelper;

    private final NotificationSubject notificationPublisher;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDTO createNotification(NotificationCreateDTO dto) {
        UserDto user = userClient.getUserByIdAsDto(dto.getUserId()).getData();
        Notification notification = NotificationMapper.INSTANCE.toEntity(dto);
        notification.setRecipientId(user.getId());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setIsRead(false);
        notification = notificationRepository.save(notification);


        NotificationDTO result = NotificationMapper.INSTANCE.toDTO(notification);

        notificationPublisher.notifyObservers(result, NotificationAction.CREATE);
        return result;
    }

    // Lấy danh sách các notifications từ user lấy từ Auth backend thông
    // qua tokenAccess
    @Override
    public Page<NotificationDTO> getUserNotificationsPaginated(Boolean read, NotificationType type, Pageable pageable) {
        UUID currentUserId = securityContextHelper.getCurrentUserIdAsUUID();
        Page<Notification> page;

        if (type != null && read != null) {
            page = notificationRepository.findByRecipientIdAndTypeAndIsRead(currentUserId, type, read, pageable);
        } else if (type != null) {
            page = notificationRepository.findByRecipientIdAndType(currentUserId, type, pageable);
        } else if (read != null) {
            page = notificationRepository.findByRecipientIdAndIsRead(currentUserId, read, pageable);
        } else {
            page = notificationRepository.findByRecipientId(currentUserId, pageable);
        }

        return page.map(NotificationMapper.INSTANCE::toDTO);

    }

    //đánh dấu đã đọc cho noti
    @Override
    public void markAsRead(UUID notificationId) {
        Notification notification = findNotification(notificationId);
        UUID currentUserId = securityContextHelper.getCurrentUserIdAsUUID();
        if (!notification.getRecipientId().equals(currentUserId))
            throw UnauthorizedException.message("Xác thực thất bại vui lòng kiểm tra lại");
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);

        notificationPublisher.notifyObservers(NotificationMapper.INSTANCE.toDTO(notification),
                NotificationAction.READ);
    }

    //chuyển noti sang status bị xóa
    @Override
    public void deleteNotification(UUID notificationId) {
        Notification notification = findNotification(notificationId);
        UUID currentUserId = securityContextHelper.getCurrentUserIdAsUUID();
        if (!notification.getRecipientId().equals(currentUserId))
            throw UnauthorizedException.message("Xác thực thất bại vui lòng kiểm tra lại");
        notificationRepository.delete(notification); // Perform hard deletion

        notificationPublisher.notifyObservers(NotificationMapper.INSTANCE.toDTO(notification),
                NotificationAction.DELETE);
    }

    public Notification findNotification(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> BadRequestException.message("Không tìm thấy thông báo"));
    }

    // đánh dấu đã đọc tất cả noti
    @Override
    @Transactional
    public void markReadAll() {
        UUID currentUserId = securityContextHelper.getCurrentUserIdAsUUID();
        List<Notification> unreadNotis = notificationRepository.findByIsReadAndRecipientId(false, currentUserId);

        if (unreadNotis.isEmpty()) return;

        notificationRepository.markRead(unreadNotis); // marks in DB in a transaction

        unreadNotis.forEach(notification ->
                notificationPublisher.notifyObservers(
                        NotificationMapper.INSTANCE.toDTO(notification),
                        NotificationAction.READ
                )
        );
    }

}