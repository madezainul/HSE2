package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.dto.IncidentSearchCriteria;
import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.model.entity.Incident;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import ahqpck.hse.safety.util.ListQuerySupport;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the database query for the incident list filters.
 * Text search is case-insensitive and treats % and _ typed by the user as ordinary characters.
 */
public final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    public static Specification<Incident> matching(IncidentSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.keyword() != null) {
                String pattern = ListQuerySupport.likePattern(criteria.keyword());
                Join<Incident, Area> area = root.join("area", JoinType.LEFT);
                predicates.add(cb.or(
                        ListQuerySupport.like(cb, root.get("code"), pattern),
                        ListQuerySupport.like(cb, root.get("description"), pattern),
                        ListQuerySupport.like(cb, root.get("immediateAction"), pattern),
                        ListQuerySupport.like(cb, root.get("correctiveAction"), pattern),
                        ListQuerySupport.like(cb, root.get("involvedPersonName"), pattern),
                        ListQuerySupport.like(cb, root.get("involvedPersonEmployeeId"), pattern),
                        ListQuerySupport.like(cb, root.get("witnesses"), pattern),
                        ListQuerySupport.like(cb, root.get("reportedBy"), pattern),
                        ListQuerySupport.like(cb, area.get("name"), pattern)));
            }

            if (criteria.reportDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("reportDate"), criteria.reportDateFrom().atStartOfDay()));
            }
            if (criteria.reportDateTo() != null) {
                // inclusive: the whole "to" day counts
                predicates.add(cb.lessThanOrEqualTo(root.get("reportDate"), criteria.reportDateTo().atTime(LocalTime.MAX)));
            }

            if (criteria.type() != null) {
                predicates.add(cb.equal(root.get("type"), criteria.type()));
            }
            if (criteria.severity() != null) {
                predicates.add(cb.equal(root.get("severity"), criteria.severity()));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.areaCode() != null) {
                Join<Incident, Area> area = root.join("area", JoinType.INNER);
                predicates.add(cb.equal(area.get("code"), criteria.areaCode()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
