package com.yourcompany.pingpong.modules.notification.repository;

import com.yourcompany.pingpong.domain.Notification;
import com.yourcompany.pingpong.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.tournament WHERE n.user = :user ORDER BY n.createdAt DESC")
    List<Notification> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    List<Notification> findByUserAndIsRead(User user, Boolean isRead);

    long countByUserAndIsRead(User user, Boolean isRead);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.tournament WHERE n.user = :user AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUser(@Param("user") User user);
}