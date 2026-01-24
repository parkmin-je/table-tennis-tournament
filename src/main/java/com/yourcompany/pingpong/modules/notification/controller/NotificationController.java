package com.yourcompany.pingpong.modules.notification.controller;

import com.yourcompany.pingpong.domain.Notification;
import com.yourcompany.pingpong.domain.User;
import com.yourcompany.pingpong.modules.notification.service.NotificationService;
import com.yourcompany.pingpong.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    /**
     * 알림 목록 페이지
     */
    @GetMapping("/list")
    public String notificationList(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/user/login";
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        List<Notification> notifications = notificationService.getUserNotifications(user);

        model.addAttribute("notifications", notifications);
        return "notification/list";
    }

    /**
     * 알림 읽음 처리
     */
    @PostMapping("/read/{id}")
    public String markAsRead(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAsRead(id);
            redirectAttributes.addFlashAttribute("message", "알림을 확인했습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "알림 처리 중 오류가 발생했습니다.");
            redirectAttributes.addFlashAttribute("alertType", "danger");
        }
        return "redirect:/notification/list";
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PostMapping("/readAll")
    public String markAllAsRead(@AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/user/login";
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        notificationService.markAllAsRead(user);

        redirectAttributes.addFlashAttribute("message", "모든 알림을 확인했습니다.");
        redirectAttributes.addFlashAttribute("alertType", "success");
        return "redirect:/notification/list";
    }

    /**
     * 알림 삭제
     */
    @PostMapping("/delete/{id}")
    public String deleteNotification(@PathVariable("id") Long id,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserByUsername(userDetails.getUsername());
            notificationService.deleteNotification(id, user);

            redirectAttributes.addFlashAttribute("message", "알림이 삭제되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "danger");
        }
        return "redirect:/notification/list";
    }

    /**
     * 읽지 않은 알림 개수 (AJAX)
     */
    @GetMapping("/unread-count")
    @ResponseBody
    public long getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return 0;
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        return notificationService.getUnreadCount(user);
    }
}