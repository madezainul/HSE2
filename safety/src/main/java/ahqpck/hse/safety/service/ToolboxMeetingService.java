package ahqpck.hse.safety.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ahqpck.hse.safety.model.dto.ToolboxMeetingDTO;
import ahqpck.hse.safety.model.dto.ToolboxMeetingDTO.ToolboxMeetingFileDTO;
import ahqpck.hse.safety.model.dto.ToolboxSearchCriteria;
import ahqpck.hse.safety.model.entity.ToolboxMeeting;
import ahqpck.hse.safety.model.entity.ToolboxMeetingFile;
import ahqpck.hse.safety.repository.ToolboxMeetingFileRepository;
import ahqpck.hse.safety.repository.ToolboxMeetingRepository;
import ahqpck.hse.safety.util.ListQuerySupport;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ToolboxMeetingService {

    @Autowired
    private ToolboxMeetingRepository toolboxRepo;

    @Autowired
    private ToolboxMeetingFileRepository toolboxFileRepo;

    /** Columns the list may be sorted by: URL name -> entity property. Anything else is ignored. */
    private static final java.util.Map<String, String> SORTABLE_FIELDS = java.util.Map.ofEntries(
            java.util.Map.entry("code", "code"),
            java.util.Map.entry("location", "location"),
            java.util.Map.entry("meetingDate", "meetingDate"),
            java.util.Map.entry("timeStarted", "timeStarted"),
            java.util.Map.entry("timeEnd", "timeEnd"),
            java.util.Map.entry("supervisor", "supervisor"),
            java.util.Map.entry("supervisorEmployeeId", "supervisorEmployeeId"),
            java.util.Map.entry("status", "status"),
            java.util.Map.entry("createdAt", "createdAt"),
            java.util.Map.entry("updatedAt", "updatedAt"));

    /**
     * Turns the sort choice from the URL into a safe Sort. An unknown column falls back to
     * "newest meeting first"; the id is added as a tie-breaker so paging stays stable.
     */
    public static org.springframework.data.domain.Sort resolveSort(String sortBy, boolean asc) {
        return ListQuerySupport.resolveSort(SORTABLE_FIELDS, sortBy, asc, "meetingDate");
    }

    /**
     * One page of toolbox meetings for the list, filtered and sorted.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ToolboxMeeting> search(ToolboxSearchCriteria criteria,
                                                                       int page, int size, String sortBy, boolean asc) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                ListQuerySupport.safePage(page), ListQuerySupport.safeSize(size), resolveSort(sortBy, asc));
        return toolboxRepo.findAll(ToolboxSpecifications.matching(criteria), pageable);
    }

    @Transactional
    public ToolboxMeeting save(ToolboxMeetingDTO dto) {
        ToolboxMeeting meeting = dto.toEntity();

        if (meeting.getCode() == null || meeting.getCode().isEmpty()) {
            meeting.setCode(generateCode());
        }

        return toolboxRepo.save(meeting);
    }

    @Transactional
    public ToolboxMeeting update(String code, ToolboxMeetingDTO dto) {
        ToolboxMeeting meeting = toolboxRepo.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Toolbox meeting not found: " + code));

        if (dto.getLocation() != null)           meeting.setLocation(dto.getLocation());
        if (dto.getMeetingDate() != null)         meeting.setMeetingDate(dto.getMeetingDate());
        if (dto.getTimeStarted() != null)         meeting.setTimeStarted(dto.getTimeStarted());
        if (dto.getTimeEnd() != null)             meeting.setTimeEnd(dto.getTimeEnd());
        if (dto.getSupervisor() != null)          meeting.setSupervisor(dto.getSupervisor());
        if (dto.getSupervisorEmployeeId() != null) meeting.setSupervisorEmployeeId(dto.getSupervisorEmployeeId());
        if (dto.getNotes() != null)               meeting.setNotes(dto.getNotes());
        if (dto.getStatus() != null) {
            try {
                meeting.setStatus(ToolboxMeeting.MeetingStatus.valueOf(dto.getStatus().toUpperCase()));
            } catch (IllegalArgumentException ignored) { }
        }

        return toolboxRepo.save(meeting);
    }

    public ToolboxMeeting findByCode(String code) {
        return toolboxRepo.findByCode(code).orElse(null);
    }

    @Transactional
    public ToolboxMeetingFileDTO saveFile(String meetingCode, MultipartFile file, String fileType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        ToolboxMeeting meeting = toolboxRepo.findByCode(meetingCode)
                .orElseThrow(() -> new IllegalArgumentException("Toolbox meeting not found: " + meetingCode));

        String fileDir = "DOCUMENT".equalsIgnoreCase(fileType)
                ? "toolbox-meeting/documents"
                : "toolbox-meeting/images";

        Path uploadPath = Paths.get("uploads/" + fileDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String originalFileName = file.getOriginalFilename();
        String fileName = System.currentTimeMillis() + "_" + originalFileName;
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        ToolboxMeetingFile meetingFile = ToolboxMeetingFile.builder()
                .fileName(originalFileName)
                .filePath(fileDir + "/" + fileName)
                .fileType(ToolboxMeetingFile.FileType.valueOf(fileType.toUpperCase()))
                .uploadedAt(LocalDateTime.now())
                .toolboxMeeting(meeting)
                .build();

        meetingFile = toolboxFileRepo.save(meetingFile);
        return ToolboxMeetingFileDTO.fromEntity(meetingFile);
    }

    /**
     * Generate a unique code in the format TBX-000001, TBX-000002, etc.
     */
    private String generateCode() {
        long maxNumber = 0;
        int attempts = 0;

        while (attempts < 10) {
            try {
                for (ToolboxMeeting m : toolboxRepo.findAll()) {
                    if (m.getCode() != null && m.getCode().startsWith("TBX-")) {
                        try {
                            long number = Long.parseLong(m.getCode().substring(4));
                            if (number > maxNumber) maxNumber = number;
                        } catch (NumberFormatException ignored) { }
                    }
                }

                String newCode = String.format("TBX-%06d", maxNumber + 1);
                if (!toolboxRepo.existsByCode(newCode)) {
                    log.debug("Generated toolbox meeting code: {}", newCode);
                    return newCode;
                }
                maxNumber++;
            } catch (Exception e) {
                log.warn("Error generating toolbox code on attempt {}: {}", attempts, e.getMessage());
            }
            attempts++;
        }

        String fallback = "TBX-" + System.currentTimeMillis();
        log.warn("Falling back to timestamp-based toolbox code: {}", fallback);
        return fallback;
    }
}
