package com.glbajaj.campuscare.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class CommonDtos {
    private CommonDtos() {}

    /** Error body used for every failure: { success:false, message, timestamp, status }. */
    public record ErrorResponse(boolean success, String message, LocalDateTime timestamp, int status,
                                Map<String, String> errors) {}

    public record MessageResponse(String message) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    public record StatusRequest(@jakarta.validation.constraints.NotNull com.glbajaj.campuscare.entity.RecordStatus status) {}
}
