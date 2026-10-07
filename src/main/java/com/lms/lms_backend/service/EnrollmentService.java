package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.Course;
import com.lms.lms_backend.entity.Enrollment;
import com.lms.lms_backend.entity.Teacher;
import com.lms.lms_backend.repository.CourseRepository;
import com.lms.lms_backend.repository.EnrollmentRepository;
import com.lms.lms_backend.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Enrollment flow (manual mobile-banking payment):
 *  1. Student sends money to the bKash / Nagad personal number shown on the course page.
 *  2. Student submits name, mobile, email, transaction ID (+ password for a new account).
 *     -> enrollment is saved as PENDING; a new account is saved as PENDING too (cannot log in yet).
 *  3. Admin checks the transaction in the bKash / Nagad app and approves or rejects it.
 *     -> approve: enrollment ACTIVE, account APPROVED (can log in), seat count +1
 *     -> reject : enrollment REJECTED with a reason the student sees when trying to log in.
 */
@Service
public class EnrollmentService {

    public static final String PENDING = "PENDING";
    public static final String ACTIVE = "ACTIVE";
    public static final String REJECTED = "REJECTED";
    public static final String COMPLETED = "COMPLETED";

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern BD_PHONE = Pattern.compile("^01[3-9]\\d{8}$");
    private static final Pattern PASSWORD = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,64}$");
    // bKash TrxID is 10 characters, Nagad is 8 — accept 8-12 letters/digits
    private static final Pattern TRX_ID = Pattern.compile("^[A-Z0-9]{8,12}$");

    private final EnrollmentRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             TeacherRepository teacherRepository,
                             CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.teacherRepository = teacherRepository;
        this.courseRepository = courseRepository;
    }

    /** Validation problem; {@code code} lets the frontend react (e.g. ask the user to log in). */
    public static class EnrollmentException extends RuntimeException {
        private final String code;
        public EnrollmentException(String code, String message) {
            super(message);
            this.code = code;
        }
        public String getCode() { return code; }
    }

    // ================= Legacy create (kept for backward compatibility) =================
    @Transactional
    public Enrollment createEnrollment(Enrollment enrollment) {
        if (enrollmentRepository.existsByStudentIdAndCourseId(enrollment.getStudentId(), enrollment.getCourseId())) {
            throw new RuntimeException("Student already enrolled in this course");
        }
        // A paid enrollment is never activated without an admin checking the payment
        boolean free = enrollment.getAmount() == null || enrollment.getAmount() == 0;
        enrollment.setStatus(free ? ACTIVE : PENDING);
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());
        return enrollmentRepository.save(enrollment);
    }

    // ================= New: submit payment info =================
    @Transactional
    public Map<String, Object> submitPaymentEnrollment(Map<String, Object> req) {
        Long courseId = toLong(req.get("courseId"));
        Long studentId = toLong(req.get("studentId"));
        String fullName = str(req.get("fullName"));
        String phone = str(req.get("phone"));
        String email = str(req.get("email"));
        String password = req.get("password") == null ? null : req.get("password").toString();
        String method = str(req.get("paymentMethod"));
        String trxId = str(req.get("transactionId"));

        // ---------- course ----------
        if (courseId == null) throw new EnrollmentException("INVALID", "Course is missing.");
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || course.getIsPublished() == null || course.getIsPublished() != 1) {
            throw new EnrollmentException("COURSE_NOT_AVAILABLE", "এই কোর্সে এখন এনরোল করা যাচ্ছে না।");
        }
        if (course.getTotalSeats() != null && course.getTotalSeats() > 0) {
            int enrolled = course.getEnrolledStudents() == null ? 0 : course.getEnrolledStudents();
            if (enrolled >= course.getTotalSeats()) {
                throw new EnrollmentException("SEATS_FULL", "দুঃখিত, এই ব্যাচের সব সিট পূর্ণ হয়ে গেছে।");
            }
        }
        boolean free = course.getPrice() == null || course.getPrice() == 0;

        // ---------- basic fields ----------
        if (fullName == null || fullName.length() < 2 || fullName.length() > 100)
            throw new EnrollmentException("INVALID", "পূর্ণ নাম লিখুন (২–১০০ অক্ষর)।");
        if (phone == null || !BD_PHONE.matcher(phone).matches())
            throw new EnrollmentException("INVALID", "সঠিক ১১ ডিজিটের মোবাইল নম্বর দিন (01XXXXXXXXX)।");
        if (email == null || !EMAIL.matcher(email).matches())
            throw new EnrollmentException("INVALID", "সঠিক ইমেইল ঠিকানা দিন।");

        // ---------- payment ----------
        if (!free) {
            method = method == null ? null : method.toUpperCase();
            if (!"BKASH".equals(method) && !"NAGAD".equals(method))
                throw new EnrollmentException("INVALID", "bKash অথবা Nagad নির্বাচন করুন।");
            trxId = trxId == null ? null : trxId.toUpperCase().replaceAll("\\s+", "");
            if (trxId == null || !TRX_ID.matcher(trxId).matches())
                throw new EnrollmentException("INVALID", "সঠিক Transaction ID দিন (৮–১২ অক্ষর, শুধু ইংরেজি অক্ষর ও সংখ্যা)।");
            if (enrollmentRepository.existsByTransactionIdIgnoreCase(trxId))
                throw new EnrollmentException("DUPLICATE_TRX", "এই Transaction ID দিয়ে আগেই একটি এনরোলমেন্ট জমা দেওয়া হয়েছে।");
        }

        // ---------- student account ----------
        boolean accountCreated = false;
        Teacher student;
        if (studentId != null) {
            student = teacherRepository.findById(studentId).orElse(null);
            if (student == null || !"STUDENT".equals(student.getRole()))
                throw new EnrollmentException("INVALID_ACCOUNT", "শুধু স্টুডেন্ট অ্যাকাউন্ট দিয়ে এনরোল করা যাবে।");
        } else {
            student = teacherRepository.findByEmail(email).orElse(null);
            if (student == null) {
                if (password == null || !PASSWORD.matcher(password).matches())
                    throw new EnrollmentException("INVALID",
                            "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে — বড় হাতের, ছোট হাতের অক্ষর, সংখ্যা ও একটি চিহ্ন (যেমন @#!) সহ।");
                student = new Teacher();
                student.setFullName(fullName);
                student.setEmail(email);
                student.setPhone(phone);
                student.setPassword(password);
                student.setCurrentProfession("Student");
                student.setOrganization("Self");
                student.setExperience("New Student");
                student.setRole("STUDENT");
                // Paid course: cannot log in until the admin verifies the payment
                student.setStatus(free ? "APPROVED" : PENDING);
                student.setAppliedAt(LocalDateTime.now());
                student.setUpdatedAt(LocalDateTime.now());
                student = teacherRepository.save(student);
                accountCreated = true;
            } else if (!"STUDENT".equals(student.getRole())) {
                throw new EnrollmentException("EMAIL_IN_USE", "এই ইমেইলটি একটি ইন্সট্রাক্টর অ্যাকাউন্টে ব্যবহৃত হচ্ছে। অন্য ইমেইল দিন।");
            } else if ("APPROVED".equals(student.getStatus())) {
                throw new EnrollmentException("ACCOUNT_EXISTS",
                        "এই ইমেইলে আপনার অ্যাকাউন্ট আছে। আগে লগইন করুন, তারপর এনরোল করুন।");
            } else {
                // Account waiting for (or rejected at) a previous payment check — must prove ownership
                if (password == null || !password.equals(student.getPassword()))
                    throw new EnrollmentException("WRONG_PASSWORD",
                            "এই ইমেইলে আগে একটি এনরোলমেন্ট জমা হয়েছে। সেই সময়ের পাসওয়ার্ডটি দিন।");
                if (!free) {
                    student.setStatus(PENDING);
                } else {
                    student.setStatus("APPROVED");
                }
                student.setUpdatedAt(LocalDateTime.now());
                teacherRepository.save(student);
            }
        }

        // ---------- existing enrollment for this course ----------
        Enrollment enrollment = null;
        List<Enrollment> existing = enrollmentRepository
                .findAllByStudentIdAndCourseIdOrderByUpdatedAtDesc(student.getId(), courseId);
        for (Enrollment e : existing) {
            String st = e.getStatus() == null ? ACTIVE : e.getStatus();
            if (PENDING.equals(st))
                throw new EnrollmentException("ALREADY_PENDING",
                        "এই কোর্সে আপনার এনরোলমেন্ট আগেই জমা হয়েছে। অ্যাডমিন পেমেন্ট যাচাই করছেন।");
            if (ACTIVE.equals(st) || COMPLETED.equals(st))
                throw new EnrollmentException("ALREADY_ENROLLED", "আপনি এই কোর্সে আগেই এনরোলড।");
            if (enrollment == null && REJECTED.equals(st)) enrollment = e; // re-submit on the rejected row
        }
        if (enrollment == null) enrollment = new Enrollment();

        enrollment.setStudentId(student.getId());
        enrollment.setCourseId(courseId);
        enrollment.setStudentName(fullName);
        enrollment.setStudentEmail(student.getEmail());
        enrollment.setStudentPhone(phone);
        enrollment.setCourseTitle(course.getCourseTitle());
        enrollment.setCourseCode("CRS-" + courseId);
        enrollment.setAmount(course.getPrice() == null ? 0.0 : course.getPrice());
        enrollment.setPaymentMethod(free ? "FREE" : method);
        enrollment.setTransactionId(free ? "FREE-" + System.currentTimeMillis() : trxId);
        enrollment.setAdminNote(null);
        enrollment.setVerifiedAt(null);
        enrollment.setVerifiedBy(null);
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());

        if (free) {
            enrollment.setStatus(ACTIVE);
            enrollment.setVerifiedAt(LocalDateTime.now());
            enrollment.setVerifiedBy("AUTO (free course)");
            incrementSeats(course);
        } else {
            enrollment.setStatus(PENDING);
        }
        enrollment = enrollmentRepository.save(enrollment);

        Map<String, Object> result = new HashMap<>();
        result.put("enrollment", enrollment);
        result.put("status", enrollment.getStatus());
        result.put("requiresApproval", !free);
        result.put("accountCreated", accountCreated);
        result.put("studentId", student.getId());
        result.put("accountStatus", student.getStatus());
        result.put("message", free
                ? "এনরোলমেন্ট সম্পন্ন হয়েছে! এখন লগইন করে কোর্স শুরু করুন।"
                : "এনরোলমেন্ট সম্পন্ন হয়েছে! অ্যাডমিন আপনার পেমেন্ট যাচাই করে অ্যাপ্রুভ করলে আপনি লগইন করতে পারবেন।");
        return result;
    }

    // ================= Admin actions =================
    @Transactional
    public Enrollment approve(Long id, String adminName) {
        Enrollment e = enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        if (ACTIVE.equals(e.getStatus()) || COMPLETED.equals(e.getStatus())) {
            throw new RuntimeException("This enrollment is already approved.");
        }
        e.setStatus(ACTIVE);
        e.setAdminNote(null);
        e.setVerifiedAt(LocalDateTime.now());
        e.setVerifiedBy(adminName);
        e.setUpdatedAt(LocalDateTime.now());
        enrollmentRepository.save(e);

        // The account can log in from now on
        teacherRepository.findById(e.getStudentId()).ifPresent(s -> {
            if (!"APPROVED".equals(s.getStatus())) {
                s.setStatus("APPROVED");
                s.setUpdatedAt(LocalDateTime.now());
                teacherRepository.save(s);
            }
        });

        courseRepository.findById(e.getCourseId()).ifPresent(this::incrementSeats);
        return e;
    }

    @Transactional
    public Enrollment reject(Long id, String reason, String adminName) {
        Enrollment e = enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        boolean wasActive = ACTIVE.equals(e.getStatus());
        e.setStatus(REJECTED);
        e.setAdminNote(reason == null || reason.isBlank()
                ? "Transaction ID মিলেনি বা পেমেন্ট পাওয়া যায়নি।" : reason.trim());
        e.setVerifiedAt(LocalDateTime.now());
        e.setVerifiedBy(adminName);
        e.setUpdatedAt(LocalDateTime.now());
        enrollmentRepository.save(e);

        if (wasActive) {
            courseRepository.findById(e.getCourseId()).ifPresent(this::decrementSeats);
        }

        // A new account whose only enrollment got rejected stays locked and sees the reason at login.
        teacherRepository.findById(e.getStudentId()).ifPresent(s -> {
            long okCount = enrollmentRepository.countByStudentIdAndStatusIn(s.getId(), List.of(ACTIVE, COMPLETED, PENDING));
            if (PENDING.equals(s.getStatus()) && okCount == 0) {
                s.setStatus(REJECTED);
                s.setUpdatedAt(LocalDateTime.now());
                teacherRepository.save(s);
            }
        });
        return e;
    }

    private void incrementSeats(Course course) {
        int n = (course.getEnrolledStudents() == null ? 0 : course.getEnrolledStudents()) + 1;
        course.setEnrolledStudents(n);
        course.setTotalStudents(n);
        course.setUpdatedAt(LocalDateTime.now());
        courseRepository.save(course);
    }

    private void decrementSeats(Course course) {
        int n = Math.max(0, (course.getEnrolledStudents() == null ? 0 : course.getEnrolledStudents()) - 1);
        course.setEnrolledStudents(n);
        course.setTotalStudents(n);
        course.setUpdatedAt(LocalDateTime.now());
        courseRepository.save(course);
    }

    // ================= Queries =================
    public List<Enrollment> getEnrollmentsByStudentId(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    public List<Enrollment> getEnrollmentsByCourseId(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    /** Only verified enrollments count as "enrolled" (old rows with no status are treated as ACTIVE). */
    public boolean isStudentEnrolled(Long studentId, Long courseId) {
        return enrollmentRepository.findAllByStudentIdAndCourseIdOrderByUpdatedAtDesc(studentId, courseId).stream()
                .anyMatch(e -> e.getStatus() == null || ACTIVE.equals(e.getStatus()) || COMPLETED.equals(e.getStatus()));
    }

    /** Most relevant enrollment for this student + course (verified first, else newest), or null. */
    public Enrollment getLatestEnrollment(Long studentId, Long courseId) {
        List<Enrollment> list = enrollmentRepository.findAllByStudentIdAndCourseIdOrderByUpdatedAtDesc(studentId, courseId);
        for (Enrollment e : list) {
            if (e.getStatus() == null || ACTIVE.equals(e.getStatus()) || COMPLETED.equals(e.getStatus())) return e;
        }
        return list.isEmpty() ? null : list.get(0);
    }

    public Enrollment getLatestRejected(Long studentId) {
        return enrollmentRepository.findFirstByStudentIdAndStatusOrderByUpdatedAtDesc(studentId, REJECTED).orElse(null);
    }

    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAllByOrderByEnrollmentDateDesc();
    }

    public List<Enrollment> getEnrollmentsByStatus(String status) {
        return enrollmentRepository.findByStatusOrderByEnrollmentDateDesc(status);
    }

    public long countByStatus(String status) {
        return enrollmentRepository.countByStatus(status);
    }

    public long countAll() {
        return enrollmentRepository.count();
    }

    @Transactional
    public Enrollment updateEnrollmentStatus(Long id, String status) {
        Enrollment enrollment = enrollmentRepository.findById(id).orElse(null);
        if (enrollment != null) {
            enrollment.setStatus(status);
            enrollment.setUpdatedAt(LocalDateTime.now());
            return enrollmentRepository.save(enrollment);
        }
        return null;
    }

    // ================= helpers =================
    private static String str(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        String s = v.toString().trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }
}
