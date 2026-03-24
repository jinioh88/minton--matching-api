package org.app.mintonmatchapi.notification.repository;

import org.app.mintonmatchapi.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUser_IdAndReadAtIsNull(Long userId);

    Optional<Notification> findByIdAndUser_Id(Long notificationId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.user.id = :userId AND n.readAt IS NULL")
    int markAllReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
