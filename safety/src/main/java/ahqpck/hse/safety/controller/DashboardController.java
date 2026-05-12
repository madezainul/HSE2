package ahqpck.hse.safety.controller;

import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.service.IncidentService;
import ahqpck.hse.safety.service.ObservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/")
public class DashboardController {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private ObservationService observationService;

    @GetMapping
    public String index(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }
        
        model.addAttribute("pageTitle", "Dashboard");
        
        // Get the authenticated user object
        if (authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            model.addAttribute("currentUser", user);
            model.addAttribute("userUsername", user.getUsername());
            model.addAttribute("userFullName", user.getFullName());
            model.addAttribute("userEmail", user.getEmail());
        } else {
            // Fallback if principal is not User
            model.addAttribute("userUsername", authentication.getName());
        }

        // ── Stat Card Data ──────────────────────────────────────────────────
        long totalIncidents = incidentService.getTotalCount();
        long totalObservations = observationService.getTotalCount();

        long openIncidents = incidentService.countByStatus(Incident.IncidentStatus.REPORTED)
                + incidentService.countByStatus(Incident.IncidentStatus.UNDER_INVESTIGATION);
        long openObservations = observationService.countByStatus(Observation.Status.OPEN)
                + observationService.countByStatus(Observation.Status.MITIGATED);

        long closedIncidents = incidentService.countByStatus(Incident.IncidentStatus.CLOSED)
                + incidentService.countByStatus(Incident.IncidentStatus.RESOLVED);
        long closedObservations = observationService.countByStatus(Observation.Status.CLOSED);

        model.addAttribute("totalIncidents", totalIncidents);
        model.addAttribute("totalObservations", totalObservations);
        model.addAttribute("openReports", openIncidents + openObservations);
        model.addAttribute("closedReports", closedIncidents + closedObservations);

        // ── Chart Data (current year) ───────────────────────────────────────
        int year = LocalDate.now().getYear();

        model.addAttribute("incidentNearMissData",
                toJsonArray(incidentService.getMonthlyCountByType(year, Incident.Type.NEAR_MISS)));
        model.addAttribute("incidentAccidentData",
                toJsonArray(incidentService.getMonthlyCountByType(year, Incident.Type.ACCIDENT)));
        model.addAttribute("incidentUnsafeActData",
                toJsonArray(incidentService.getMonthlyCountByType(year, Incident.Type.UNSAFE_ACT)));

        model.addAttribute("obsOpenData",
                toJsonArray(observationService.getMonthlyCountByStatus(year, Observation.Status.OPEN)));
        model.addAttribute("obsMitigatedData",
                toJsonArray(observationService.getMonthlyCountByStatus(year, Observation.Status.MITIGATED)));
        model.addAttribute("obsClosedData",
                toJsonArray(observationService.getMonthlyCountByStatus(year, Observation.Status.CLOSED)));

        return "dashboard/index";
    }

    private String toJsonArray(int[] data) {
        return Arrays.stream(data)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }

    @GetMapping("/icon-menu")
    public String iconMenu(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }
        
        model.addAttribute("pageTitle", "Icon Menu");
        return "icon-menu";
    }
}
