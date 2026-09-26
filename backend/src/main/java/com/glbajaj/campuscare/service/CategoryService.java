package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.dto.AdminDtos.*;
import com.glbajaj.campuscare.entity.*;
import com.glbajaj.campuscare.exception.ApiException;
import com.glbajaj.campuscare.repository.CategoryRepository;
import com.glbajaj.campuscare.repository.DepartmentRepository;
import com.glbajaj.campuscare.util.EntityMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final EntityMapper mapper;

    public CategoryService(CategoryRepository categoryRepository, DepartmentRepository departmentRepository, EntityMapper mapper) {
        this.categoryRepository = categoryRepository;
        this.departmentRepository = departmentRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> list(boolean activeOnly) {
        return categoryRepository.findAll(Sort.by("id")).stream()
                .filter(c -> !activeOnly || c.getStatus() == RecordStatus.ACTIVE).map(mapper::category).toList();
    }

    @Transactional
    public CategoryDto create(CategoryRequest req) {
        Category c = new Category();
        apply(c, req);
        return mapper.category(categoryRepository.save(c));
    }

    @Transactional
    public CategoryDto update(Long id, CategoryRequest req) {
        Category c = find(id);
        if (req.parentId() != null && req.parentId().equals(id)) throw ApiException.badRequest("A category cannot be its own parent.");
        apply(c, req);
        return mapper.category(c);
    }

    @Transactional
    public CategoryDto setStatus(Long id, RecordStatus status) {
        Category c = find(id);
        c.setStatus(status);
        if (status == RecordStatus.INACTIVE) {                       // deactivating a parent also hides its subcategories
            categoryRepository.findByParentId(id).forEach(child -> child.setStatus(RecordStatus.INACTIVE));
        }
        return mapper.category(c);
    }

    private void apply(Category c, CategoryRequest req) {
        c.setName(req.name().trim());
        c.setDescription(req.description());
        Category parent = null;
        if (req.parentId() != null) {
            parent = find(req.parentId());
            if (parent.getParent() != null) throw ApiException.badRequest("Only one level of subcategories is supported.");
        }
        c.setParent(parent);
        c.setDepartment(req.departmentId() == null ? null : departmentRepository.findById(req.departmentId())
                .orElseThrow(() -> ApiException.notFound("Department not found")));
    }

    /** The category id plus the ids of its subcategories (used by the category filter). */
    @Transactional(readOnly = true)
    public Set<Long> idsWithChildren(Long categoryId) {
        Set<Long> ids = new HashSet<>();
        ids.add(categoryId);
        categoryRepository.findByParentId(categoryId).forEach(c -> ids.add(c.getId()));
        return ids;
    }

    public Category find(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> ApiException.notFound("Category not found"));
    }
}
