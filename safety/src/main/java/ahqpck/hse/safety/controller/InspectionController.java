package ahqpck.hse.safety.controller;

import ahqpck.hse.safety.model.entity.HseDocument;
import ahqpck.hse.safety.service.HseDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/inspection")
@Slf4j
public class InspectionController {

    @Autowired
    private HseDocumentService documentService;

    @GetMapping("/fire-safety")
    public String fireSafety(Model model) {
        log.info("Viewing Fire Safety Inspection page");
        model.addAttribute("title", "Fire Safety Inspection");
        model.addAttribute("pageTitle", "Fire Safety Inspection Documents");
        model.addAttribute("moduleUrl", "fire-safety-inspection");
        model.addAttribute("documents", documentService.findByModule(HseDocument.Module.FIRE_SAFETY_INSPECTION));
        return "inspection/fire-safety";
    }

    @GetMapping("/environment")
    public String environment(Model model) {
        log.info("Viewing Environment Inspection page");
        model.addAttribute("title", "Environment Inspection");
        model.addAttribute("pageTitle", "Environment Inspection Documents");
        model.addAttribute("moduleUrl", "environment-inspection");
        model.addAttribute("documents", documentService.findByModule(HseDocument.Module.ENVIRONMENT_INSPECTION));
        return "inspection/environment";
    }

    @GetMapping("/first-aid")
    public String firstAid(Model model) {
        log.info("Viewing First Aid Inspection page");
        model.addAttribute("title", "First Aid Inspection");
        model.addAttribute("pageTitle", "First Aid Inspection Documents");
        model.addAttribute("moduleUrl", "first-aid-inspection");
        model.addAttribute("documents", documentService.findByModule(HseDocument.Module.FIRST_AID_INSPECTION));
        return "inspection/first-aid";
    }

    @GetMapping("/spill-kit")
    public String spillKit(Model model) {
        log.info("Viewing Spill Kit Inspection page");
        model.addAttribute("title", "Spill Kit Inspection");
        model.addAttribute("pageTitle", "Spill Kit Inspection Documents");
        model.addAttribute("moduleUrl", "spill-kit-inspection");
        model.addAttribute("documents", documentService.findByModule(HseDocument.Module.SPILL_KIT_INSPECTION));
        return "inspection/spill-kit";
    }

    @GetMapping("/other")
    public String other(Model model) {
        log.info("Viewing Other Inspection page");
        model.addAttribute("title", "Other Inspection");
        model.addAttribute("pageTitle", "Other Inspection Documents");
        model.addAttribute("moduleUrl", "other-inspection");
        model.addAttribute("documents", documentService.findByModule(HseDocument.Module.OTHER_INSPECTION));
        return "inspection/other";
    }
}
