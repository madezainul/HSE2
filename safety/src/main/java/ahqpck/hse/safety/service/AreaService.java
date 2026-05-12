package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.model.dto.AreaDTO;
import ahqpck.hse.safety.repository.AreaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AreaService {

    private final AreaRepository areaRepository;

    public List<Area> getAllAreas() {
        log.info("Fetching all areas");
        return areaRepository.findAll();
    }

    /**
     * Get all areas with optional filtering and sorting
     */
    public List<AreaDTO> getAllAreas(String keyword, String sortBy, boolean asc) {
        log.info("Fetching all areas with keyword: {}, sortBy: {}, asc: {}", 
                 keyword, sortBy, asc);
        
        Sort.Direction direction = asc ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        
        List<Area> areas;
        if (keyword != null && !keyword.trim().isEmpty()) {
            areas = areaRepository.findAll(sort);
            areas = areas.stream()
                    .filter(a -> a.getCode().toLowerCase().contains(keyword.toLowerCase()) ||
                                a.getName().toLowerCase().contains(keyword.toLowerCase()))
                    .collect(Collectors.toList());
        } else {
            areas = areaRepository.findAll(sort);
        }
        
        return areas.stream()
                .map(AreaDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<Area> getActiveAreas() {
        log.info("Fetching active areas");
        return areaRepository.findByStatus(Area.AreaStatus.ACTIVE);
    }

    public long getTotalAreaCount() {
        log.info("Getting total area count");
        return areaRepository.count();
    }

    public Area getAreaById(String id) {
        log.info("Fetching area with id: {}", id);
        return areaRepository.findById(id).orElse(null);
    }

    public Area createArea(Area area) {
        log.info("Creating new area with code: {}", area.getCode());
        return areaRepository.save(area);
    }

    public Area updateArea(String id, Area areaDetails) {
        log.info("Updating area with id: {}", id);
        Area area = areaRepository.findById(id).orElse(null);
        if (area != null) {
            area.setCode(areaDetails.getCode());
            area.setName(areaDetails.getName());
            area.setDescription(areaDetails.getDescription());
            area.setStatus(areaDetails.getStatus());
            return areaRepository.save(area);
        }
        return null;
    }
    
    public Area getAreaByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        log.info("Fetching area with code: {}", code);
        return areaRepository.findByCode(code.trim());
    }

    public void deleteArea(String id) {
        log.info("Deleting area with id: {}", id);
        areaRepository.deleteById(id);
    }
}
