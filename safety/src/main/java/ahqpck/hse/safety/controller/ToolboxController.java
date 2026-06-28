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
import ahqpck.hse.safety.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/training/toolbox")
@Slf4j
public class ToolboxController {

    @Autowired
    private ToolboxMeetingRepository toolboxRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String toolboxPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("meetingDate").descending());
        Page<ToolboxMeeting> toolboxPage = toolboxRepository.findAll(pageable);

        Map<String, String> supervisorNameMap = new HashMap<>();
        for (ToolboxMeeting m : toolboxPage.getContent()) {
            if ((m.getSupervisor() == null || m.getSupervisor().isBlank())
                    && m.getSupervisorEmployeeId() != null
                    && !supervisorNameMap.containsKey(m.getSupervisorEmployeeId())) {
                userRepository.findByEmployeeId(m.getSupervisorEmployeeId())
                        .ifPresent(u -> supervisorNameMap.put(m.getSupervisorEmployeeId(), u.getFullName()));
            }
        }

        model.addAttribute("title", "Toolbox Meeting");
        model.addAttribute("toolboxPage", toolboxPage);
        model.addAttribute("supervisorNameMap", supervisorNameMap);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", "meetingDate");
        model.addAttribute("asc", false);

        return "toolbox/index";
    }

    @GetMapping("/{code}")
    public String toolboxDetailPage(@PathVariable String code, Model model) {
        ToolboxMeeting meeting = toolboxRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Toolbox meeting not found: " + code));

        String supervisorDisplayName = meeting.getSupervisor();
        if ((supervisorDisplayName == null || supervisorDisplayName.isBlank())
                && meeting.getSupervisorEmployeeId() != null) {
            supervisorDisplayName = userRepository.findByEmployeeId(meeting.getSupervisorEmployeeId())
                    .map(u -> u.getFullName())
                    .orElse(null);
        }

        model.addAttribute("title", "Toolbox Meeting");
        model.addAttribute("meeting", meeting);
        model.addAttribute("supervisorDisplayName", supervisorDisplayName);
        return "toolbox/detail";
    }
}
