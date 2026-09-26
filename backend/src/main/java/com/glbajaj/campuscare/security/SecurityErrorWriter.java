package com.glbajaj.campuscare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glbajaj.campuscare.dto.CommonDtos.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/** Writes the standard JSON error body for failures that happen inside the security filter chain. */
@Component
public class SecurityErrorWriter {
    private final ObjectMapper mapper;

    public SecurityErrorWriter(ObjectMapper mapper) { this.mapper = mapper; }

    public void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), new ErrorResponse(false, message, LocalDateTime.now(), status, null));
    }
}
