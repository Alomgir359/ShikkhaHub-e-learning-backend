package com.lms.lms_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Stores recorded videos, thumbnails, assignment attachments and student submissions.
 * Files live in {@code file.classroom.directory} (default "classroom-data"), deliberately OUTSIDE the
 * "uploads/" folder because that folder is exposed as static content by application.properties.
 * Everything is served through {@code /api/classroom/files/...} instead.
 */
@Service
public class ClassroomStorageService {

    public static final String RECORDED = "recorded";
    public static final String THUMBNAILS = "thumbnails";
    public static final String ASSIGNMENTS = "assignments";
    public static final String SUBMISSIONS = "submissions";
    public static final Set<String> CATEGORIES = Set.of(RECORDED, THUMBNAILS, ASSIGNMENTS, SUBMISSIONS);

    private static final Set<String> VIDEO_EXT = Set.of("mp4", "webm", "ogg", "mov", "m4v");
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp");
    // Anything that can be run directly on a student's / instructor's computer is refused.
    private static final Set<String> BLOCKED_EXT = Set.of("exe", "bat", "cmd", "com", "msi", "scr", "dll", "vbs", "ps1");

    private static final long MB = 1024L * 1024L;

    @Value("${file.classroom.directory:classroom-data}")
    private String baseDirectory;

    public Path categoryDir(String category) {
        if (!CATEGORIES.contains(category)) throw ClassroomException.notFound("Unknown file category");
        return Paths.get(baseDirectory, category).toAbsolutePath().normalize();
    }

    public String storeVideo(MultipartFile file) throws IOException {
        return store(RECORDED, file, VIDEO_EXT, null, 1024 * MB, "Only MP4, WebM, OGG, MOV or M4V videos are allowed.");
    }

    public String storeThumbnail(MultipartFile file) throws IOException {
        return store(THUMBNAILS, file, IMAGE_EXT, null, 10 * MB, "Thumbnail must be a JPG, PNG or WebP image (max 10MB).");
    }

    public String storeAssignmentFile(MultipartFile file) throws IOException {
        return store(ASSIGNMENTS, file, null, BLOCKED_EXT, 50 * MB, "This file type is not allowed.");
    }

    public String storeSubmission(MultipartFile file) throws IOException {
        return store(SUBMISSIONS, file, null, BLOCKED_EXT, 50 * MB, "This file type is not allowed.");
    }

    private String store(String category, MultipartFile file, Set<String> allowed, Set<String> blocked,
                         long maxBytes, String error) throws IOException {
        if (file == null || file.isEmpty()) throw ClassroomException.badRequest("Please choose a file.");
        if (file.getSize() > maxBytes) {
            throw ClassroomException.badRequest("File is too large (max " + (maxBytes / MB) + "MB).");
        }
        String safeName = sanitize(file.getOriginalFilename());
        String ext = extensionOf(safeName);
        if (allowed != null && !allowed.contains(ext)) throw ClassroomException.badRequest(error);
        if (blocked != null && blocked.contains(ext)) throw ClassroomException.badRequest(error);

        Path dir = categoryDir(category);
        Files.createDirectories(dir);
        // "<random>_<original name>" -> the original name can be shown again when the file is downloaded
        String stored = UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;
        Files.copy(file.getInputStream(), dir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        return stored;
    }

    public Path resolve(String category, String fileName) {
        Path dir = categoryDir(category);
        Path file = dir.resolve(fileName).normalize();
        if (!file.startsWith(dir)) throw ClassroomException.badRequest("Invalid file name");
        return file;
    }

    public void delete(String category, String fileName) {
        if (fileName == null || fileName.isBlank()) return;
        try {
            Files.deleteIfExists(resolve(category, fileName));
        } catch (IOException | RuntimeException ignored) { }
    }

    /** Absolute URL the React app can use to stream / download a stored file. */
    public static String url(String category, String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/classroom/files/").path(category).path("/").path(fileName)
                .build().encode().toUriString();
    }

    /** "ab12cd34_report final.pdf" -> "report final.pdf" */
    public static String displayName(String storedName) {
        if (storedName == null) return null;
        int i = storedName.indexOf('_');
        return i >= 0 && i < storedName.length() - 1 ? storedName.substring(i + 1) : storedName;
    }

    public static String extensionOf(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String sanitize(String original) {
        String name = original == null ? "file" : original;
        name = name.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);              // drop any path
        name = name.replaceAll("[^\\p{L}\\p{N}._ -]", "_").trim();     // letters (incl. Bangla), digits, . _ - space
        if (name.isBlank() || name.startsWith(".")) name = "file" + name;
        return name.length() > 120 ? name.substring(name.length() - 120) : name;
    }
}
