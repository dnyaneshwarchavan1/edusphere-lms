package com.edusphere.lms.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public class QuizDtos {
    public record QuestionRequest(@NotBlank String text, List<String> options, @NotBlank String correctAnswer) {}
    public record QuizRequest(@NotBlank String title, List<QuestionRequest> questions) {}
    public record QuestionResponse(Long id, String text, List<String> options) {}
    public record QuizResponse(Long id, String title, List<QuestionResponse> questions) {}
    public record QuizSubmitRequest(Map<Long, String> answers) {}
    public record QuizAttemptResponse(Long id, String quizTitle, int score, int totalQuestions) {}
}
