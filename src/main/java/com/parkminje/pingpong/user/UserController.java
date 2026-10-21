package com.parkminje.pingpong.user;

import com.parkminje.pingpong.club.ClubService;
import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.common.web.LoginUser;
import com.parkminje.pingpong.player.Gender;
import com.parkminje.pingpong.player.Player;
import com.parkminje.pingpong.player.PlayerService;
import com.parkminje.pingpong.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PlayerService playerService;
    private final ClubService clubService;

    @GetMapping("/user/login")
    public String login(@LoginUser User user) {
        return user != null ? "redirect:/" : "user/login";
    }

    @GetMapping("/user/signup")
    public String signupForm(Model model) {
        model.addAttribute("form", new UserForms.SignUp());
        return "user/signup";
    }

    @PostMapping("/user/signup")
    public String signup(@Valid @ModelAttribute("form") UserForms.SignUp form, BindingResult binding, RedirectAttributes ra) {
        if (!binding.hasFieldErrors("passwordConfirm") && form.getPassword() != null
                && !form.getPassword().equals(form.getPasswordConfirm())) {
            binding.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다.");
        }
        if (binding.hasErrors()) {
            return "user/signup";
        }
        try {
            userService.signUp(form.getUsername(), form.getPassword(), form.getEmail(), form.getName());
        } catch (BusinessException e) {
            binding.reject("duplicate", e.getMessage());
            return "user/signup";
        }
        ra.addFlashAttribute("success", "가입이 완료되었습니다. 로그인 후 '내 선수 프로필'을 등록하면 대회 신청을 할 수 있습니다.");
        return "redirect:/user/login";
    }

    @GetMapping("/me")
    public String me(@LoginUser User user, Model model) {
        Player player = playerService.findByUser(user.getId()).orElse(null);
        model.addAttribute("user", user);
        model.addAttribute("player", player);
        if (player != null) {
            model.addAttribute("entries", playerService.entries(player.getId()));
            model.addAttribute("stats", playerService.stats(player.getId()));
        }
        if (!model.containsAttribute("profileForm")) {
            UserForms.Profile pf = new UserForms.Profile();
            pf.setName(user.getName());
            pf.setEmail(user.getEmail());
            model.addAttribute("profileForm", pf);
        }
        return "user/me";
    }

    @PostMapping("/me/profile")
    public String updateProfile(@LoginUser User user, @Valid @ModelAttribute("profileForm") UserForms.Profile form,
                                BindingResult binding, RedirectAttributes ra, HttpServletRequest request) {
        if (binding.hasErrors()) {
            ra.addFlashAttribute("flashError", binding.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/me";
        }
        userService.updateProfile(user.getId(), form.getName(), form.getEmail());
        playerService.syncName(user.getId(), form.getName().trim());
        refreshPrincipal(user.getId(), request);
        ra.addFlashAttribute("success", "회원 정보가 저장되었습니다.");
        return "redirect:/me";
    }

    @GetMapping("/me/password")
    public String passwordForm(Model model) {
        model.addAttribute("form", new UserForms.Password());
        return "user/password";
    }

    @PostMapping("/me/password")
    public String changePassword(@LoginUser User user, @Valid @ModelAttribute("form") UserForms.Password form,
                                 BindingResult binding, RedirectAttributes ra) {
        if (!binding.hasFieldErrors("confirm") && form.getNext() != null && !form.getNext().equals(form.getConfirm())) {
            binding.rejectValue("confirm", "mismatch", "새 비밀번호가 일치하지 않습니다.");
        }
        if (binding.hasErrors()) {
            return "user/password";
        }
        try {
            userService.changePassword(user.getId(), form.getCurrent(), form.getNext());
        } catch (BusinessException e) {
            binding.reject("invalid", e.getMessage());
            return "user/password";
        }
        ra.addFlashAttribute("success", "비밀번호가 변경되었습니다.");
        return "redirect:/me";
    }

    // ── 내 선수 프로필 ───────────────────────────────────

    @Getter
    @Setter
    public static class MyPlayerForm {
        private Gender gender;
        private Integer birthYear;
        private Integer division;
        private String phone;
        private Long clubId;
    }

    @GetMapping("/me/player")
    public String myPlayerForm(@LoginUser User user, Model model) {
        MyPlayerForm form = new MyPlayerForm();
        playerService.findByUser(user.getId()).ifPresent(p -> {
            form.setGender(p.getGender());
            form.setBirthYear(p.getBirthYear());
            form.setDivision(p.getDivision());
            form.setPhone(p.getPhone());
            form.setClubId(p.getClub() == null ? null : p.getClub().getId());
        });
        model.addAttribute("form", form);
        model.addAttribute("clubs", clubService.all());
        model.addAttribute("genders", Gender.values());
        return "user/player-form";
    }

    @PostMapping("/me/player")
    public String saveMyPlayer(@LoginUser User user, @ModelAttribute("form") MyPlayerForm form, RedirectAttributes ra) {
        playerService.saveMyProfile(user, new PlayerService.PlayerInput(user.getName(), form.getGender(), form.getBirthYear(),
                form.getDivision(), form.getPhone(), form.getClubId()));
        ra.addFlashAttribute("success", "선수 프로필이 저장되었습니다.");
        return "redirect:/me";
    }

    /** 이름 등 세션 정보 갱신 */
    private void refreshPrincipal(Long userId, HttpServletRequest request) {
        User fresh = userService.get(userId);
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = new UserPrincipal(fresh);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        if (current != null) {
            SecurityContextHolder.setContext(context);
            request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        }
    }
}
