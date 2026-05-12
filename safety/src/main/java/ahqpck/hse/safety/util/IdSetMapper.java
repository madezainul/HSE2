package ahqpck.hse.safety.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public final class IdSetMapper {

    private IdSetMapper() {}

    /**
     * Maps a set of string IDs to entities.
     * 
     * @param <T> entity type
     * @param ids set of IDs (e.g., ["EMP001", "EQ100"])
     * @param finder ID → Optional<T> (e.g., userRepository::findByEmployeeId)
     * @param entityName human-readable entity name (e.g., "User")
     * @param idLabel field name (e.g., "employeeId")
     * @return non-null, immutable set of resolved entities
     * @throws IllegalArgumentException if any ID is invalid or not found
     */
    public static <T> Set<T> map(
            Set<String> ids,
            Function<String, Optional<T>> finder,
            String entityName,
            String idLabel) {

        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }

        Set<String> validIds = new LinkedHashSet<>(); // preserve order, dedupe
        List<String> invalidIds = new ArrayList<>();

        // Phase 1: Validate & normalize IDs
        for (String id : ids) {
            if (id == null) {
                invalidIds.add("(null)");
                continue;
            }
            String trimmed = id.trim();
            if (trimmed.isEmpty()) {
                invalidIds.add("(empty)");
                continue;
            }
            validIds.add(trimmed.toUpperCase());
        }

        if (!invalidIds.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("%s has invalid %s(s): %s", 
                            entityName, idLabel, String.join(", ", invalidIds)));
        }

        // Phase 2: Batch lookup + collect missing
        Set<T> result = new LinkedHashSet<>(validIds.size());
        List<String> missing = new ArrayList<>();

        for (String id : validIds) {
            Optional<T> found = finder.apply(id);
            if (found.isPresent()) {
                result.add(found.get());
            } else {
                missing.add(id);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("%s not found by %s: %s", 
                            entityName, idLabel, String.join(", ", missing)));
        }

        return Set.copyOf(result); // immutable, thread-safe
    }
}
