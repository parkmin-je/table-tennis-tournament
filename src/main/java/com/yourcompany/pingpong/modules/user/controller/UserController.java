package com.yourcompany.pingpong.modules.user.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yourcompany.pingpong.domain.Participation;
import com.yourcompany.pingpong.domain.User;
import com.yourcompany.pingpong.modules.match.service.ParticipationService;
import com.yourcompany.pingpong.modules.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final ParticipationService participationService;

    // ⭐⭐⭐ 로그인 GET 핸들러 ⭐⭐⭐
    @GetMapping("/login")
    public String loginForm(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "signup", required = false) String signup,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        if (logout != null) {
            model.addAttribute("logoutMessage", "로그아웃이 성공적으로 완료되었습니다.");
        }

        if (signup != null) {
            model.addAttribute("signupMessage", "회원가입이 완료되었습니다. 로그인해주세요.");
        }

        return "user/login";
    }

    // ⭐⭐⭐ 로그아웃 GET 핸들러 (리다이렉트) ⭐⭐⭐
    @GetMapping("/logout")
    public String logout() {
        log.info("Logout GET request - redirecting to /logout POST");
        return "redirect:/logout";
    }

    // 회원가입 GET
    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("userCreateForm", new UserCreateForm());
        return "user/signup";
    }

    // 회원가입 POST
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute UserCreateForm userCreateForm,
                         BindingResult bindingResult,
                         Model model) {

        if (bindingResult.hasErrors()) {
            return "user/signup";
        }

        if (!userCreateForm.getPassword1().equals(userCreateForm.getPassword2())) {
            bindingResult.rejectValue("password2", "passwordMismatch", "비밀번호가 일치하지 않습니다.");
            return "user/signup";
        }

        try {
            userService.createUser(
                    userCreateForm.getUsername(),
                    userCreateForm.getPassword1(),
                    userCreateForm.getEmail(),
                    userCreateForm.getName()
            );

            log.info("User created successfully: {}", userCreateForm.getUsername());
            return "redirect:/user/login?signup=true";

        } catch (DataIntegrityViolationException e) {
            log.error("User creation failed: {}", e.getMessage());
            bindingResult.reject("signupFailed", "이미 등록된 사용자입니다.");
            return "user/signup";
        } catch (Exception e) {
            log.error("Unexpected error during signup", e);
            bindingResult.reject("signupFailed", "회원가입 중 오류가 발생했습니다.");
            return "user/signup";
        }
    }

    // 프로필 페이지
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/user/login";
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        model.addAttribute("user", user);

        // ⭐ 참가 내역 추가
        List<Participation> participations = participationService.findByUsername(userDetails.getUsername());
        model.addAttribute("participations", participations);

        return "user/profile";
    }

    // ⭐ 비밀번호 변경 폼
    @GetMapping("/changePassword")
    public String changePasswordForm() {
        return "user/change_password";
    }

    // ⭐ 비밀번호 변경 처리
    @PostMapping("/changePassword")
    public String changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("message", "새 비밀번호가 일치하지 않습니다.");
            redirectAttributes.addFlashAttribute("alertType", "danger");
            return "redirect:/user/changePassword";
        }

        try {
            User user = userService.getUserByUsername(userDetails.getUsername());
            userService.changePassword(user.getId(), currentPassword, newPassword);

            redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");
            return "redirect:/user/profile";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "danger");
            return "redirect:/user/changePassword";
        }
    }
}