package com.lms.lms_backend.controller;

import com.lms.lms_backend.entity.Admin;
import com.lms.lms_backend.entity.Enrollment;
import com.lms.lms_backend.service.EnrollmentService;
import com.lms.lms_backend.service.SiteSettingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Admin pages (Thymeleaf):
 *  /admin/enrollments     → verify bKash / Nagad payments, approve or reject enrollments
 *  /admin/site-settings   → landing page video upload + payment numbers
 */
@Controller
@RequestMapping("/admin")
public class AdminEnrollmentController {

    private static final Pattern BD_PHONE = Pattern.compile("^01[3-9]\\d{8}$");

    private final EnrollmentService enrollmentService;
    private final SiteSettingService settings;

    public AdminEnrollmentController(EnrollmentService enrollmentService, SiteSettingService settings) {
        this.enrollmentService = enrollmentService;
        this.settings = settings;
    }

    private Admin admin(HttpSession session) {
        Object a = session.getAttribute("admin");
        return a instanceof Admin ? (Admin) a : null;
    }

    private String adminName(Admin a) {
        if (a == null) return "Admin";
        return a.getFullName() != null && !a.getFullName().isBlank() ? a.getFullName() : a.getUsername();
    }

    // ======================= ENROLLMENT REQUESTS =======================

    @GetMapping("/enrollments")
    public String enrollments(@RequestParam(defaultValue = "PENDING") String status,
                              @RequestParam(required = false) String q,
                              Model model, HttpSession session) {
        Admin a = admin(session);
        if (a == null) return "redirect:/admin/login";

        String filter = status.toUpperCase(Locale.ROOT);
        List<Enrollment> list = "ALL".equals(filter)
                ? enrollmentService.getAllEnrollments()
                : enrollmentService.getEnrollmentsByStatus(filter);

        if (q != null && !q.isBlank()) {
            String needle = q.trim().toLowerCase(Locale.ROOT);
            list = list.stream().filter(e ->
                    contains(e.getTransactionId(), needle) || contains(e.getStudentName(), needle)
                            || contains(e.getStudentEmail(), needle) || contains(e.getStudentPhone(), needle)
                            || contains(e.getCourseTitle(), needle))
                    .collect(Collectors.toList());
        }

        model.addAttribute("enrollments", list);
        model.addAttribute("filter", filter);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("pendingCount", enrollmentService.countByStatus(EnrollmentService.PENDING));
        model.addAttribute("activeCount", enrollmentService.countByStatus(EnrollmentService.ACTIVE));
        model.addAttribute("rejectedCount", enrollmentService.countByStatus(EnrollmentService.REJECTED));
        model.addAttribute("totalCount", enrollmentService.countAll());
        model.addAttribute("admin", a);
        return "admin-enrollments";
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    @PostMapping("/enrollments/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(defaultValue = "PENDING") String returnStatus,
                          HttpSession session, RedirectAttributes ra) {
        Admin a = admin(session);
        if (a == null) return "redirect:/admin/login";
        try {
            Enrollment e = enrollmentService.approve(id, adminName(a));
            ra.addFlashAttribute("success", "Approved: " + e.getStudentName() + " → " + e.getCourseTitle()
                    + ". The student can log in now.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/enrollments?status=" + returnStatus;
    }

    @PostMapping("/enrollments/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         @RequestParam(defaultValue = "PENDING") String returnStatus,
                         HttpSession session, RedirectAttributes ra) {
        Admin a = admin(session);
        if (a == null) return "redirect:/admin/login";
        try {
            Enrollment e = enrollmentService.reject(id, reason, adminName(a));
            ra.addFlashAttribute("success", "Rejected: " + e.getStudentName() + " (" + e.getTransactionId() + ")");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/enrollments?status=" + returnStatus;
    }

    // ======================= SITE SETTINGS =======================

    @GetMapping("/site-settings")
    public String siteSettings(Model model, HttpSession session) {
        Admin a = admin(session);
        if (a == null) return "redirect:/admin/login";

        String file = settings.get(SiteSettingService.VIDEO_FILE);
        String poster = settings.get(SiteSettingService.VIDEO_POSTER);
        model.addAttribute("videoEnabled", !"false".equalsIgnoreCase(settings.get(SiteSettingService.VIDEO_ENABLED, "true")));
        model.addAttribute("videoSource", settings.get(SiteSettingService.VIDEO_SOURCE, "UPLOAD"));
        model.addAttribute("videoFile", file);
        model.addAttribute("videoFileUrl", file == null ? null : "/api/site/media/" + file);
        model.addAttribute("videoLink", settings.get(SiteSettingService.VIDEO_LINK, ""));
        model.addAttribute("posterUrl", poster == null ? null : "/api/site/media/" + poster);
        model.addAttribute("videoTitle", settings.get(SiteSettingService.VIDEO_TITLE, ""));
        model.addAttribute("videoSubtitle", settings.get(SiteSettingService.VIDEO_SUBTITLE, ""));
        model.addAttribute("bkashNumber", settings.get(SiteSettingService.BKASH_NUMBER, ""));
        model.addAttribute("nagadNumber", settings.get(SiteSettingService.NAGAD_NUMBER, ""));
        model.addAttribute("paymentNote", settings.get(SiteSettingService.PAYMENT_NOTE, ""));
        model.addAttribute("supportPhone", settings.get(SiteSettingService.SUPPORT_PHONE, ""));
        model.addAttribute("pendingCount", enrollmentService.countByStatus(EnrollmentService.PENDING));
        model.addAttribute("admin", a);
        return "admin-site-settings";
    }

    @PostMapping("/site-settings/video")
    public String saveVideo(@RequestParam(defaultValue = "UPLOAD") String videoSource,
                            @RequestParam(required = false) MultipartFile videoFile,
                            @RequestParam(required = false) MultipartFile posterFile,
                            @RequestParam(required = false) String videoLink,
                            @RequestParam(required = false) String videoTitle,
                            @RequestParam(required = false) String videoSubtitle,
                            @RequestParam(required = false) String videoEnabled,
                            HttpSession session, RedirectAttributes ra) {
        if (admin(session) == null) return "redirect:/admin/login";
        try {
            String source = "LINK".equalsIgnoreCase(videoSource) ? "LINK" : "UPLOAD";
            if (videoFile != null && !videoFile.isEmpty()) settings.saveLandingVideo(videoFile);
            if (posterFile != null && !posterFile.isEmpty()) settings.saveLandingPoster(posterFile);

            if ("LINK".equals(source)) {
                String link = videoLink == null ? "" : videoLink.trim();
                if (!link.isEmpty() && !link.matches("^https?://.+")) {
                    throw new IllegalArgumentException("Video link must start with http:// or https://");
                }
                settings.set(SiteSettingService.VIDEO_LINK, link);
            } else if (settings.get(SiteSettingService.VIDEO_FILE) == null) {
                throw new IllegalArgumentException("Please choose a video file to upload.");
            }

            settings.set(SiteSettingService.VIDEO_SOURCE, source);
            settings.set(SiteSettingService.VIDEO_TITLE, videoTitle);
            settings.set(SiteSettingService.VIDEO_SUBTITLE, videoSubtitle);
            settings.set(SiteSettingService.VIDEO_ENABLED, videoEnabled != null ? "true" : "false");
            ra.addFlashAttribute("success", "Landing page video saved.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/site-settings";
    }

    @PostMapping("/site-settings/video/remove")
    public String removeVideo(HttpSession session, RedirectAttributes ra) {
        if (admin(session) == null) return "redirect:/admin/login";
        settings.removeLandingVideoFile();
        ra.addFlashAttribute("success", "Uploaded video removed.");
        return "redirect:/admin/site-settings";
    }

    @PostMapping("/site-settings/poster/remove")
    public String removePoster(HttpSession session, RedirectAttributes ra) {
        if (admin(session) == null) return "redirect:/admin/login";
        settings.removeLandingPoster();
        ra.addFlashAttribute("success", "Poster image removed.");
        return "redirect:/admin/site-settings";
    }

    @PostMapping("/site-settings/payment")
    public String savePayment(@RequestParam(required = false) String bkashNumber,
                              @RequestParam(required = false) String nagadNumber,
                              @RequestParam(required = false) String paymentNote,
                              @RequestParam(required = false) String supportPhone,
                              HttpSession session, RedirectAttributes ra) {
        if (admin(session) == null) return "redirect:/admin/login";
        String bk = bkashNumber == null ? "" : bkashNumber.replaceAll("[\\s-]", "");
        String ng = nagadNumber == null ? "" : nagadNumber.replaceAll("[\\s-]", "");
        if ((!bk.isEmpty() && !BD_PHONE.matcher(bk).matches()) || (!ng.isEmpty() && !BD_PHONE.matcher(ng).matches())) {
            ra.addFlashAttribute("error", "bKash / Nagad number must be an 11-digit number like 01XXXXXXXXX.");
            return "redirect:/admin/site-settings";
        }
        settings.set(SiteSettingService.BKASH_NUMBER, bk);
        settings.set(SiteSettingService.NAGAD_NUMBER, ng);
        settings.set(SiteSettingService.PAYMENT_NOTE, paymentNote);
        settings.set(SiteSettingService.SUPPORT_PHONE, supportPhone);
        ra.addFlashAttribute("success", "Payment numbers saved.");
        return "redirect:/admin/site-settings";
    }
}
