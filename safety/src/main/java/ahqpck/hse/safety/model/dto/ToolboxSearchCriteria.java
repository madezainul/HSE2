package ahqpck.hse.safety.model.dto;

import ahqpck.hse.safety.model.entity.ToolboxMeeting;
import ahqpck.hse.safety.util.ListQuerySupport;

import java.time.LocalDate;

/**
 * Filters for the toolbox meeting list. Every field is optional (null = no filter).
 * <p>
 * Values come straight from the query string, so {@link #of} reads them leniently:
 * a blank, unknown or malformed value simply means "no filter" instead of an error page.
 */
public record ToolboxSearchCriteria(
        String keyword,
        LocalDate meetingDateFrom,
        LocalDate meetingDateTo,
        ToolboxMeeting.MeetingStatus status) {

    public static ToolboxSearchCriteria none() {
        return new ToolboxSearchCriteria(null, null, null, null);
    }

    public static ToolboxSearchCriteria of(String keyword, String meetingDateFrom, String meetingDateTo, String status) {
        return new ToolboxSearchCriteria(
                ListQuerySupport.blankToNull(keyword),
                ListQuerySupport.parseDate(meetingDateFrom),
                ListQuerySupport.parseDate(meetingDateTo),
                ListQuerySupport.parseEnum(ToolboxMeeting.MeetingStatus.class, status));
    }

    public boolean isEmpty() {
        return keyword == null && meetingDateFrom == null && meetingDateTo == null && status == null;
    }
}
