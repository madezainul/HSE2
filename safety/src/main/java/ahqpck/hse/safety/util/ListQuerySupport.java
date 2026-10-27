package ahqpck.hse.safety.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Small helpers shared by the list pages (Incident, Observation, Toolbox):
 * page limits, a safe sort whitelist, LIKE search that treats % and _ as ordinary text,
 * and lenient reading of filter values from the query string.
 */
public final class ListQuerySupport {

    public static final int DEFAULT_PAGE_SIZE = 10;
    /** Largest page a list can ask for (the page-size selector goes up to 500). */
    public static final int MAX_PAGE_SIZE = 500;

    private static final char LIKE_ESCAPE = '\\';

    private ListQuerySupport() {
    }

    // ── paging and sorting ─────────────────────────────────────────────

    public static int safePage(int page) {
        return Math.max(page, 0);
    }

    public static int safeSize(int size) {
        return size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * Turns the sort choice from the URL into a safe Sort.
     *
     * @param sortable        URL column name -> entity property (anything not listed is ignored)
     * @param defaultProperty property used, newest first, when the column is unknown
     */
    public static Sort resolveSort(Map<String, String> sortable, String sortBy, boolean asc, String defaultProperty) {
        String property = sortable.get(sortBy == null ? "" : sortBy);
        Sort primary = (property == null)
                ? Sort.by(Sort.Direction.DESC, defaultProperty)
                : Sort.by(asc ? Sort.Direction.ASC : Sort.Direction.DESC, property);
        // the id is a tie-breaker so paging stays stable when the sorted values repeat
        return primary.and(Sort.by("id"));
    }

    // ── text search ────────────────────────────────────────────────────

    /** "%text%" in lower case, with % _ and \ in the text escaped. */
    public static String likePattern(String keyword) {
        String escaped = keyword.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /** Case-insensitive LIKE for use with {@link #likePattern}. A null column never matches. */
    public static Predicate like(CriteriaBuilder cb, Expression<String> field, String pattern) {
        return cb.like(cb.lower(field), pattern, LIKE_ESCAPE);
    }

    // ── lenient filter values (a bad value means "no filter", never an error page) ──

    public static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    public static LocalDate parseDate(String value) {
        String text = blankToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        String text = blankToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, text.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
