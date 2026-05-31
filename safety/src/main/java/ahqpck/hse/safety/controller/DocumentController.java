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
@RequestMapping("/documents")
@Slf4j
public class DocumentController {

    @Autowired
    private HseDocumentService documentService;

    @GetMapping("/risk-assessment")
    public String riskAssessment(Model model) {
        log.info("Viewing Risk Assessment page");
        model.addAttribute("title", "Risk Assessment");
        model.addAttribute("pageTitle", "Risk Assessment Documents");
        model.addAttribute("moduleUrl", "risk-assessment");
        model.addAttribute("documents", documentService.findByModule(HseDocument.Module.RISK_ASSESSMENT));
        return "documents/risk-assessment";
    }

    @GetMapping("/work-instruction")
    public String workInstruction(Model model) {
        log.info("Viewing Work Instruction page");
        model.addAttribute("title", "Work Instruction");
        model.addAttribute("pageTitle", "Work Instruction Documents");
        model.addAttribute("moduleUrl", "work-instruction");
        model.addAttribute("documents", documentService.findByModule(HseDocument.Module.WORK_INSTRUCTION));
        return "documents/work-instruction";
    }

    @GetMapping("/work-permit")
    public String workPermit(Model model) {
        log.info("Viewing Work Permit page");
        model.addAttribute("title", "Work Permit");
        model.addAttribute("pageTitle", "Work Permit Documents");
        model.addAttribute("moduleUrl", "work-permit");
        model.addAttribute("documents", documentService.findByModule(HseDocument.Module.WORK_PERMIT));
        return "documents/work-permit";
    }

    @GetMapping("/hse-form")
    public String hseForm(Model model) {
        log.info("Viewing HSE Form page");
        model.addAttribute("title", "HSE Form");
        model.addAttribute("pageTitle", "HSE Form Documents");
        model.addAttribute("moduleUrl", "hse-form");
        model.addAttribute("documents", documentService.findByModule(HseDocument.Module.HSE_FORM));
        return "documents/hse-form";
    }
}
