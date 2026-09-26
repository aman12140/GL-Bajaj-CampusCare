package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * Local image storage (backend/uploads/issues and backend/uploads/resolutions).
 *
 * Safety rules: only JPG/PNG, max 5 MB, the declared MIME type AND the real file signature ("magic bytes") must match,
 * and the original file name is NEVER used - we generate a random UUID name.
 */
@Service
public class FileStorageService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/jpg", "image/png");
    private static final String ERROR = "Only JPG, JPEG or PNG images up to 5 MB are allowed.";

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root.resolve("issues"));
        Files.createDirectories(root.resolve("resolutions"));
    }

    /** @param subDir "issues" or "resolutions". Returns the public URL path, or null when no file was sent. */
    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) return null;
        if (!subDir.equals("issues") && !subDir.equals("resolutions")) throw new IllegalArgumentException("bad sub directory");
        if (file.getSize() > MAX_BYTES) throw ApiException.badRequest(ERROR);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED_TYPES.contains(contentType)) throw ApiException.badRequest(ERROR);
        try {
            byte[] data = file.getBytes();
            String ext = detectExtension(data);
            if (ext == null) throw ApiException.badRequest(ERROR);
            String name = UUID.randomUUID() + "." + ext;
            Files.write(root.resolve(subDir).resolve(name), data);
            return "/uploads/" + subDir + "/" + name;
        } catch (IOException e) {
            throw ApiException.badRequest("Could not save the uploaded file. Please try again.");
        }
    }

    private String detectExtension(byte[] d) {
        if (d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) return "jpg";
        if (d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') return "png";
        return null;
    }
}
