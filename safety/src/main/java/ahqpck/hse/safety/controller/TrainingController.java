package ahqpck.hse.safety.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ahqpck.hse.safety.exception.ResourceNotFoundException;
import ahqpck.hse.safety.model.entity.HseCertification;
import ahqpck.hse.safety.model.entity.HseExternalTraining;
import ahqpck.hse.safety.model.entity.HseInduction;
import ahqpck.hse.safety.model.entity.HseInternalTraining;
import ahqpck.hse.safety.service.HseTrainingService;
import ahqpck.hse.safety.service.HseCertificationService;
import ahqpck.hse.safety.service.HseExternalTrainingService;
import ahqpck.hse.safety.service.HseInternalTrainingService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/training")
@Slf4j
@RequiredArgsConstructor
public class TrainingController {

    private final HseTrainingService trainingService;
    private final HseInternalTrainingService internalTrainingService;
    private final HseExternalTrainingService externalTrainingService;
    private final HseCertificationService certificationService;

    @GetMapping("/external-training")
    public String externalTraining(Model model) {
        log.info("Viewing HSE External Training page");

        try {
            // Fetch recent records (last 5)
            Pageable recentPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<HseExternalTraining> recentRecords = externalTrainingService.findAll(recentPageable);

            model.addAttribute("title", "HSE External Training Records");
            model.addAttribute("pageTitle", "HSE External Training Records");
            model.addAttribute("recentRecords", recentRecords.getContent());

            log.info("HSE External Training dashboard loaded");
        } catch (Exception e) {
            log.error("Error loading HSE External Training dashboard", e);
            model.addAttribute("recentRecords", java.util.Collections.emptyList());
        }

        return "training/external-training";
    }

    @GetMapping("/hse-external-training")
    public String hseExternalTraining(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        log.info("Viewing HSE External Training Record page - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<HseExternalTraining> trainingPage = externalTrainingService.findAll(pageable);

        model.addAttribute("title", "HSE External Training Records");
        model.addAttribute("pageTitle", "HSE External Training Records");
        model.addAttribute("trainingPage", trainingPage);
        model.addAttribute("pageSize", size);

        return "external-training/index";
    }

    @GetMapping("/hse-external-training/{code}")
    public String viewHseExternalTrainingDetails(
            @PathVariable String code,
            Model model) {
        log.info("Viewing HSE External Training Record details: {}", code);

        HseExternalTraining training;
        try {
            training = externalTrainingService.findByCode(code);
        } catch (ResourceNotFoundException e) {
            log.warn("HSE External Training record not found: {}", code);
            return "redirect:/training/hse-external-training";
        }

        model.addAttribute("title", "HSE External Training Records");
        model.addAttribute("pageTitle", "HSE External Training Record Detail");
        model.addAttribute("training", training);

        return "external-training/detail";
    }

    @GetMapping("/certification")
    public String certification(Model model) {
        log.info("Viewing Certification page");

        try {
            // Fetch recent records (last 5)
            Pageable recentPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<HseCertification> recentRecords = certificationService.findAll(recentPageable);

            model.addAttribute("title", "Certification");
            model.addAttribute("pageTitle", "Certification Records");
            model.addAttribute("recentRecords", recentRecords.getContent());

            log.info("Certification dashboard loaded");
        } catch (Exception e) {
            log.error("Error loading Certification dashboard", e);
            model.addAttribute("recentRecords", java.util.Collections.emptyList());
        }

        return "training/certification";
    }

    @GetMapping("/hse-certification")
    public String hseCertification(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        log.info("Viewing Certification Record page - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<HseCertification> certificationPage = certificationService.findAll(pageable);

        model.addAttribute("title", "Certification");
        model.addAttribute("pageTitle", "Certification Records");
        model.addAttribute("certificationPage", certificationPage);
        model.addAttribute("pageSize", size);

        return "certification/index";
    }

    @GetMapping("/hse-certification/{code}")
    public String viewHseCertificationDetails(
            @PathVariable String code,
            Model model) {
        log.info("Viewing Certification Record details: {}", code);

        HseCertification certification;
        try {
            certification = certificationService.findByCode(code);
        } catch (ResourceNotFoundException e) {
            log.warn("Certification record not found: {}", code);
            return "redirect:/training/hse-certification";
        }

        model.addAttribute("title", "Certification");
        model.addAttribute("pageTitle", "Certification Record Detail");
        model.addAttribute("certification", certification);

        return "certification/detail";
    }

    @GetMapping("/induction")
    public String induction(Model model) {
        log.info("Viewing HSE Induction page");

        try {
            // Fetch recent records (last 5)
            Pageable recentPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<HseInduction> recentRecords = trainingService.findAll(recentPageable);

            model.addAttribute("title", "HSE Induction Records");
            model.addAttribute("pageTitle", "HSE Induction Records");
            model.addAttribute("recentRecords", recentRecords.getContent());

            log.info("HSE Induction dashboard loaded");
        } catch (Exception e) {
            log.error("Error loading HSE Induction dashboard", e);
            model.addAttribute("recentRecords", java.util.Collections.emptyList());
        }

        return "training/induction";
    }

    @GetMapping("/internal-training")
    public String internalTraining(Model model) {
        log.info("Viewing HSE Internal Training page");

        try {
            // Fetch recent records (last 5)
            Pageable recentPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<HseInternalTraining> recentRecords = internalTrainingService.findAll(recentPageable);

            model.addAttribute("title", "HSE Internal Training Records");
            model.addAttribute("pageTitle", "HSE Internal Training Records");
            model.addAttribute("recentRecords", recentRecords.getContent());

            log.info("HSE Internal Training dashboard loaded");
        } catch (Exception e) {
            log.error("Error loading HSE Internal Training dashboard", e);
            model.addAttribute("recentRecords", java.util.Collections.emptyList());
        }

        return "training/internal-training";
    }

    @GetMapping("/hse-induction")
    public String hseInduction(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        log.info("Viewing HSE Induction Record page - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<HseInduction> inductionPage = trainingService.findAll(pageable);

        model.addAttribute("title", "HSE Induction Records");
        model.addAttribute("pageTitle", "HSE Induction Records");
        model.addAttribute("inductionPage", inductionPage);
        model.addAttribute("pageSize", size);

        return "induction/index";
    }

    @GetMapping("/hse-induction/{code}")
    public String viewHseInductionDetails(
            @PathVariable String code,
            Model model) {
        log.info("Viewing HSE Induction Record details: {}", code);

        HseInduction induction = trainingService.findByCode(code);
        if (induction == null) {
            log.warn("HSE Induction record not found: {}", code);
            return "redirect:/training/hse-induction";
        }

        model.addAttribute("title", "HSE Induction Record");
        model.addAttribute("pageTitle", "HSE Induction Record Detail");
        model.addAttribute("induction", induction);

        return "induction/detail";
    }

    @GetMapping("/hse-internal-training")
    public String hseInternalTraining(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        log.info("Viewing HSE Internal Training Record page - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<HseInternalTraining> trainingPage = internalTrainingService.findAll(pageable);

        model.addAttribute("title", "HSE Internal Training Records");
        model.addAttribute("pageTitle", "HSE Internal Training Records");
        model.addAttribute("trainingPage", trainingPage);
        model.addAttribute("pageSize", size);

        return "internal-training/index";
    }

    @GetMapping("/hse-internal-training/{code}")
    public String viewHseInternalTrainingDetails(
            @PathVariable String code,
            Model model) {
        log.info("Viewing HSE Internal Training Record details: {}", code);

        HseInternalTraining training = internalTrainingService.findByCode(code);
        if (training == null) {
            log.warn("HSE Internal Training record not found: {}", code);
            return "redirect:/training/hse-internal-training";
        }

        model.addAttribute("title", "HSE Internal Training Record");
        model.addAttribute("pageTitle", "HSE Internal Training Record Detail");
        model.addAttribute("training", training);

        return "internal-training/detail";
    }
}
