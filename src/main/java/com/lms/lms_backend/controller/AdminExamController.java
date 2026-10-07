package com.lms.lms_backend.controller;

import com.lms.lms_backend.entity.*;
import com.lms.lms_backend.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin/exam")
public class AdminExamController {

    private final RankService rankService;
    private final ExamService examService;
    private final QuestionService questionService;
    private final EmployeeService employeeService;
    private final ExamAttemptService examAttemptService;

    public AdminExamController(RankService rankService, ExamService examService,
                               QuestionService questionService, EmployeeService employeeService,
                               ExamAttemptService examAttemptService) {
        this.rankService = rankService;
        this.examService = examService;
        this.questionService = questionService;
        this.employeeService = employeeService;
        this.examAttemptService = examAttemptService;
    }

    // Initialize default ranks
    @PostConstruct
    public void init() {
        rankService.initializeDefaultRanks();
    }

    // Sidebar for admin with exam options
    @GetMapping("/dashboard")
    public String examDashboard(Model model, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("ranks", rankService.getAllRanks());
        model.addAttribute("exams", examService.getAllExams());
        model.addAttribute("draftExams", examService.getDraftExams());
        model.addAttribute("publishedExams", examService.getPublishedExams());
        return "admin-exam-dashboard";
    }

    // ==================== RANK MANAGEMENT ====================

    @GetMapping("/ranks")
    public String manageRanks(Model model, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("ranks", rankService.getAllRanks());
        return "admin-ranks";
    }

    @PostMapping("/ranks/add")
    public String addRank(@RequestParam String rankName, @RequestParam Integer priority,
                          @RequestParam String shortCode, @RequestParam(required = false) String description,
                          RedirectAttributes redirectAttributes) {
        try {
            Rank rank = new Rank();
            rank.setRankName(rankName);
            rank.setPriority(priority);
            rank.setShortCode(shortCode);
            rank.setDescription(description);
            rankService.createRank(rank);
            redirectAttributes.addFlashAttribute("success", "Rank added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exam/ranks";
    }

    @PostMapping("/ranks/update/{id}")
    public String updateRank(@PathVariable Long id, @RequestParam String rankName, @RequestParam Integer priority,
                             @RequestParam String shortCode, @RequestParam(required = false) String description,
                             RedirectAttributes redirectAttributes) {
        try {
            Rank rank = new Rank();
            rank.setRankName(rankName);
            rank.setPriority(priority);
            rank.setShortCode(shortCode);
            rank.setDescription(description);
            rankService.updateRank(id, rank);
            redirectAttributes.addFlashAttribute("success", "Rank updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exam/ranks";
    }

    @GetMapping("/ranks/delete/{id}")
    public String deleteRank(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rankService.deleteRank(id);
            redirectAttributes.addFlashAttribute("success", "Rank deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exam/ranks";
    }

    // ==================== EMPLOYEE MANAGEMENT ====================

    @GetMapping("/employees")
    public String manageEmployees(Model model, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("employees", employeeService.getAllEmployees());
        model.addAttribute("ranks", rankService.getAllRanks());
        return "admin-employees";
    }

    @PostMapping("/employees/add")
    public String addEmployee(@RequestParam String employeeId, @RequestParam String fullName,
                              @RequestParam String email, @RequestParam String phone,
                              @RequestParam Long rankId, @RequestParam(required = false) String department,
                              @RequestParam(required = false) String designation,
                              RedirectAttributes redirectAttributes) {
        try {
            Employee employee = new Employee();
            employee.setEmployeeId(employeeId);
            employee.setFullName(fullName);
            employee.setEmail(email);
            employee.setPhone(phone);
            employee.setDepartment(department);
            employee.setDesignation(designation);
            employeeService.createEmployee(employee, rankId);
            redirectAttributes.addFlashAttribute("success", "Employee added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exam/employees";
    }

    @PostMapping("/employees/update/{id}")
    public String updateEmployee(@PathVariable Long id, @RequestParam String fullName,
                                 @RequestParam String email, @RequestParam String phone,
                                 @RequestParam(required = false) Long rankId,
                                 @RequestParam(required = false) String department,
                                 @RequestParam(required = false) String designation,
                                 RedirectAttributes redirectAttributes) {
        try {
            Employee employee = new Employee();
            employee.setFullName(fullName);
            employee.setEmail(email);
            employee.setPhone(phone);
            employee.setDepartment(department);
            employee.setDesignation(designation);
            employeeService.updateEmployee(id, employee, rankId);
            redirectAttributes.addFlashAttribute("success", "Employee updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exam/employees";
    }

    @GetMapping("/employees/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            employeeService.deleteEmployee(id);
            redirectAttributes.addFlashAttribute("success", "Employee deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exam/employees";
    }

    // ==================== EXAM SETTINGS ====================

    @GetMapping("/settings")
    public String examSettings(Model model, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("ranks", rankService.getAllRanks());
        model.addAttribute("exams", examService.getAllExams());
        return "admin-exam-settings";
    }

    @PostMapping("/settings/create")
    public String createExam(@RequestParam String examName, @RequestParam String description,
                             @RequestParam Integer durationMinutes, @RequestParam String examDate,
                             @RequestParam String startTime, @RequestParam String endTime,
                             @RequestParam Long fromRankId, @RequestParam Long toRankId,
                             @RequestParam(required = false) Integer passingMarks,
                             RedirectAttributes redirectAttributes) {
        try {
            Exam exam = new Exam();
            exam.setExamName(examName);
            exam.setDescription(description);
            exam.setDurationMinutes(durationMinutes);

            LocalDateTime examDateTime = LocalDateTime.parse(examDate + "T00:00:00");
            exam.setExamDate(examDateTime);
            exam.setStartTime(LocalDateTime.parse(examDate + "T" + startTime + ":00"));
            exam.setEndTime(LocalDateTime.parse(examDate + "T" + endTime + ":00"));
            exam.setPassingMarks(passingMarks);

            examService.createExam(exam, fromRankId, toRankId);
            redirectAttributes.addFlashAttribute("success", "Exam created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exam/settings";
    }

    @PostMapping("/settings/update/{id}")
    public String updateExam(@PathVariable Long id, @RequestParam String examName, @RequestParam String description,
                             @RequestParam Integer durationMinutes, @RequestParam String examDate,
                             @RequestParam String startTime, @RequestParam String endTime,
                             @RequestParam(required = false) Long fromRankId, @RequestParam(required = false) Long toRankId,
                             @RequestParam(required = false) Integer passingMarks,
                             RedirectAttributes redirectAttributes) {
        try {
            Exam exam = new Exam();
            exam.setExamName(examName);
            exam.setDescription(description);
            exam.setDurationMinutes(durationMinutes);

            LocalDateTime examDateTime = LocalDateTime.parse(examDate + "T00:00:00");
            exam.setExamDate(examDateTime);
            exam.setStartTime(LocalDateTime.parse(examDate + "T" + startTime + ":00"));
            exam.setEndTime(LocalDateTime.parse(examDate + "T" + endTime + ":00"));
            exam.setPassingMarks(passingMarks);

            examService.updateExam(id, exam, fromRankId, toRankId);
            redirectAttributes.addFlashAttribute("success", "Exam updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exam/settings";
    }

    @GetMapping("/settings/publish/{id}")
    public String publishExam(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            examService.publishExam(id);
            redirectAttributes.addFlashAttribute("success", "Exam published successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exam/settings";
    }

    @GetMapping("/settings/delete/{id}")
    public String deleteExam(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            examService.deleteExam(id);
            redirectAttributes.addFlashAttribute("success", "Exam deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exam/settings";
    }

    // ==================== QUESTION SETUP ====================

    @GetMapping("/questions/{examId}")
    public String manageQuestions(@PathVariable Long examId, Model model, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        Exam exam = examService.getExamById(examId);
        if (exam == null) {
            return "redirect:/admin/exam/settings";
        }
        model.addAttribute("exam", exam);
        model.addAttribute("questions", questionService.getQuestionsByExam(examId));
        return "admin-exam-questions";
    }

    @PostMapping("/questions/add-mcq/{examId}")
    public String addMcqQuestion(@PathVariable Long examId, @RequestParam String questionText,
                                 @RequestParam String optionA, @RequestParam String optionB,
                                 @RequestParam String optionC, @RequestParam String optionD,
                                 @RequestParam String correctAnswer, @RequestParam Integer marks,
                                 @RequestParam(required = false) String explanation,
                                 RedirectAttributes redirectAttributes) {
        try {
            Question question = new Question();
            question.setQuestionText(questionText);
            question.setQuestionType("MCQ");
            question.setOptionA(optionA);
            question.setOptionB(optionB);
            question.setOptionC(optionC);
            question.setOptionD(optionD);
            question.setCorrectAnswer(correctAnswer);
            question.setMarks(marks);
            question.setExplanation(explanation);

            questionService.addQuestion(examId, question);
            redirectAttributes.addFlashAttribute("success", "MCQ question added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exam/questions/" + examId;
    }

    @PostMapping("/questions/add-short/{examId}")
    public String addShortQuestion(@PathVariable Long examId, @RequestParam String questionText,
                                   @RequestParam String correctAnswer, @RequestParam Integer marks,
                                   @RequestParam(required = false) String explanation,
                                   RedirectAttributes redirectAttributes) {
        try {
            Question question = new Question();
            question.setQuestionText(questionText);
            question.setQuestionType("SHORT");
            question.setCorrectAnswer(correctAnswer);
            question.setMarks(marks);
            question.setExplanation(explanation);

            questionService.addQuestion(examId, question);
            redirectAttributes.addFlashAttribute("success", "Short question added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exam/questions/" + examId;
    }

    @PostMapping("/questions/add-broad/{examId}")
    public String addBroadQuestion(@PathVariable Long examId, @RequestParam String questionText,
                                   @RequestParam String correctAnswer, @RequestParam Integer marks,
                                   @RequestParam(required = false) String explanation,
                                   RedirectAttributes redirectAttributes) {
        try {
            Question question = new Question();
            question.setQuestionText(questionText);
            question.setQuestionType("BROAD");
            question.setCorrectAnswer(correctAnswer);
            question.setMarks(marks);
            question.setExplanation(explanation);

            questionService.addQuestion(examId, question);
            redirectAttributes.addFlashAttribute("success", "Broad question added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exam/questions/" + examId;
    }

    @PostMapping("/questions/update/{questionId}")
    public String updateQuestion(@PathVariable Long questionId, @RequestParam String questionText,
                                 @RequestParam(required = false) String optionA,
                                 @RequestParam(required = false) String optionB,
                                 @RequestParam(required = false) String optionC,
                                 @RequestParam(required = false) String optionD,
                                 @RequestParam String correctAnswer, @RequestParam Integer marks,
                                 @RequestParam(required = false) String explanation,
                                 RedirectAttributes redirectAttributes) {
        try {
            Question question = new Question();
            question.setQuestionText(questionText);
            question.setOptionA(optionA);
            question.setOptionB(optionB);
            question.setOptionC(optionC);
            question.setOptionD(optionD);
            question.setCorrectAnswer(correctAnswer);
            question.setMarks(marks);
            question.setExplanation(explanation);

            Question existing = questionService.getQuestionById(questionId);
            question.setQuestionType(existing.getQuestionType());

            questionService.updateQuestion(questionId, question);
            redirectAttributes.addFlashAttribute("success", "Question updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/exam/questions/" +
                questionService.getQuestionById(questionId).getExam().getId();
    }

    @GetMapping("/questions/delete/{questionId}")
    public String deleteQuestion(@PathVariable Long questionId, RedirectAttributes redirectAttributes) {
        try {
            Long examId = questionService.getQuestionById(questionId).getExam().getId();
            questionService.deleteQuestion(questionId);
            redirectAttributes.addFlashAttribute("success", "Question deleted successfully!");
            return "redirect:/admin/exam/questions/" + examId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/exam/settings";
        }
    }

    // ==================== RESULTS & ANALYTICS ====================

    @GetMapping("/results")
    public String examResults(Model model, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("exams", examService.getAllExams());
        return "admin-exam-results";
    }

    @GetMapping("/results/{examId}")
    public String examResultDetails(@PathVariable Long examId, Model model, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        Exam exam = examService.getExamById(examId);
        if (exam == null) {
            return "redirect:/admin/exam/results";
        }

        Map<String, Object> results = examAttemptService.getExamResults(examId);
        model.addAttribute("exam", exam);
        model.addAttribute("results", results);
        return "admin-exam-result-details";
    }

    @PostMapping("/results/evaluate/{answerId}")
    @ResponseBody
    public Map<String, Object> evaluateDescriptive(@PathVariable Long answerId, @RequestParam Integer marks) {
        Map<String, Object> response = new HashMap<>();
        try {
            examAttemptService.evaluateDescriptiveQuestion(answerId, marks);
            response.put("success", true);
            response.put("message", "Answer evaluated successfully!");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}