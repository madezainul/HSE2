package ahqpck.hse.safety.model.dto;

import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.util.ListQuerySupport;

import java.time.LocalDate;

/**
 * Filters for the observation list and its exports. Every field is optional (null = no filter).
 * <p>
 * Values come straight from the query string, so {@link #of} reads them leniently:
 * a blank, unknown or malformed value simply means "no filter" instead of an error page.
 */
public record ObservationSearchCriteria(
        String keyword,
        LocalDate observationDateFrom,
        LocalDate observationDateTo,
        Observation.Category category,
        Observation.Type type,
        Observation.Status status,
        Observation.Mitigation mitigation,
        String areaCode) {

    public static ObservationSearchCriteria none() {
        return new ObservationSearchCriteria(null, null, null, null, null, null, null, null);
    }

    public static ObservationSearchCriteria of(String keyword, String observationDateFrom, String observationDateTo,
                                               String category, String type, String status,
                                               String mitigation, String areaCode) {
        return new ObservationSearchCriteria(
                ListQuerySupport.blankToNull(keyword),
                ListQuerySupport.parseDate(observationDateFrom),
                ListQuerySupport.parseDate(observationDateTo),
                ListQuerySupport.parseEnum(Observation.Category.class, category),
                ListQuerySupport.parseEnum(Observation.Type.class, type),
                ListQuerySupport.parseEnum(Observation.Status.class, status),
                ListQuerySupport.parseEnum(Observation.Mitigation.class, mitigation),
                ListQuerySupport.blankToNull(areaCode));
    }

    public boolean isEmpty() {
        return keyword == null && observationDateFrom == null && observationDateTo == null
                && category == null && type == null && status == null && mitigation == null && areaCode == null;
    }
}
