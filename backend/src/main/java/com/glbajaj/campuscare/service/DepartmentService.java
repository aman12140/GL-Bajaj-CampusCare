package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.dto.AdminDtos.*;
import com.glbajaj.campuscare.entity.*;
import com.glbajaj.campuscare.exception.ApiException;
import com.glbajaj.campuscare.repository.DepartmentRepository;
import com.glbajaj.campuscare.util.EntityMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final EntityMapper mapper;

    public DepartmentService(DepartmentRepository departmentRepository, EntityMapper mapper) {
        this.departmentRepository = departmentRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> list(boolean activeOnly) {
        return departmentRepository.findAll(Sort.by("name")).stream()
                .filter(d -> !activeOnly || d.getStatus() == RecordStatus.ACTIVE).map(mapper::department).toList();
    }

    @Transactional
    public DepartmentDto create(DepartmentRequest req) {
        String name = req.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) throw ApiException.conflict("A department with this name already exists.");
        Department d = new Department();
        d.setName(name);
        d.setDescription(req.description());
        return mapper.department(departmentRepository.save(d));
    }

    @Transactional
    public DepartmentDto update(Long id, DepartmentRequest req) {
        Department d = find(id);
        String name = req.name().trim();
        departmentRepository.findByNameIgnoreCase(name).ifPresent(other -> {
            if (!other.getId().equals(id)) throw ApiException.conflict("A department with this name already exists.");
        });
        d.setName(name);
        d.setDescription(req.description());
        return mapper.department(d);
    }

    @Transactional
    public DepartmentDto setStatus(Long id, RecordStatus status) {
        Department d = find(id);
        d.setStatus(status);
        return mapper.department(d);
    }

    public Department find(Long id) {
        return departmentRepository.findById(id).orElseThrow(() -> ApiException.notFound("Department not found"));
    }
}
