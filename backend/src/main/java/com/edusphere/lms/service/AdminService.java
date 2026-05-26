package com.edusphere.lms.service;

import com.edusphere.lms.dto.DashboardDtos.AdminAnalyticsResponse;
import com.edusphere.lms.dto.DashboardDtos.AdminStats;
import com.edusphere.lms.dto.DashboardDtos.MonthlyEnrollmentPoint;
import com.edusphere.lms.dto.DashboardDtos.QuizStatPoint;
import com.edusphere.lms.entity.CourseStatus;
import com.edusphere.lms.entity.PaymentStatus;
import com.edusphere.lms.entity.QuizAttempt;
import com.edusphere.lms.repository.CourseRepository;
import com.edusphere.lms.repository.EnrollmentRepository;
import com.edusphere.lms.repository.PaymentTransactionRepository;
import com.edusphere.lms.repository.QuizAttemptRepository;
import com.edusphere.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse analytics() {
        long totalStudents = userRepository.findAll().stream().filter(user -> user.getRole().name().equals("STUDENT")).count();
        long totalCourses = courseRepository.count();
        long totalEnrollments = enrollmentRepository.count();
        long activeCourses = courseRepository.findAll().stream().filter(course -> course.getStatus() == CourseStatus.PUBLISHED).count();

        BigDecimal totalRevenue = paymentRepository.findAll().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .map(payment -> payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<QuizAttempt> attempts = quizAttemptRepository.findAll();
        long quizAttempts = attempts.size();
        double averageQuizScore = attempts.stream()
                .mapToDouble(attempt -> attempt.getTotalQuestions() == 0 ? 0.0 : (attempt.getScore() * 100.0) / attempt.getTotalQuestions())
                .average()
                .orElse(0.0);

        AdminStats summary = new AdminStats(
                totalStudents,
                totalCourses,
                totalEnrollments,
                activeCourses,
                formatCurrency(totalRevenue),
                quizAttempts,
                formatPercent(averageQuizScore)
        );

        return new AdminAnalyticsResponse(
                summary,
                monthlyEnrollments(),
                quizBreakdown(attempts)
        );
    }

    private List<MonthlyEnrollmentPoint> monthlyEnrollments() {
        Map<String, Long> counts = new LinkedHashMap<>();
        LocalDate current = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = current.minusMonths(i);
            String label = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            counts.put(label, 0L);
        }

        enrollmentRepository.findAll().forEach(enrollment -> {
            LocalDate enrolledDate = enrollment.getEnrolledAt().atZone(ZoneId.systemDefault()).toLocalDate();
            String label = enrolledDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            if (counts.containsKey(label)) {
                counts.put(label, counts.get(label) + 1);
            }
        });

        List<MonthlyEnrollmentPoint> points = new ArrayList<>();
        counts.forEach((month, count) -> points.add(new MonthlyEnrollmentPoint(month, count)));
        return points;
    }

    private List<QuizStatPoint> quizBreakdown(List<QuizAttempt> attempts) {
        long strongAttempts = attempts.stream()
                .filter(attempt -> attempt.getTotalQuestions() > 0 && ((attempt.getScore() * 100.0) / attempt.getTotalQuestions()) >= 80)
                .count();
        long averageAttempts = attempts.stream()
                .filter(attempt -> attempt.getTotalQuestions() > 0)
                .filter(attempt -> {
                    double percent = (attempt.getScore() * 100.0) / attempt.getTotalQuestions();
                    return percent >= 50 && percent < 80;
                })
                .count();
        long improvingAttempts = attempts.stream()
                .filter(attempt -> attempt.getTotalQuestions() > 0 && ((attempt.getScore() * 100.0) / attempt.getTotalQuestions()) < 50)
                .count();

        return List.of(
                new QuizStatPoint("High Score", strongAttempts),
                new QuizStatPoint("Average", averageAttempts),
                new QuizStatPoint("Needs Support", improvingAttempts)
        );
    }

    private String formatCurrency(BigDecimal value) {
        return "Rs " + value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatPercent(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
