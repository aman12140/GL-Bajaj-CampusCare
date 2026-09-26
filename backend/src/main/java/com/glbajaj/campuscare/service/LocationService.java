package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.dto.AdminDtos.*;
import com.glbajaj.campuscare.entity.*;
import com.glbajaj.campuscare.exception.ApiException;
import com.glbajaj.campuscare.repository.LocationRepository;
import com.glbajaj.campuscare.repository.LocationTypeRepository;
import com.glbajaj.campuscare.util.EntityMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Database-driven, hierarchical campus locations (BUILDING -> FLOOR -> ROOM) plus configurable location types. */
@Service
public class LocationService {
    private final LocationRepository locationRepository;
    private final LocationTypeRepository typeRepository;
    private final EntityMapper mapper;

    public LocationService(LocationRepository locationRepository, LocationTypeRepository typeRepository, EntityMapper mapper) {
        this.locationRepository = locationRepository;
        this.typeRepository = typeRepository;
        this.mapper = mapper;
    }

    // ---------- location types ----------
    @Transactional(readOnly = true)
    public List<LocationTypeDto> listTypes(boolean activeOnly) {
        return typeRepository.findAll(Sort.by("name")).stream()
                .filter(t -> !activeOnly || t.getStatus() == RecordStatus.ACTIVE)
                .map(t -> new LocationTypeDto(t.getId(), t.getName(), t.getStatus())).toList();
    }

    @Transactional
    public LocationTypeDto createType(LocationTypeRequest req) {
        String name = req.name().trim();
        if (typeRepository.existsByNameIgnoreCase(name)) throw ApiException.conflict("This location type already exists.");
        LocationType t = new LocationType();
        t.setName(name);
        t = typeRepository.save(t);
        return new LocationTypeDto(t.getId(), t.getName(), t.getStatus());
    }

    @Transactional
    public LocationTypeDto setTypeStatus(Long id, RecordStatus status) {
        LocationType t = typeRepository.findById(id).orElseThrow(() -> ApiException.notFound("Location type not found"));
        t.setStatus(status);
        return new LocationTypeDto(t.getId(), t.getName(), t.getStatus());
    }

    // ---------- locations ----------
    @Transactional(readOnly = true)
    public List<LocationDto> list(boolean activeOnly) {
        return locationRepository.findAll(Sort.by("id")).stream()
                .filter(l -> !activeOnly || l.getStatus() == RecordStatus.ACTIVE).map(mapper::location).toList();
    }

    @Transactional
    public LocationDto create(LocationRequest req) {
        Location l = new Location();
        apply(l, req);
        return mapper.location(locationRepository.save(l));
    }

    @Transactional
    public LocationDto update(Long id, LocationRequest req) {
        Location l = find(id);
        if (req.parentId() != null && descendantIds(id).contains(req.parentId())) {
            throw ApiException.badRequest("A location cannot be moved under itself.");
        }
        apply(l, req);
        return mapper.location(l);
    }

    /** Deactivating a location also deactivates everything inside it (floors, rooms). Activating affects only that node. */
    @Transactional
    public LocationDto setStatus(Long id, RecordStatus status) {
        Location l = find(id);
        l.setStatus(status);
        if (status == RecordStatus.INACTIVE) {
            for (Long childId : descendantIds(id)) locationRepository.findById(childId).ifPresent(c -> c.setStatus(RecordStatus.INACTIVE));
        }
        return mapper.location(l);
    }

    private void apply(Location l, LocationRequest req) {
        Location parent = null;
        if (req.parentId() != null) parent = find(req.parentId());
        switch (req.level()) {
            case BUILDING -> {
                if (parent != null) throw ApiException.badRequest("A building/block cannot have a parent.");
                if (req.type() == null || req.type().isBlank()) throw ApiException.badRequest("Location type is required for a building/block.");
                LocationType t = typeRepository.findByNameIgnoreCase(req.type().trim())
                        .orElseThrow(() -> ApiException.badRequest("Unknown location type. Create it first."));
                l.setType(t.getName());
            }
            case FLOOR -> {
                if (parent == null || parent.getLevel() != LocationLevel.BUILDING) throw ApiException.badRequest("A floor must be placed inside a building/block.");
                l.setType(parent.getType());
            }
            case ROOM -> {
                if (parent == null || parent.getLevel() == LocationLevel.ROOM) throw ApiException.badRequest("A room/area must be placed inside a building or a floor.");
                l.setType(parent.getType());
            }
        }
        l.setName(req.name().trim());
        l.setLevel(req.level());
        l.setParent(parent);
        if (req.level() == LocationLevel.FLOOR) l.setFloor(req.floor() == null || req.floor().isBlank() ? req.name().trim() : req.floor().trim());
        else if (req.level() == LocationLevel.ROOM && parent.getLevel() == LocationLevel.FLOOR) l.setFloor(parent.getFloor());
        else l.setFloor(req.floor());
    }

    /** All ids below a node (children, grand-children ...). Used for "filter by location" and for cascading deactivation. */
    @Transactional(readOnly = true)
    public Set<Long> descendantIds(Long rootId) {
        Map<Long, List<Long>> children = new HashMap<>();
        for (Location l : locationRepository.findAll()) {
            if (l.getParent() != null) children.computeIfAbsent(l.getParent().getId(), k -> new ArrayList<>()).add(l.getId());
        }
        Set<Long> result = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>(children.getOrDefault(rootId, List.of()));
        while (!stack.isEmpty()) {
            Long id = stack.pop();
            if (result.add(id)) stack.addAll(children.getOrDefault(id, List.of()));
        }
        return result;
    }

    /** The node itself plus all descendants. */
    @Transactional(readOnly = true)
    public Set<Long> idsWithDescendants(Long rootId) {
        Set<Long> ids = new HashSet<>(descendantIds(rootId));
        ids.add(rootId);
        return ids;
    }

    public Location find(Long id) {
        return locationRepository.findById(id).orElseThrow(() -> ApiException.notFound("Location not found"));
    }
}
