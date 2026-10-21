package com.parkminje.pingpong.admin;

import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.common.web.LoginUser;
import com.parkminje.pingpong.tournament.EntryType;
import com.parkminje.pingpong.tournament.GenderRestriction;
import com.parkminje.pingpong.tournament.Tournament;
import com.parkminje.pingpong.tournament.TournamentForm;
import com.parkminje.pingpong.tournament.TournamentFormat;
import com.parkminje.pingpong.tournament.TournamentService;
import com.parkminje.pingpong.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/tournaments")
public class AdminTournamentController {

    private final TournamentService tournamentService;

    @ModelAttribute
    public void options(Model model) {
        model.addAttribute("formats", TournamentFormat.values());
        model.addAttribute("entryTypes", EntryType.values());
        model.addAttribute("genderRestrictions", GenderRestriction.values());
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("tournaments", tournamentService.allForAdmin());
        return "admin/tournaments";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        TournamentForm form = new TournamentForm();
        LocalDate day = LocalDate.now().plusWeeks(2);
        form.setStartAt(day.atTime(LocalTime.of(9, 0)));
        form.setEndAt(day.atTime(LocalTime.of(18, 0)));
        form.setRegistrationDeadline(day.minusDays(3).atTime(LocalTime.of(23, 59)));
        model.addAttribute("form", form);
        return "admin/tournament-form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") TournamentForm form, BindingResult binding,
                         @LoginUser User user, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            return "admin/tournament-form";
        }
        try {
            Tournament t = tournamentService.create(form.toSpec(), user);
            ra.addFlashAttribute("success", "대회가 만들어졌습니다. 내용을 확인한 뒤 '접수 시작'을 누르면 공개됩니다.");
            return "redirect:/manage/tournaments/" + t.getId();
        } catch (BusinessException e) {
            binding.reject("invalid", e.getMessage());
            return "admin/tournament-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Tournament t = tournamentService.get(id);
        model.addAttribute("form", TournamentForm.from(t));
        model.addAttribute("tournament", t);
        return "admin/tournament-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") TournamentForm form, BindingResult binding,
                         Model model, RedirectAttributes ra) {
        if (!binding.hasErrors()) {
            try {
                tournamentService.update(id, form.toSpec());
                ra.addFlashAttribute("success", "대회 정보가 저장되었습니다.");
                return "redirect:/manage/tournaments/" + id;
            } catch (BusinessException e) {
                binding.reject("invalid", e.getMessage());
            }
        }
        model.addAttribute("tournament", tournamentService.get(id));
        return "admin/tournament-form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        tournamentService.delete(id);
        ra.addFlashAttribute("success", "대회를 삭제했습니다.");
        return "redirect:/admin/tournaments";
    }
}
