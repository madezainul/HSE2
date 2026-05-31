package ahqpck.hse.safety.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/procedure")
@Slf4j
public class ProcedureController {

    @GetMapping("/training")
    public String hseTraining(Model model) {
        log.info("Viewing HSE Training page");
        model.addAttribute("title", "HSE Training");
        model.addAttribute("pageTitle", "HSE Training Records");
        return "procedure/training";
    }

    @GetMapping("/hse-index")
    public String hseIndex(Model model) {
        log.info("Viewing HSE Index Record page");
        model.addAttribute("title", "HSE Index Record");
        model.addAttribute("pageTitle", "HSE Index Records");
        return "procedure/hse-index";
    }

    @GetMapping("/external-training")
    public String externalTraining(Model model) {
        log.info("Viewing HSE External Training Record page");
        model.addAttribute("title", "HSE External Training Record");
        model.addAttribute("pageTitle", "HSE External Training Records");
        return "procedure/external-training";
    }

    @GetMapping("/certification")
    public String certification(Model model) {
        log.info("Viewing Certification page");
        model.addAttribute("title", "Certification");
        model.addAttribute("pageTitle", "Certification Records");
        return "procedure/certification";
    }
}
