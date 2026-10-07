package com.lms.lms_backend.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:3000")
public class FileController {

    @GetMapping("/view/{filename:.+}")
    public ResponseEntity<InputStreamResource> viewFile(@PathVariable String filename) {
        try {
            // Search in materials folder
            File file = Paths.get("uploads/materials", filename).toFile();

            // If not found, search in teachers folder
            if (!file.exists()) {
                file = Paths.get("uploads/teachers", filename).toFile();
            }

            if (file.exists()) {
                InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

                String contentType = "application/octet-stream";
                if (filename.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (filename.endsWith(".png")) {
                    contentType = "image/png";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            }

            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}