package ahqpck.hse.safety.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/inspection")
@Slf4j
public class InspectionController {

    @GetMapping("/fire-safety")
    public String fireSafety(Model model) {
        log.info("Viewing Fire Safety Inspection page");
        model.addAttribute("title", "Fire Safety Inspection");
        model.addAttribute("pageTitle", "Fire Safety Inspection Records");
        return "inspection/fire-safety";
    }

    @GetMapping("/environment")
    public String environment(Model model) {
        log.info("Viewing Environment Inspection page");
        model.addAttribute("title", "Environment Inspection");
        model.addAttribute("pageTitle", "Environment Inspection Records");
        return "inspection/environment";
    }

    @GetMapping("/first-aid")
    public String firstAid(Model model) {
        log.info("Viewing First Aid Inspection page");
        model.addAttribute("title", "First Aid Inspection");
        model.addAttribute("pageTitle", "First Aid Inspection Records");
        return "inspection/first-aid";
    }

    @GetMapping("/spill-kit")
    public String spillKit(Model model) {
        log.info("Viewing Spill Kit Inspection page");
        model.addAttribute("title", "Spill Kit Inspection");
        model.addAttribute("pageTitle", "Spill Kit Inspection Records");
        return "inspection/spill-kit";
    }

    @GetMapping("/other")
    public String other(Model model) {
        log.info("Viewing Other Inspection page");
        model.addAttribute("title", "Other Inspection");
        model.addAttribute("pageTitle", "Other Inspection Records");
        return "inspection/other";
    }
}
