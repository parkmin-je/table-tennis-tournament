package com.parkminje.pingpong.common.web;

import com.parkminje.pingpong.config.AppProperties;
import com.parkminje.pingpong.notification.NotificationService;
import com.parkminje.pingpong.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 모든 화면 공통 모델 (사이트명, 로그인 사용자, 읽지 않은 알림 수) */
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final AppProperties props;
    private final NotificationService notificationService;

    @ModelAttribute
    public void common(Model model) {
        model.addAttribute("appName", props.name());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal me) {
            model.addAttribute("me", me);
            model.addAttribute("unreadCount", notificationService.unreadCount(me.getId()));
        }
    }
}
