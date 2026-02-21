package com.yourcompany.pingpong.modules.notification.service;

import com.yourcompany.pingpong.domain.Notification;
import com.yourcompany.pingpong.domain.Tournament;
import com.yourcompany.pingpong.domain.User;
import com.yourcompany.pingpong.modules.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 알림 생성
     */
    public Notification createNotification(User user, String title, String message,
                                           String type, Tournament tournament) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .tournament(tournament)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        log.info("NOTIFICATION: Created for user {} - {}", user.getUsername(), title);
        return notificationRepository.save(notification);
    }

    /**
     * 사용자의 모든 알림 조회
     */
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * 읽지 않은 알림만 조회
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findUnreadByUser(user);
    }

    /**
     * 읽지 않은 알림 개수
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsRead(user, false);
    }

    /**
     * 알림 읽음 처리
     */
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * 모든 알림 읽음 처리
     */
    public void markAllAsRead(User user) {
        List<Notification> notifications = notificationRepository.findByUserAndIsRead(user, false);
        notifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications);
        log.info("NOTIFICATION: Marked all as read for user {}", user.getUsername());
    }

    /**
     * 알림 삭제
     */
    public void deleteNotification(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("본인의 알림만 삭제할 수 있습니다.");
        }

        notificationRepository.delete(notification);
    }
}