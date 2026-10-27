package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.dto.ObservationSearchCriteria;
import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.util.ListQuerySupport;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the database query for the observation list filters.
 * Text search is case-insensitive and treats % and _ typed by the user as ordinary characters.
 */
public final class ObservationSpecifications {

    private ObservationSpecifications() {
    }

    public static Specification<Observation> matching(ObservationSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.keyword() != null) {
                String pattern = ListQuerySupport.likePattern(criteria.keyword());
                Join<Observation, Area> area = root.join("area", JoinType.LEFT);
                predicates.add(cb.or(
                        ListQuerySupport.like(cb, root.get("code"), pattern),
                        ListQuerySupport.like(cb, root.get("description"), pattern),
                        ListQuerySupport.like(cb, root.get("observer"), pattern),
                        ListQuerySupport.like(cb, root.get("observerEmployeeId"), pattern),
                        ListQuerySupport.like(cb, root.get("responsible"), pattern),
                        ListQuerySupport.like(cb, root.get("responsibleEmployeeId"), pattern),
                        ListQuerySupport.like(cb, root.get("correctiveAction"), pattern),
                        ListQuerySupport.like(cb, root.get("remarks"), pattern),
                        ListQuerySupport.like(cb, area.get("name"), pattern)));
            }

            if (criteria.observationDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("observationDate"),
                        criteria.observationDateFrom().atStartOfDay()));
            }
            if (criteria.observationDateTo() != null) {
                // inclusive: the whole "to" day counts
                predicates.add(cb.lessThanOrEqualTo(root.get("observationDate"),
                        criteria.observationDateTo().atTime(LocalTime.MAX)));
            }

            if (criteria.category() != null) {
                predicates.add(cb.equal(root.get("category"), criteria.category()));
            }
            if (criteria.type() != null) {
                predicates.add(cb.equal(root.get("type"), criteria.type()));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.mitigation() != null) {
                predicates.add(cb.equal(root.get("mitigation"), criteria.mitigation()));
            }
            if (criteria.areaCode() != null) {
                Join<Observation, Area> area = root.join("area", JoinType.INNER);
                predicates.add(cb.equal(area.get("code"), criteria.areaCode()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
