package com.parkminje.pingpong.player;

import com.parkminje.pingpong.club.ClubService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;
    private final ClubService clubService;

    @Getter
    @Setter
    public static class PlayerForm {
        private String name;
        private Gender gender;
        private Integer birthYear;
        private Integer division;
        private String phone;
        private Long clubId;

        PlayerService.PlayerInput toInput() {
            return new PlayerService.PlayerInput(name, gender, birthYear, division, phone, clubId);
        }
    }

    @GetMapping("/players")
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) Long clubId,
                       @RequestParam(required = false) Integer division, @RequestParam(required = false) Gender gender,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", playerService.search(q, clubId, division, gender, page));
        model.addAttribute("q", q);
        model.addAttribute("clubId", clubId);
        model.addAttribute("division", division);
        model.addAttribute("gender", gender);
        model.addAttribute("clubs", clubService.all());
        model.addAttribute("genders", Gender.values());
        return "player/list";
    }

    @GetMapping("/player/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("player", playerService.get(id));
        model.addAttribute("stats", playerService.stats(id));
        model.addAttribute("history", playerService.ratingHistory(id));
        model.addAttribute("entries", playerService.entries(id));
        return "player/detail";
    }

    @GetMapping("/ranking")
    public String ranking(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", playerService.ranking(page));
        return "player/ranking";
    }

    // ── 운영진 ────────────────────────────────────────────

    @GetMapping("/manage/players/new")
    public String createForm(Model model) {
        model.addAttribute("form", new PlayerForm());
        formOptions(model);
        return "player/form";
    }

    @PostMapping("/manage/players/new")
    public String create(@ModelAttribute("form") PlayerForm form, RedirectAttributes ra) {
        Player p = playerService.create(form.toInput());
        ra.addFlashAttribute("success", p.getName() + " 선수를 등록했습니다.");
        return "redirect:/player/" + p.getId();
    }

    @GetMapping("/manage/players/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Player p = playerService.get(id);
        PlayerForm form = new PlayerForm();
        form.setName(p.getName());
        form.setGender(p.getGender());
        form.setBirthYear(p.getBirthYear());
        form.setDivision(p.getDivision());
        form.setPhone(p.getPhone());
        form.setClubId(p.getClub() == null ? null : p.getClub().getId());
        model.addAttribute("form", form);
        model.addAttribute("player", p);
        formOptions(model);
        return "player/form";
    }

    @PostMapping("/manage/players/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute("form") PlayerForm form, RedirectAttributes ra) {
        playerService.update(id, form.toInput());
        ra.addFlashAttribute("success", "선수 정보를 저장했습니다.");
        return "redirect:/player/" + id;
    }

    @PostMapping("/manage/players/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        playerService.delete(id);
        ra.addFlashAttribute("success", "선수를 삭제했습니다.");
        return "redirect:/players";
    }

    private void formOptions(Model model) {
        model.addAttribute("clubs", clubService.all());
        model.addAttribute("genders", Gender.values());
    }
}
