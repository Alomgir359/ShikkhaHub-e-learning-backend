package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.CourseMaterial;
import com.lms.lms_backend.repository.CourseMaterialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CourseMaterialService {

    private final CourseMaterialRepository materialRepository;

    @Value("${file.upload.directory:uploads/materials}")
    private String uploadDirectory;

    public CourseMaterialService(CourseMaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public String savePDFFile(MultipartFile file, Long courseId, Long teacherId) throws IOException {
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = "course_" + courseId + "_teacher_" + teacherId + "_" +
                UUID.randomUUID().toString() + extension;
        Path filePath = Paths.get(uploadDirectory, filename);
        Files.copy(file.getInputStream(), filePath);

        return "/uploads/materials/" + filename;
    }

    public CourseMaterial saveMaterial(CourseMaterial material) {
        return materialRepository.save(material);
    }

    public List<CourseMaterial> getMaterialsByCourseId(Long courseId) {
        return materialRepository.findByCourseId(courseId);
    }

    public List<CourseMaterial> getMaterialsByTeacherId(Long teacherId) {
        return materialRepository.findByTeacherId(teacherId);
    }

    public Optional<CourseMaterial> getMaterialById(Long id) {
        return materialRepository.findById(id);
    }

    public void deleteMaterial(Long id) {
        materialRepository.deleteById(id);
    }
}