package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.dto.ToolboxSearchCriteria;
import ahqpck.hse.safety.model.entity.ToolboxMeeting;
import ahqpck.hse.safety.util.ListQuerySupport;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the database query for the toolbox meeting list filters.
 * Text search is case-insensitive and treats % and _ typed by the user as ordinary characters.
 */
public final class ToolboxSpecifications {

    private ToolboxSpecifications() {
    }

    public static Specification<ToolboxMeeting> matching(ToolboxSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.keyword() != null) {
                String pattern = ListQuerySupport.likePattern(criteria.keyword());
                predicates.add(cb.or(
                        ListQuerySupport.like(cb, root.get("code"), pattern),
                        ListQuerySupport.like(cb, root.get("location"), pattern),
                        ListQuerySupport.like(cb, root.get("notes"), pattern),
                        ListQuerySupport.like(cb, root.get("supervisor"), pattern),
                        ListQuerySupport.like(cb, root.get("supervisorEmployeeId"), pattern)));
            }

            // meetingDate is a plain date, so both ends are inclusive
            if (criteria.meetingDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("meetingDate"), criteria.meetingDateFrom()));
            }
            if (criteria.meetingDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("meetingDate"), criteria.meetingDateTo()));
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
