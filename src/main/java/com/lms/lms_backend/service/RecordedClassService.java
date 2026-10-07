package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.Course;
import com.lms.lms_backend.entity.RecordedClass;
import com.lms.lms_backend.entity.Teacher;
import com.lms.lms_backend.repository.RecordedClassRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RecordedClassService {

    private final RecordedClassRepository repo;
    private final CourseAccessService access;
    private final ClassroomStorageService storage;

    public RecordedClassService(RecordedClassRepository repo, CourseAccessService access, ClassroomStorageService storage) {
        this.repo = repo;
        this.access = access;
        this.storage = storage;
    }

    // ---------------- instructor ----------------

    public RecordedClass create(Long teacherId, Long courseId, String title, String description, Integer classNumber,
                                Integer durationSeconds, String videoUrl, MultipartFile video, MultipartFile thumbnail)
            throws IOException {
        Teacher teacher = access.requireTeacher(teacherId);
        Course course = access.requireOwnedCourse(teacherId, courseId);
        if (title == null || title.isBlank()) throw ClassroomException.badRequest("Class title is required.");
        boolean hasFile = video != null && !video.isEmpty();
        boolean hasLink = videoUrl != null && !videoUrl.isBlank();
        if (!hasFile && !hasLink) throw ClassroomException.badRequest("Please upload a video file (or paste a video link).");
        if (hasLink) requireHttpUrl(videoUrl, "Video link");

        RecordedClass rc = new RecordedClass();
        rc.setCourseId(course.getId());
        rc.setCourseTitle(course.getCourseTitle());
        rc.setTeacherId(teacherId);
        rc.setTeacherName(teacher.getFullName());
        rc.setTitle(title.trim());
        rc.setDescription(blankToNull(description));
        rc.setClassNumber(classNumber != null ? classNumber : (int) repo.countByCourseId(course.getId()) + 1);
        rc.setDurationSeconds(durationSeconds != null && durationSeconds > 0 ? durationSeconds : null);

        String storedVideo = null, storedThumb = null;
        try {
            if (hasFile) storedVideo = storage.storeVideo(video); else rc.setVideoUrl(videoUrl.trim());
            if (thumbnail != null && !thumbnail.isEmpty()) storedThumb = storage.storeThumbnail(thumbnail);
        } catch (RuntimeException | IOException e) {
            storage.delete(ClassroomStorageService.RECORDED, storedVideo);
            throw e;
        }
        rc.setVideoFile(storedVideo);
        rc.setThumbnailFile(storedThumb);
        return repo.save(rc);
    }

    public RecordedClass update(Long id, Long teacherId, String title, String description, Integer classNumber,
                                Integer durationSeconds, String videoUrl, MultipartFile video, MultipartFile thumbnail)
            throws IOException {
        RecordedClass rc = requireOwned(id, teacherId);
        if (title != null) {
            if (title.isBlank()) throw ClassroomException.badRequest("Class title is required.");
            rc.setTitle(title.trim());
        }
        if (description != null) rc.setDescription(blankToNull(description));
        if (classNumber != null) rc.setClassNumber(classNumber);
        if (video != null && !video.isEmpty()) {
            String old = rc.getVideoFile();
            rc.setVideoFile(storage.storeVideo(video));
            rc.setVideoUrl(null);
            storage.delete(ClassroomStorageService.RECORDED, old);
            rc.setDurationSeconds(durationSeconds != null && durationSeconds > 0 ? durationSeconds : null);
        } else {
            if (videoUrl != null && !videoUrl.isBlank()) {
                requireHttpUrl(videoUrl, "Video link");
                storage.delete(ClassroomStorageService.RECORDED, rc.getVideoFile());
                rc.setVideoFile(null);
                rc.setVideoUrl(videoUrl.trim());
            }
            if (durationSeconds != null && durationSeconds > 0) rc.setDurationSeconds(durationSeconds);
        }
        if (thumbnail != null && !thumbnail.isEmpty()) {
            String old = rc.getThumbnailFile();
            rc.setThumbnailFile(storage.storeThumbnail(thumbnail));
            storage.delete(ClassroomStorageService.THUMBNAILS, old);
        }
        rc.setUpdatedAt(LocalDateTime.now());
        return repo.save(rc);
    }

    public void delete(Long id, Long teacherId) {
        RecordedClass rc = requireOwned(id, teacherId);
        storage.delete(ClassroomStorageService.RECORDED, rc.getVideoFile());
        storage.delete(ClassroomStorageService.THUMBNAILS, rc.getThumbnailFile());
        repo.delete(rc);
    }

    public List<Map<String, Object>> forTeacher(Long teacherId, Long courseId) {
        access.requireTeacher(teacherId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (RecordedClass rc : repo.findByTeacherIdOrderByUploadedAtDesc(teacherId)) {
            if (courseId == null || courseId.equals(rc.getCourseId())) out.add(toMap(rc));
        }
        return out;
    }

    // ---------------- student ----------------

    /** Recorded classes of every course the student is enrolled in (optionally one course), newest first. */
    public List<Map<String, Object>> forStudent(Long studentId, Long courseId) {
        access.requireStudent(studentId);
        Set<Long> courseIds = access.enrolledCourseIds(studentId);
        if (courseId != null) {
            if (!courseIds.contains(courseId)) return List.of();
            courseIds = Set.of(courseId);
        }
        if (courseIds.isEmpty()) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (RecordedClass rc : repo.findByCourseIdInOrderByUploadedAtDesc(courseIds)) out.add(toMap(rc));
        return out;
    }

    // ---------------- helpers ----------------

    private RecordedClass requireOwned(Long id, Long teacherId) {
        RecordedClass rc = repo.findById(id).orElseThrow(() -> ClassroomException.notFound("Recorded class not found."));
        if (teacherId == null || !teacherId.equals(rc.getTeacherId())) {
            throw ClassroomException.forbidden("You can only manage your own recorded classes.");
        }
        return rc;
    }

    public Map<String, Object> toMap(RecordedClass rc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rc.getId());
        m.put("courseId", rc.getCourseId());
        m.put("courseTitle", rc.getCourseTitle());
        m.put("teacherName", rc.getTeacherName());
        m.put("title", rc.getTitle());
        m.put("description", rc.getDescription());
        m.put("classNumber", rc.getClassNumber());
        m.put("durationSeconds", rc.getDurationSeconds());
        boolean file = rc.getVideoFile() != null && !rc.getVideoFile().isBlank();
        m.put("videoSource", file ? "FILE" : "LINK");
        m.put("videoUrl", file ? ClassroomStorageService.url(ClassroomStorageService.RECORDED, rc.getVideoFile()) : rc.getVideoUrl());
        m.put("thumbnailUrl", ClassroomStorageService.url(ClassroomStorageService.THUMBNAILS, rc.getThumbnailFile()));
        m.put("uploadedAt", rc.getUploadedAt());
        return m;
    }

    static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    static void requireHttpUrl(String url, String label) {
        String u = url.trim().toLowerCase(Locale.ROOT);
        if (!(u.startsWith("http://") || u.startsWith("https://"))) {
            throw ClassroomException.badRequest(label + " must start with http:// or https://");
        }
    }
}
