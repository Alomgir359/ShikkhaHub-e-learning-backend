package com.lms.lms_backend.controller;

import com.lms.lms_backend.service.ClassroomException;
import com.lms.lms_backend.service.ClassroomStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

/**
 * Streams recorded videos (HTTP Range supported, so the player can seek), thumbnails,
 * assignment attachments and student submissions.
 */
@RestController
@RequestMapping("/api/classroom/files")
@CrossOrigin(origins = {"http://localhost:3000", "https://shikkha-hub-e-learning-platform.vercel.app"})
public class ClassroomFileController {

    // Only these are shown inside the browser; every other type is downloaded.
    private static final Set<String> INLINE_TYPES = Set.of(
            "video/mp4", "video/webm", "video/ogg", "video/quicktime",
            "image/png", "image/jpeg", "image/webp", "image/gif", "application/pdf");

    private final ClassroomStorageService storage;

    public ClassroomFileController(ClassroomStorageService storage) {
        this.storage = storage;
    }

    @GetMapping("/{category}/{fileName:.+}")
    public ResponseEntity<Resource> file(@PathVariable String category, @PathVariable String fileName) {
        try {
            Path path = storage.resolve(category, fileName);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) return ResponseEntity.notFound().build();

            MediaType type = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
            boolean inline = INLINE_TYPES.contains(type.toString());
            ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                    .filename(ClassroomStorageService.displayName(fileName), StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(type)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(resource);
        } catch (ClassroomException e) {
            return ResponseEntity.status(e.getStatus()).build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
