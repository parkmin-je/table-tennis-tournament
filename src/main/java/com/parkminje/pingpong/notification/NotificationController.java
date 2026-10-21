package com.parkminje.pingpong.notification;

import com.parkminje.pingpong.common.web.LoginUser;
import com.parkminje.pingpong.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public String list(@LoginUser User user, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", notificationService.list(user.getId(), page));
        return "notification/list";
    }

    /** 읽음 처리 후 알림의 링크로 이동 */
    @PostMapping("/notifications/{id}/read")
    public String read(@PathVariable Long id, @LoginUser User user) {
        String link = notificationService.read(id, user.getId());
        return "redirect:" + (link != null && link.startsWith("/") && !link.startsWith("//") ? link : "/notifications");
    }

    @PostMapping("/notifications/read-all")
    public String readAll(@LoginUser User user, RedirectAttributes ra) {
        notificationService.readAll(user.getId());
        ra.addFlashAttribute("success", "모든 알림을 읽음 처리했습니다.");
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/{id}/delete")
    public String delete(@PathVariable Long id, @LoginUser User user) {
        notificationService.delete(id, user.getId());
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/delete-read")
    public String deleteRead(@LoginUser User user, RedirectAttributes ra) {
        int n = notificationService.deleteRead(user.getId());
        ra.addFlashAttribute("success", "읽은 알림 " + n + "건을 삭제했습니다.");
        return "redirect:/notifications";
    }

    @GetMapping("/api/notifications/unread-count")
    @ResponseBody
    public Map<String, Long> unread(@LoginUser User user) {
        return Map.of("count", notificationService.unreadCount(user.getId()));
    }
}
