package com.edusphere.lms.dto;

public class DashboardDtos {
    public record EnrollmentResponse(Long id, Long courseId, String courseTitle, int progressPercent) {}
    public record UserResponse(Long id, String name, String email, String role, boolean enabled) {}
    public record AdminStats(
            long users,
            long courses,
            long enrollments,
            long activeCourses,
            String totalRevenue,
            long quizAttempts,
            String averageQuizScore
    ) {}
    public record MonthlyEnrollmentPoint(String month, long enrollments) {}
    public record QuizStatPoint(String label, double value) {}
    public record AdminAnalyticsResponse(
            AdminStats summary,
            java.util.List<MonthlyEnrollmentPoint> monthlyEnrollments,
            java.util.List<QuizStatPoint> quizBreakdown
    ) {}
    public record AdminEnrollmentResponse(
            Long enrollmentId,
            String studentName,
            String studentEmail,
            Long courseId,
            String courseTitle,
            int progressPercent,
            String paymentStatus,
            String paymentReference,
            String paymentMethod
    ) {}
}
