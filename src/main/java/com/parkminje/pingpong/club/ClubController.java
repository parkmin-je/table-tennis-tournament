package com.parkminje.pingpong.club;

import com.parkminje.pingpong.player.PlayerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;
    private final PlayerService playerService;

    @Getter
    @Setter
    public static class ClubForm {
        @NotBlank(message = "클럽명을 입력하세요.")
        @Size(max = 100)
        private String name;
        @Size(max = 100)
        private String region;
        @Size(max = 1000)
        private String description;
    }

    @GetMapping("/clubs")
    public String list(Model model) {
        model.addAttribute("clubs", clubService.summaries());
        return "club/list";
    }

    @GetMapping("/club/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("club", clubService.get(id));
        model.addAttribute("members", playerService.clubMembers(id));
        return "club/detail";
    }

    @GetMapping("/manage/clubs/new")
    public String createForm(Model model) {
        model.addAttribute("form", new ClubForm());
        return "club/form";
    }

    @PostMapping("/manage/clubs/new")
    public String create(@Valid @ModelAttribute("form") ClubForm form, BindingResult binding, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            return "club/form";
        }
        Club c = clubService.create(form.getName(), form.getRegion(), form.getDescription());
        ra.addFlashAttribute("success", "클럽을 등록했습니다.");
        return "redirect:/club/" + c.getId();
    }

    @GetMapping("/manage/clubs/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Club c = clubService.get(id);
        ClubForm form = new ClubForm();
        form.setName(c.getName());
        form.setRegion(c.getRegion());
        form.setDescription(c.getDescription());
        model.addAttribute("form", form);
        model.addAttribute("club", c);
        return "club/form";
    }

    @PostMapping("/manage/clubs/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ClubForm form, BindingResult binding,
                         Model model, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            model.addAttribute("club", clubService.get(id));
            return "club/form";
        }
        clubService.update(id, form.getName(), form.getRegion(), form.getDescription());
        ra.addFlashAttribute("success", "클럽 정보를 저장했습니다.");
        return "redirect:/club/" + id;
    }

    @PostMapping("/manage/clubs/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        clubService.delete(id);
        ra.addFlashAttribute("success", "클럽을 삭제했습니다.");
        return "redirect:/clubs";
    }
}
