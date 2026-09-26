package com.glbajaj.campuscare.controller;

import com.glbajaj.campuscare.dto.AdminDtos.*;
import com.glbajaj.campuscare.dto.CommonDtos.StatusRequest;
import com.glbajaj.campuscare.service.CategoryService;
import com.glbajaj.campuscare.service.DepartmentService;
import com.glbajaj.campuscare.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Departments, categories, locations and location types.
 * Every logged-in user may read (GET); only ADMIN may create/change (see SecurityConfig).
 */
@RestController
@RequestMapping("/api")
public class MasterDataController {
    private final DepartmentService departmentService;
    private final CategoryService categoryService;
    private final LocationService locationService;

    public MasterDataController(DepartmentService departmentService, CategoryService categoryService, LocationService locationService) {
        this.departmentService = departmentService;
        this.categoryService = categoryService;
        this.locationService = locationService;
    }

    // ---- departments ----
    @GetMapping("/departments")
    public List<DepartmentDto> departments(@RequestParam(defaultValue = "false") boolean activeOnly) { return departmentService.list(activeOnly); }

    @PostMapping("/departments")
    public ResponseEntity<DepartmentDto> createDepartment(@Valid @RequestBody DepartmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.create(req));
    }

    @PutMapping("/departments/{id}")
    public DepartmentDto updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentRequest req) { return departmentService.update(id, req); }

    @PutMapping("/departments/{id}/status")
    public DepartmentDto departmentStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) { return departmentService.setStatus(id, req.status()); }

    // ---- categories ----
    @GetMapping("/categories")
    public List<CategoryDto> categories(@RequestParam(defaultValue = "false") boolean activeOnly) { return categoryService.list(activeOnly); }

    @PostMapping("/categories")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(req));
    }

    @PutMapping("/categories/{id}")
    public CategoryDto updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) { return categoryService.update(id, req); }

    @PutMapping("/categories/{id}/status")
    public CategoryDto categoryStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) { return categoryService.setStatus(id, req.status()); }

    // ---- locations ----
    @GetMapping("/locations")
    public List<LocationDto> locations(@RequestParam(defaultValue = "false") boolean activeOnly) { return locationService.list(activeOnly); }

    @PostMapping("/locations")
    public ResponseEntity<LocationDto> createLocation(@Valid @RequestBody LocationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.create(req));
    }

    @PutMapping("/locations/{id}")
    public LocationDto updateLocation(@PathVariable Long id, @Valid @RequestBody LocationRequest req) { return locationService.update(id, req); }

    @PutMapping("/locations/{id}/status")
    public LocationDto locationStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) { return locationService.setStatus(id, req.status()); }

    // ---- location types ----
    @GetMapping("/location-types")
    public List<LocationTypeDto> locationTypes(@RequestParam(defaultValue = "false") boolean activeOnly) { return locationService.listTypes(activeOnly); }

    @PostMapping("/location-types")
    public ResponseEntity<LocationTypeDto> createLocationType(@Valid @RequestBody LocationTypeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.createType(req));
    }

    @PutMapping("/location-types/{id}/status")
    public LocationTypeDto locationTypeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) { return locationService.setTypeStatus(id, req.status()); }
}
