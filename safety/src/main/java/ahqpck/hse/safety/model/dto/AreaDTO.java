package ahqpck.hse.safety.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ahqpck.hse.safety.model.entity.Area;

/**
 * Data Transfer Object for Area entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AreaDTO {

    private String id;
    private String code;
    private String name;
    private String description;
    private String status;

    /**
     * Convert AreaDTO to Area entity
     */
    public Area toEntity() {
        return Area.builder()
                .id(this.id)
                .code(this.code)
                .name(this.name)
                .description(this.description)
                .status(Area.AreaStatus.valueOf(this.status != null ? this.status : "ACTIVE"))
                .build();
    }

    /**
     * Convert Area entity to AreaDTO
     */
    public static AreaDTO fromEntity(Area area) {
        if (area == null) {
            return null;
        }
        return AreaDTO.builder()
                .id(area.getId())
                .code(area.getCode())
                .name(area.getName())
                .description(area.getDescription())
                .status(area.getStatus() != null ? area.getStatus().toString() : "ACTIVE")
                .build();
    }
}
