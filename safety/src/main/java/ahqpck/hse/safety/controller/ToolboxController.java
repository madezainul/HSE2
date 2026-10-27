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

import ahqpck.hse.safety.model.dto.ToolboxSearchCriteria;
import ahqpck.hse.safety.model.entity.ToolboxMeeting;
import ahqpck.hse.safety.repository.ToolboxMeetingRepository;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.service.ToolboxMeetingService;
import ahqpck.hse.safety.util.ListQuerySupport;
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

    @Autowired
    private ToolboxMeetingService toolboxService;

    @GetMapping
    public String toolboxPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String meetingDateFrom,
            @RequestParam(defaultValue = "") String meetingDateTo,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "meetingDate") String sortBy,
            @RequestParam(defaultValue = "false") String asc,
            @RequestParam(defaultValue = "") String hiddenColumns,
            Model model) {

        ToolboxSearchCriteria criteria = ToolboxSearchCriteria.of(keyword, meetingDateFrom, meetingDateTo, status);
        boolean ascending = "true".equalsIgnoreCase(asc);

        Page<ToolboxMeeting> toolboxPage = toolboxService.search(criteria, page, size, sortBy, ascending);

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
        model.addAttribute("pageSize", ListQuerySupport.safeSize(size));
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("asc", ascending);

        // Echoed back so the filter form keeps its values and the page links keep the filters
        model.addAttribute("keyword", keyword);
        model.addAttribute("meetingDateFrom", meetingDateFrom);
        model.addAttribute("meetingDateTo", meetingDateTo);
        model.addAttribute("status", status);
        model.addAttribute("hiddenColumns", hiddenColumns);

        // Choices for the status drop-down
        model.addAttribute("statuses", ToolboxMeeting.MeetingStatus.values());

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
