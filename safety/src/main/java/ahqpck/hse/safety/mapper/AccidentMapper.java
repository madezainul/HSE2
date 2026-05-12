package ahqpck.hse.safety.mapper;

import ahqpck.hse.safety.model.dto.AccidentDTO;
import ahqpck.hse.safety.model.dto.AreaDTO;
import ahqpck.hse.safety.model.entity.Accident;
import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.repository.AreaRepository;
import ahqpck.hse.safety.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Accident entity to/from AccidentDTO
 * Follows the pattern established in Complaint service for consistency
 */
@Component
@RequiredArgsConstructor
public class AccidentMapper {

    /**
     * Convert Accident entity to AccidentDTO
     * Maps all fields including user and area relationships
     */
    public AccidentDTO toDTO(Accident accident) {
        AccidentDTO dto = new AccidentDTO();

        // Mandatory fields
        dto.setAccidentCode(accident.getCode());
        dto.setAccidentDate(accident.getAccidentDate());
        dto.setDescription(accident.getDescription());
        dto.setSeverity(accident.getSeverity() != null ? accident.getSeverity().toString() : null);
        dto.setStatus(accident.getStatus() != null ? accident.getStatus().toString() : null);
        dto.setCreatedAt(accident.getCreatedAt());
        dto.setUpdatedAt(accident.getUpdatedAt());

        // Affected person details
        dto.setAffectedPersonName(accident.getAffectedPersonName());
        dto.setAffectedPersonEmployeeId(accident.getAffectedPersonEmployeeId());
        dto.setAffectedPersonPosition(accident.getAffectedPersonPosition());

        // Incident details
        dto.setInjuryType(accident.getInjuryType());
        dto.setMedicalAttentionRequired(accident.getMedicalAttentionRequired());
        dto.setCauseOfAccident(accident.getCauseOfAccident());
        dto.setWitnesses(accident.getWitnesses());

        // Image reference
        dto.setAccidentImages(accident.getAccidentImages());

        // Reported by
        dto.setReportedBy(accident.getReportedBy());

        // Area mapping
        if (accident.getArea() != null) {
            AreaDTO areaDTO = new AreaDTO();
            areaDTO.setId(accident.getArea().getId());
            areaDTO.setCode(accident.getArea().getCode());
            areaDTO.setName(accident.getArea().getName());
            areaDTO.setDescription(accident.getArea().getDescription());
            areaDTO.setStatus(accident.getArea().getStatus() != null ? accident.getArea().getStatus().toString() : null);
            dto.setArea(areaDTO);
        }

        return dto;
    }

    /**
     * Convert AccidentDTO to Accident entity
     * Maps all fields including looking up referenced entities from repositories
     */
    public void mapToEntity(Accident accident, AccidentDTO dto,
            AreaRepository areaRepository, UserRepository userRepository) {

        // Mandatory fields
        accident.setAccidentDate(dto.getAccidentDate());
        accident.setDescription(dto.getDescription());
        accident.setSeverity(Accident.SeverityLevel.valueOf(dto.getSeverity() != null ? dto.getSeverity() : "MEDIUM"));
        accident.setStatus(Accident.AccidentStatus.valueOf(dto.getStatus() != null ? dto.getStatus() : "REPORTED"));

        // Affected person details
        accident.setAffectedPersonName(dto.getAffectedPersonName());
        accident.setAffectedPersonEmployeeId(dto.getAffectedPersonEmployeeId());
        accident.setAffectedPersonPosition(dto.getAffectedPersonPosition());

        // Incident details
        accident.setInjuryType(dto.getInjuryType());
        accident.setMedicalAttentionRequired(
            dto.getMedicalAttentionRequired() != null ? dto.getMedicalAttentionRequired() : false
        );
        accident.setCauseOfAccident(dto.getCauseOfAccident());
        accident.setWitnesses(dto.getWitnesses());

        // Image reference (set from uploaded file stored by controller)
        accident.setAccidentImages(dto.getAccidentImages());

        // Map area by code
        if (dto.getArea() != null && dto.getArea().getCode() != null && !dto.getArea().getCode().trim().isEmpty()) {
            String areaCode = dto.getArea().getCode().trim();
            Area area = areaRepository.findByCode(areaCode);
            if (area == null) {
                throw new IllegalArgumentException("Area not found with code: " + areaCode);
            }
            accident.setArea(area);
        } else {
            accident.setArea(null);
        }

        // Map affected person details and other fields as needed
        if (dto.getAffectedPersonName() == null && dto.getAffectedPersonEmployeeId() != null) {
            accident.setAffectedPersonName("Employee ID: " + dto.getAffectedPersonEmployeeId());
        } else {
            accident.setAffectedPersonName(null);
        }

        // Map witnesses
        if (dto.getWitnesses() != null && !dto.getWitnesses().trim().isEmpty()) {
            accident.setWitnesses(dto.getWitnesses());
        } else {
            accident.setWitnesses(null);
        }

        // Note: reportedBy should be set from the authenticated user context in the service/controller
        // and not from the DTO for security reasons
    }
}
