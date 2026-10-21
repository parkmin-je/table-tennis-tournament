package com.parkminje.pingpong.notification;

import com.parkminje.pingpong.common.NotFoundException;
import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.player.Player;
import com.parkminje.pingpong.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void notify(User user, Notification.Type type, String title, String message, String link) {
        if (user == null) {
            return;
        }
        notificationRepository.save(new Notification(user, type, title, message, link));
    }

    /** 엔트리에 연결된 회원 전원(선수·파트너·신청자)에게 중복 없이 알림 */
    public void notifyEntry(Entry entry, Notification.Type type, String title, String message, String link) {
        if (entry == null) {
            return;
        }
        Map<Long, User> users = new LinkedHashMap<>();
        addUser(users, entry.getPlayer());
        addUser(users, entry.getPartner());
        if (entry.getApplicant() != null && !entry.getApplicant().isStaff()) {
            users.putIfAbsent(entry.getApplicant().getId(), entry.getApplicant());
        }
        users.values().forEach(u -> notify(u, type, title, message, link));
    }

    private static void addUser(Map<Long, User> users, Player player) {
        if (player != null && player.getUser() != null) {
            users.putIfAbsent(player.getUser().getId(), player.getUser());
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(Long userId, int page) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), 20));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /** 본인 알림만 읽음 처리하고 이동할 링크를 돌려준다 */
    public String read(Long id, Long userId) {
        Notification n = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("알림을 찾을 수 없습니다."));
        n.markRead();
        return n.getLink();
    }

    public void readAll(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    public void delete(Long id, Long userId) {
        Notification n = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("알림을 찾을 수 없습니다."));
        notificationRepository.delete(n);
    }

    public int deleteRead(Long userId) {
        return notificationRepository.deleteRead(userId);
    }
}
