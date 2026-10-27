package ahqpck.hse.safety.model.dto;

import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.util.ListQuerySupport;

import java.time.LocalDate;

/**
 * Filters for the incident list and its exports. Every field is optional (null = no filter).
 * <p>
 * Values come straight from the query string, so {@link #of} reads them leniently:
 * a blank, unknown or malformed value simply means "no filter" instead of an error page.
 */
public record IncidentSearchCriteria(
        String keyword,
        LocalDate reportDateFrom,
        LocalDate reportDateTo,
        Incident.Type type,
        Incident.SeverityLevel severity,
        Incident.IncidentStatus status,
        String areaCode) {

    public static IncidentSearchCriteria none() {
        return new IncidentSearchCriteria(null, null, null, null, null, null, null);
    }

    public static IncidentSearchCriteria of(String keyword, String reportDateFrom, String reportDateTo,
                                            String type, String severity, String status, String areaCode) {
        return new IncidentSearchCriteria(
                ListQuerySupport.blankToNull(keyword),
                ListQuerySupport.parseDate(reportDateFrom),
                ListQuerySupport.parseDate(reportDateTo),
                ListQuerySupport.parseEnum(Incident.Type.class, type),
                ListQuerySupport.parseEnum(Incident.SeverityLevel.class, severity),
                ListQuerySupport.parseEnum(Incident.IncidentStatus.class, status),
                ListQuerySupport.blankToNull(areaCode));
    }

    public boolean isEmpty() {
        return keyword == null && reportDateFrom == null && reportDateTo == null
                && type == null && severity == null && status == null && areaCode == null;
    }

}
