package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.SiteSetting;
import com.lms.lms_backend.repository.SiteSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SiteSettingService {

    // ---- keys ----
    public static final String VIDEO_ENABLED = "landing.video.enabled";      // "true" / "false"
    public static final String VIDEO_SOURCE = "landing.video.source";        // UPLOAD or LINK
    public static final String VIDEO_FILE = "landing.video.file";            // stored file name (UPLOAD)
    public static final String VIDEO_LINK = "landing.video.link";            // YouTube / Vimeo / mp4 URL (LINK)
    public static final String VIDEO_POSTER = "landing.video.poster";        // stored poster image file name
    public static final String VIDEO_TITLE = "landing.video.title";
    public static final String VIDEO_SUBTITLE = "landing.video.subtitle";

    public static final String BKASH_NUMBER = "payment.bkash.number";
    public static final String NAGAD_NUMBER = "payment.nagad.number";
    public static final String PAYMENT_NOTE = "payment.note";
    public static final String SUPPORT_PHONE = "payment.support.phone";

    private static final Set<String> VIDEO_EXT = Set.of(".mp4", ".webm", ".ogg", ".mov");
    private static final Set<String> IMAGE_EXT = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final SiteSettingRepository repo;

    @Value("${file.site.directory:uploads/site}")
    private String siteDirectory;

    public SiteSettingService(SiteSettingRepository repo) {
        this.repo = repo;
    }

    public String get(String key) {
        return repo.findById(key).map(SiteSetting::getValue).orElse(null);
    }

    public String get(String key, String fallback) {
        String v = get(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    @Transactional
    public void set(String key, String value) {
        SiteSetting s = repo.findById(key).orElse(new SiteSetting(key, null));
        s.setValue(value == null ? null : value.trim());
        s.setUpdatedAt(LocalDateTime.now());
        repo.save(s);
    }

    public Map<String, String> all() {
        Map<String, String> m = new LinkedHashMap<>();
        repo.findAll().forEach(s -> m.put(s.getKey(), s.getValue()));
        return m;
    }

    public Path siteDir() {
        return Paths.get(siteDirectory);
    }

    /** Saves a landing video file, deletes the old one, returns the new stored file name. */
    @Transactional
    public String saveLandingVideo(MultipartFile file) throws IOException {
        String name = storeFile(file, "landing_", VIDEO_EXT, "Only MP4, WebM, OGG or MOV videos are allowed.");
        deleteStored(get(VIDEO_FILE));
        set(VIDEO_FILE, name);
        return name;
    }

    @Transactional
    public String saveLandingPoster(MultipartFile file) throws IOException {
        String name = storeFile(file, "poster_", IMAGE_EXT, "Poster must be a JPG, PNG or WebP image.");
        deleteStored(get(VIDEO_POSTER));
        set(VIDEO_POSTER, name);
        return name;
    }

    @Transactional
    public void removeLandingVideoFile() {
        deleteStored(get(VIDEO_FILE));
        set(VIDEO_FILE, null);
    }

    @Transactional
    public void removeLandingPoster() {
        deleteStored(get(VIDEO_POSTER));
        set(VIDEO_POSTER, null);
    }

    private String storeFile(MultipartFile file, String prefix, Set<String> allowed, String error) throws IOException {
        if (file == null || file.isEmpty()) throw new IOException("Please choose a file.");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase() : "";
        if (!allowed.contains(ext)) throw new IOException(error);
        Path dir = siteDir();
        if (!Files.exists(dir)) Files.createDirectories(dir);
        String name = prefix + UUID.randomUUID() + ext;
        Files.copy(file.getInputStream(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        return name;
    }

    private void deleteStored(String name) {
        if (name == null || name.isBlank()) return;
        try {
            Path p = siteDir().resolve(name).normalize();
            if (p.startsWith(siteDir().normalize())) Files.deleteIfExists(p);
        } catch (IOException ignored) { }
    }
}
