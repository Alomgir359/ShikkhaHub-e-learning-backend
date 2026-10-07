package com.lms.lms_backend.controller;

import com.lms.lms_backend.entity.Employee;
import com.lms.lms_backend.entity.Exam;
import com.lms.lms_backend.entity.ExamAttempt;
import com.lms.lms_backend.service.EmployeeService;
import com.lms.lms_backend.service.ExamService;
import com.lms.lms_backend.service.ExamAttemptService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/employee")
public class EmployeeAuthController {

    private final EmployeeService employeeService;
    private final ExamService examService;
    private final ExamAttemptService examAttemptService;

    public EmployeeAuthController(EmployeeService employeeService, ExamService examService,
                                  ExamAttemptService examAttemptService) {
        this.employeeService = employeeService;
        this.examService = examService;
        this.examAttemptService = examAttemptService;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "employee-login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String email, @RequestParam String password,
                               HttpSession session, Model model) {
        Employee employee = employeeService.login(email, password);
        if (employee != null && "ACTIVE".equals(employee.getStatus())) {
            session.setAttribute("employee", employee);
            return "redirect:/employee/dashboard";
        } else {
            model.addAttribute("error", "Invalid credentials or account inactive!");
            return "employee-login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/employee/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Employee employee = (Employee) session.getAttribute("employee");
        if (employee == null) {
            return "redirect:/employee/login";
        }

        List<Exam> availableExams = examService.getExamsForEmployee(employee);
        List<ExamAttempt> previousAttempts = examAttemptService.getEmployeeAttempts(employee.getId());

        model.addAttribute("employee", employee);
        model.addAttribute("availableExams", availableExams);
        model.addAttribute("previousAttempts", previousAttempts);

        return "employee-dashboard";
    }

    @GetMapping("/exam/start/{examId}")
    public String startExam(@PathVariable Long examId, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Employee employee = (Employee) session.getAttribute("employee");
        if (employee == null) {
            return "redirect:/employee/login";
        }

        Exam exam = examService.getExamById(examId);

        // Check if exam exists
        if (exam == null) {
            redirectAttributes.addFlashAttribute("error", "Exam not found!");
            return "redirect:/employee/dashboard";
        }

        // Check if exam is published
        if (!"PUBLISHED".equals(exam.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "This exam is not yet published!");
            return "redirect:/employee/dashboard";
        }

        // Check if exam is available for employee's rank
        if (!examService.isExamAvailableForEmployee(exam, employee)) {
            redirectAttributes.addFlashAttribute("error", "This exam is not available for your rank!");
            return "redirect:/employee/dashboard";
        }

        // Check if exam time has started
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            redirectAttributes.addFlashAttribute("error", "Exam has not started yet! Exam will start at " +
                    java.time.format.DateTimeFormatter.ofPattern("hh:mm a, dd MMM yyyy").format(exam.getStartTime()));
            return "redirect:/employee/dashboard";
        }

        // Check if exam time has ended
        if (now.isAfter(exam.getEndTime())) {
            redirectAttributes.addFlashAttribute("error", "Exam time has ended! You cannot take this exam now.");
            return "redirect:/employee/dashboard";
        }

        // Check if already attempted
        if (examAttemptService.hasAttempted(examId, employee.getId())) {
            redirectAttributes.addFlashAttribute("error", "You have already attempted this exam!");
            return "redirect:/employee/dashboard";
        }

        try {
            ExamAttempt attempt = examAttemptService.startExam(examId, employee.getId());
            List<com.lms.lms_backend.entity.Question> questions = examService.getQuestionsForExam(examId);

            if (questions == null || questions.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "This exam has no questions! Please contact admin.");
                return "redirect:/employee/dashboard";
            }

            model.addAttribute("attempt", attempt);
            model.addAttribute("exam", exam);
            model.addAttribute("questions", questions);
            return "employee-exam";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/employee/dashboard";
        }
    }

    @PostMapping("/exam/submit/{attemptId}")
    public String submitExam(@PathVariable Long attemptId, HttpSession session, RedirectAttributes redirectAttributes) {
        Employee employee = (Employee) session.getAttribute("employee");
        if (employee == null) {
            return "redirect:/employee/login";
        }

        try {
            examAttemptService.submitExam(attemptId);
            redirectAttributes.addFlashAttribute("success", "Exam submitted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/employee/dashboard";
    }

    @GetMapping("/exam/result/{attemptId}")
    public String examResult(@PathVariable Long attemptId, Model model, HttpSession session) {
        Employee employee = (Employee) session.getAttribute("employee");
        if (employee == null) {
            return "redirect:/employee/login";
        }

        ExamAttempt attempt = examAttemptService.getAttemptById(attemptId);
        if (attempt == null || !attempt.getEmployee().getId().equals(employee.getId())) {
            return "redirect:/employee/dashboard";
        }

        model.addAttribute("attempt", attempt);
        model.addAttribute("answers", examAttemptService.getAnswersForAttempt(attemptId));
        return "employee-result";
    }
}