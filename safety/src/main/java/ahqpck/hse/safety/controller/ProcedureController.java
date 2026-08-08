package ahqpck.hse.safety.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ahqpck.hse.safety.model.entity.HseInduction;
import ahqpck.hse.safety.service.HseInductionService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/training")
@Slf4j
@RequiredArgsConstructor
public class ProcedureController {

    private final HseInductionService inductionService;

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

    @GetMapping("/hse-induction")
    public String hseInduction(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        log.info("Viewing HSE Induction Record page - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<HseInduction> inductionPage = inductionService.findAll(pageable);

        model.addAttribute("title", "HSE Induction Records");
        model.addAttribute("pageTitle", "HSE Induction Records");
        model.addAttribute("inductionPage", inductionPage);
        model.addAttribute("pageSize", size);

        return "induction/index";
    }

    @GetMapping("/hse-induction/new")
    public String addHseInduction(Model model) {
        log.info("Viewing Add HSE Induction Record form");
        model.addAttribute("title", "Add HSE Induction Record");
        model.addAttribute("pageTitle", "Add HSE Induction Record");
        return "induction/form";
    }

    @GetMapping("/hse-induction/{code}")
    public String viewHseInductionDetails(
            @PathVariable String code,
            Model model) {
        log.info("Viewing HSE Induction Record details: {}", code);

        HseInduction induction = inductionService.findByCode(code);
        if (induction == null) {
            log.warn("HSE Induction record not found: {}", code);
            return "redirect:/training/hse-induction";
        }

        java.util.List<ahqpck.hse.safety.model.entity.HseInductionFile> files = inductionService.getFilesByCode(code);

        // Separate files into images and documents
        java.util.List<ahqpck.hse.safety.model.entity.HseInductionFile> images = new java.util.ArrayList<>();
        java.util.List<ahqpck.hse.safety.model.entity.HseInductionFile> documents = new java.util.ArrayList<>();

        for (ahqpck.hse.safety.model.entity.HseInductionFile file : files) {
            if (file.getFileType() == ahqpck.hse.safety.model.entity.HseInductionFile.FileType.IMAGE) {
                images.add(file);
            } else {
                documents.add(file);
            }
        }

        model.addAttribute("title", "HSE Induction Record");
        model.addAttribute("pageTitle", "HSE Induction Record Detail");
        model.addAttribute("induction", induction);
        model.addAttribute("files", files);
        model.addAttribute("images", images);
        model.addAttribute("documents", documents);

        return "induction/detail";
    }
}
