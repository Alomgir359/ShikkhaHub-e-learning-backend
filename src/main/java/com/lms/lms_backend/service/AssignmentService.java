package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.AssignmentSubmission;
import com.lms.lms_backend.entity.Course;
import com.lms.lms_backend.entity.CourseAssignment;
import com.lms.lms_backend.entity.Teacher;
import com.lms.lms_backend.repository.AssignmentSubmissionRepository;
import com.lms.lms_backend.repository.CourseAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    public static final String PENDING = "PENDING"; // student-side only: nothing submitted yet
    private static final Set<String> REVIEW_STATUSES = Set.of(
            AssignmentSubmission.SUBMITTED, AssignmentSubmission.UNDER_REVIEW,
            AssignmentSubmission.GRADED, AssignmentSubmission.RESUBMIT);

    private final CourseAssignmentRepository assignments;
    private final AssignmentSubmissionRepository submissions;
    private final CourseAccessService access;
    private final ClassroomStorageService storage;

    public AssignmentService(CourseAssignmentRepository assignments, AssignmentSubmissionRepository submissions,
                             CourseAccessService access, ClassroomStorageService storage) {
        this.assignments = assignments;
        this.submissions = submissions;
        this.access = access;
        this.storage = storage;
    }

    // =====================================================================
    // Instructor: assignments
    // =====================================================================

    public CourseAssignment create(Long teacherId, Long courseId, String title, String instructions,
                                   String deadline, Integer maxMarks, MultipartFile attachment) throws IOException {
        Teacher teacher = access.requireTeacher(teacherId);
        Course course = access.requireOwnedCourse(teacherId, courseId);
        if (title == null || title.isBlank()) throw ClassroomException.badRequest("Assignment title is required.");

        CourseAssignment a = new CourseAssignment();
        a.setCourseId(course.getId());
        a.setCourseTitle(course.getCourseTitle());
        a.setTeacherId(teacherId);
        a.setTeacherName(teacher.getFullName());
        a.setTitle(title.trim());
        a.setInstructions(RecordedClassService.blankToNull(instructions));
        a.setDeadline(parseDeadline(deadline));
        a.setMaxMarks(validMarks(maxMarks));
        if (attachment != null && !attachment.isEmpty()) {
            String stored = storage.storeAssignmentFile(attachment);
            a.setAttachmentFile(stored);
            a.setAttachmentName(ClassroomStorageService.displayName(stored));
        }
        return assignments.save(a);
    }

    public CourseAssignment update(Long id, Long teacherId, String title, String instructions, String deadline,
                                   Integer maxMarks, MultipartFile attachment, boolean removeAttachment) throws IOException {
        CourseAssignment a = requireOwned(id, teacherId);
        if (title != null) {
            if (title.isBlank()) throw ClassroomException.badRequest("Assignment title is required.");
            a.setTitle(title.trim());
        }
        if (instructions != null) a.setInstructions(RecordedClassService.blankToNull(instructions));
        if (deadline != null) a.setDeadline(parseDeadline(deadline)); // "" clears the deadline
        if (maxMarks != null) a.setMaxMarks(validMarks(maxMarks));
        if (attachment != null && !attachment.isEmpty()) {
            String old = a.getAttachmentFile();
            String stored = storage.storeAssignmentFile(attachment);
            a.setAttachmentFile(stored);
            a.setAttachmentName(ClassroomStorageService.displayName(stored));
            storage.delete(ClassroomStorageService.ASSIGNMENTS, old);
        } else if (removeAttachment) {
            storage.delete(ClassroomStorageService.ASSIGNMENTS, a.getAttachmentFile());
            a.setAttachmentFile(null);
            a.setAttachmentName(null);
        }
        a.setUpdatedAt(LocalDateTime.now());
        return assignments.save(a);
    }

    @Transactional
    public void delete(Long id, Long teacherId) {
        CourseAssignment a = requireOwned(id, teacherId);
        for (AssignmentSubmission s : submissions.findByAssignmentId(id)) {
            storage.delete(ClassroomStorageService.SUBMISSIONS, s.getSubmissionFile());
            submissions.delete(s);
        }
        storage.delete(ClassroomStorageService.ASSIGNMENTS, a.getAttachmentFile());
        assignments.delete(a);
    }

    /** Instructor's assignments with submission counters. */
    public List<Map<String, Object>> forTeacher(Long teacherId, Long courseId) {
        access.requireTeacher(teacherId);
        List<CourseAssignment> list = assignments.findByTeacherIdOrderByCreatedAtDesc(teacherId).stream()
                .filter(a -> courseId == null || courseId.equals(a.getCourseId())).toList();
        Map<Long, List<AssignmentSubmission>> byAssignment = list.isEmpty() ? Map.of()
                : submissions.findByAssignmentIdIn(list.stream().map(CourseAssignment::getId).toList()).stream()
                        .collect(Collectors.groupingBy(AssignmentSubmission::getAssignmentId));
        Map<Long, Integer> enrolledByCourse = new HashMap<>();

        List<Map<String, Object>> out = new ArrayList<>();
        for (CourseAssignment a : list) {
            List<AssignmentSubmission> subs = byAssignment.getOrDefault(a.getId(), List.of());
            int enrolled = enrolledByCourse.computeIfAbsent(a.getCourseId(), cid -> access.verifiedEnrollments(cid).size());
            Map<String, Object> m = toMap(a);
            m.put("enrolledCount", enrolled);
            m.put("submittedCount", subs.size());
            m.put("gradedCount", subs.stream().filter(s -> AssignmentSubmission.GRADED.equals(s.getStatus())).count());
            m.put("toReviewCount", subs.stream().filter(s -> AssignmentSubmission.SUBMITTED.equals(s.getStatus())).count());
            out.add(m);
        }
        return out;
    }

    public long activeCount(Long teacherId) {
        LocalDateTime now = LocalDateTime.now();
        return assignments.findByTeacherIdOrderByCreatedAtDesc(teacherId).stream()
                .filter(a -> a.getDeadline() == null || a.getDeadline().isAfter(now)).count();
    }

    // =====================================================================
    // Student: assignments + submit
    // =====================================================================

    /** Assignments of the student's enrolled courses, each with the student's own submission (if any). */
    public List<Map<String, Object>> forStudent(Long studentId) {
        access.requireStudent(studentId);
        Set<Long> courseIds = access.enrolledCourseIds(studentId);
        if (courseIds.isEmpty()) return List.of();
        Map<Long, AssignmentSubmission> mine = submissions.findByStudentId(studentId).stream()
                .collect(Collectors.toMap(AssignmentSubmission::getAssignmentId, Function.identity(), (x, y) -> x));
        List<Map<String, Object>> out = new ArrayList<>();
        for (CourseAssignment a : assignments.findByCourseIdInOrderByCreatedAtDesc(courseIds)) {
            AssignmentSubmission s = mine.get(a.getId());
            Map<String, Object> m = toMap(a);
            m.put("status", s == null ? PENDING : s.getStatus());
            m.put("submission", s == null ? null : submissionMap(s, a));
            out.add(m);
        }
        return out;
    }

    /** Assignments still waiting for the student's work (nothing submitted, or resubmission requested). */
    public long pendingCount(Long studentId) {
        return forStudent(studentId).stream()
                .filter(m -> PENDING.equals(m.get("status")) || AssignmentSubmission.RESUBMIT.equals(m.get("status")))
                .count();
    }

    @Transactional
    public AssignmentSubmission submit(Long assignmentId, Long studentId, MultipartFile file, String link, String note)
            throws IOException {
        Teacher student = access.requireStudent(studentId);
        CourseAssignment a = assignments.findById(assignmentId)
                .orElseThrow(() -> ClassroomException.notFound("Assignment not found."));
        access.requireEnrolled(studentId, a.getCourseId());

        boolean hasFile = file != null && !file.isEmpty();
        boolean hasLink = link != null && !link.isBlank();
        if (!hasFile && !hasLink) {
            throw ClassroomException.badRequest("Please attach a file or paste a GitHub / Google Drive link.");
        }
        if (hasLink) RecordedClassService.requireHttpUrl(link, "Link");

        AssignmentSubmission s = submissions.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseGet(AssignmentSubmission::new);
        if (s.getId() != null && AssignmentSubmission.GRADED.equals(s.getStatus())) {
            throw ClassroomException.conflict("This assignment has already been graded, so it can't be changed.");
        }

        String newStored = hasFile ? storage.storeSubmission(file) : null;
        String oldStored = s.getSubmissionFile();

        s.setAssignmentId(assignmentId);
        s.setCourseId(a.getCourseId());
        s.setStudentId(studentId);
        s.setStudentName(student.getFullName());
        s.setStudentEmail(student.getEmail());
        s.setSubmissionFile(newStored);                                   // resubmission replaces the old work
        s.setSubmissionFileName(newStored == null ? null : ClassroomStorageService.displayName(newStored));
        s.setSubmissionLink(hasLink ? link.trim() : null);
        s.setNote(RecordedClassService.blankToNull(note));
        s.setStatus(AssignmentSubmission.SUBMITTED);
        s.setMarks(null);
        s.setGrade(null);
        s.setGradedAt(null);
        LocalDateTime now = LocalDateTime.now();
        s.setLate(a.getDeadline() != null && now.isAfter(a.getDeadline()));
        s.setSubmittedAt(now);
        s.setUpdatedAt(now);
        AssignmentSubmission saved = submissions.save(s);
        if (oldStored != null && !oldStored.equals(newStored)) storage.delete(ClassroomStorageService.SUBMISSIONS, oldStored);
        return saved;
    }

    // =====================================================================
    // Instructor: submissions + grading
    // =====================================================================

    public List<Map<String, Object>> submissionsForTeacher(Long teacherId, Long courseId, Long assignmentId, String status) {
        access.requireTeacher(teacherId);
        Map<Long, CourseAssignment> mine = assignments.findByTeacherIdOrderByCreatedAtDesc(teacherId).stream()
                .collect(Collectors.toMap(CourseAssignment::getId, Function.identity()));
        if (mine.isEmpty()) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (AssignmentSubmission s : submissions.findByAssignmentIdIn(mine.keySet())) {
            CourseAssignment a = mine.get(s.getAssignmentId());
            if (courseId != null && !courseId.equals(s.getCourseId())) continue;
            if (assignmentId != null && !assignmentId.equals(s.getAssignmentId())) continue;
            if (status != null && !status.isBlank() && !status.equalsIgnoreCase(s.getStatus())) continue;
            out.add(submissionMap(s, a));
        }
        out.sort((x, y) -> ((LocalDateTime) y.get("submittedAt")).compareTo((LocalDateTime) x.get("submittedAt")));
        return out;
    }

    public List<Map<String, Object>> recentSubmissions(Long teacherId, int limit) {
        List<Map<String, Object>> all = submissionsForTeacher(teacherId, null, null, null);
        return all.size() > limit ? all.subList(0, limit) : all;
    }

    @Transactional
    public AssignmentSubmission review(Long submissionId, Long teacherId, Double marks, String grade,
                                       String feedback, String status) {
        AssignmentSubmission s = submissions.findById(submissionId)
                .orElseThrow(() -> ClassroomException.notFound("Submission not found."));
        CourseAssignment a = assignments.findById(s.getAssignmentId())
                .orElseThrow(() -> ClassroomException.notFound("Assignment not found."));
        if (teacherId == null || !teacherId.equals(a.getTeacherId())) {
            throw ClassroomException.forbidden("You can only review submissions of your own assignments.");
        }
        if (marks != null && (marks < 0 || (a.getMaxMarks() != null && marks > a.getMaxMarks()))) {
            throw ClassroomException.badRequest("Marks must be between 0 and " + a.getMaxMarks() + ".");
        }
        boolean hasGrade = grade != null && !grade.isBlank();
        String newStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        if (newStatus != null && !REVIEW_STATUSES.contains(newStatus)) throw ClassroomException.badRequest("Invalid status.");
        if (newStatus == null) newStatus = (marks != null || hasGrade) ? AssignmentSubmission.GRADED : AssignmentSubmission.UNDER_REVIEW;
        if (AssignmentSubmission.GRADED.equals(newStatus) && marks == null && !hasGrade) {
            throw ClassroomException.badRequest("Enter marks or a grade before setting the status to Graded.");
        }

        s.setMarks(marks);
        s.setGrade(hasGrade ? grade.trim() : null);
        s.setFeedback(RecordedClassService.blankToNull(feedback));
        s.setStatus(newStatus);
        s.setGradedAt(AssignmentSubmission.GRADED.equals(newStatus) ? LocalDateTime.now() : null);
        s.setUpdatedAt(LocalDateTime.now());
        return submissions.save(s);
    }

    // =====================================================================
    // helpers
    // =====================================================================

    private CourseAssignment requireOwned(Long id, Long teacherId) {
        CourseAssignment a = assignments.findById(id).orElseThrow(() -> ClassroomException.notFound("Assignment not found."));
        if (teacherId == null || !teacherId.equals(a.getTeacherId())) {
            throw ClassroomException.forbidden("You can only manage your own assignments.");
        }
        return a;
    }

    private static LocalDateTime parseDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) return null;
        try { return LocalDateTime.parse(deadline.trim()); }
        catch (DateTimeParseException e) { throw ClassroomException.badRequest("Please choose a valid deadline."); }
    }

    private static int validMarks(Integer maxMarks) {
        if (maxMarks == null) return 100;
        if (maxMarks < 1 || maxMarks > 1000) throw ClassroomException.badRequest("Total marks must be between 1 and 1000.");
        return maxMarks;
    }

    public Map<String, Object> toMap(CourseAssignment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("courseId", a.getCourseId());
        m.put("courseTitle", a.getCourseTitle());
        m.put("teacherName", a.getTeacherName());
        m.put("title", a.getTitle());
        m.put("instructions", a.getInstructions());
        m.put("deadline", a.getDeadline());
        m.put("maxMarks", a.getMaxMarks());
        m.put("attachmentName", a.getAttachmentName());
        m.put("attachmentUrl", ClassroomStorageService.url(ClassroomStorageService.ASSIGNMENTS, a.getAttachmentFile()));
        m.put("overdue", a.getDeadline() != null && LocalDateTime.now().isAfter(a.getDeadline()));
        m.put("createdAt", a.getCreatedAt());
        return m;
    }

    public Map<String, Object> submissionMap(AssignmentSubmission s, CourseAssignment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("assignmentId", s.getAssignmentId());
        m.put("assignmentTitle", a == null ? null : a.getTitle());
        m.put("maxMarks", a == null ? null : a.getMaxMarks());
        m.put("courseId", s.getCourseId());
        m.put("courseTitle", a == null ? null : a.getCourseTitle());
        m.put("studentId", s.getStudentId());
        m.put("studentName", s.getStudentName());
        m.put("studentEmail", s.getStudentEmail());
        m.put("fileName", s.getSubmissionFileName());
        m.put("fileUrl", ClassroomStorageService.url(ClassroomStorageService.SUBMISSIONS, s.getSubmissionFile()));
        m.put("link", s.getSubmissionLink());
        m.put("note", s.getNote());
        m.put("status", s.getStatus());
        m.put("marks", s.getMarks());
        m.put("grade", s.getGrade());
        m.put("feedback", s.getFeedback());
        m.put("late", s.isLate());
        m.put("submittedAt", s.getSubmittedAt());
        m.put("gradedAt", s.getGradedAt());
        return m;
    }
}
