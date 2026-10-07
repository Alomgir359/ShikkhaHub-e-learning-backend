package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.Course;
import com.lms.lms_backend.entity.LiveClass;
import com.lms.lms_backend.entity.Teacher;
import com.lms.lms_backend.repository.LiveClassRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class LiveClassService {

    private final LiveClassRepository repo;
    private final CourseAccessService access;

    public LiveClassService(LiveClassRepository repo, CourseAccessService access) {
        this.repo = repo;
        this.access = access;
    }

    public LiveClass create(Long teacherId, Long courseId, String title, String description,
                            String date, String startTime, String meetingLink) {
        Teacher teacher = access.requireTeacher(teacherId);
        Course course = access.requireOwnedCourse(teacherId, courseId);
        LiveClass lc = new LiveClass();
        lc.setCourseId(course.getId());
        lc.setCourseTitle(course.getCourseTitle());
        lc.setTeacherId(teacherId);
        lc.setTeacherName(teacher.getFullName());
        apply(lc, title, description, date, startTime, meetingLink, true);
        return repo.save(lc);
    }

    public LiveClass update(Long id, Long teacherId, String title, String description,
                            String date, String startTime, String meetingLink) {
        LiveClass lc = requireOwned(id, teacherId);
        apply(lc, title, description, date, startTime, meetingLink, false);
        lc.setUpdatedAt(LocalDateTime.now());
        return repo.save(lc);
    }

    public void delete(Long id, Long teacherId) {
        repo.delete(requireOwned(id, teacherId));
    }

    public List<Map<String, Object>> forTeacher(Long teacherId, Long courseId) {
        access.requireTeacher(teacherId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (LiveClass lc : repo.findByTeacherIdOrderByClassDateDescStartTimeDesc(teacherId)) {
            if (courseId == null || courseId.equals(lc.getCourseId())) out.add(toMap(lc));
        }
        return out;
    }

    /** Live classes (on/after {@code from}) of the courses the student is enrolled in, soonest first. */
    public List<Map<String, Object>> forStudent(Long studentId, LocalDate from) {
        access.requireStudent(studentId);
        Set<Long> courseIds = access.enrolledCourseIds(studentId);
        if (courseIds.isEmpty()) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (LiveClass lc : repo.findByCourseIdInAndClassDateGreaterThanEqualOrderByClassDateAscStartTimeAsc(courseIds, from)) {
            out.add(toMap(lc));
        }
        return out;
    }

    public long countUpcoming(Long teacherId, LocalDate from) {
        return repo.findByTeacherIdOrderByClassDateDescStartTimeDesc(teacherId).stream()
                .filter(l -> !l.getClassDate().isBefore(from)).count();
    }

    private void apply(LiveClass lc, String title, String description, String date, String startTime,
                       String meetingLink, boolean creating) {
        if (creating || title != null) {
            if (title == null || title.isBlank()) throw ClassroomException.badRequest("Class title is required.");
            lc.setTitle(title.trim());
        }
        if (description != null) lc.setDescription(RecordedClassService.blankToNull(description));
        if (creating || date != null) {
            try { lc.setClassDate(LocalDate.parse(date)); }
            catch (DateTimeParseException | NullPointerException e) { throw ClassroomException.badRequest("Please choose a valid date."); }
        }
        if (creating || startTime != null) {
            try { lc.setStartTime(LocalTime.parse(startTime)); }
            catch (DateTimeParseException | NullPointerException e) { throw ClassroomException.badRequest("Please choose a valid start time."); }
        }
        if (creating || meetingLink != null) {
            if (meetingLink == null || meetingLink.isBlank()) throw ClassroomException.badRequest("Zoom meeting link is required.");
            RecordedClassService.requireHttpUrl(meetingLink, "Meeting link");
            lc.setMeetingLink(meetingLink.trim());
        }
    }

    private LiveClass requireOwned(Long id, Long teacherId) {
        LiveClass lc = repo.findById(id).orElseThrow(() -> ClassroomException.notFound("Live class not found."));
        if (teacherId == null || !teacherId.equals(lc.getTeacherId())) {
            throw ClassroomException.forbidden("You can only manage your own live classes.");
        }
        return lc;
    }

    public Map<String, Object> toMap(LiveClass lc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", lc.getId());
        m.put("courseId", lc.getCourseId());
        m.put("courseTitle", lc.getCourseTitle());
        m.put("teacherName", lc.getTeacherName());
        m.put("title", lc.getTitle());
        m.put("description", lc.getDescription());
        m.put("classDate", lc.getClassDate().toString());
        m.put("startTime", lc.getStartTime().toString());
        m.put("meetingLink", lc.getMeetingLink());
        return m;
    }
}
