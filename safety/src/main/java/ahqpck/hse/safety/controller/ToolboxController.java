package ahqpck.hse.safety.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import ahqpck.hse.safety.model.entity.ToolboxMeeting;
import ahqpck.hse.safety.repository.ToolboxMeetingRepository;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/toolbox")
@Slf4j
public class ToolboxController {

    @Autowired
    private ToolboxMeetingRepository toolboxRepository;

    @GetMapping
    public String toolboxPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("meetingDate").descending());
        Page<ToolboxMeeting> toolboxPage = toolboxRepository.findAll(pageable);

        model.addAttribute("toolboxPage", toolboxPage);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", "meetingDate");
        model.addAttribute("asc", false);

        return "toolbox/index";
    }

    @GetMapping("/{code}")
    public String toolboxDetailPage(@PathVariable String code, Model model) {
        ToolboxMeeting meeting = toolboxRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Toolbox meeting not found: " + code));
        model.addAttribute("meeting", meeting);
        return "toolbox/detail";
    }
}
