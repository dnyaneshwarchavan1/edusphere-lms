package com.edusphere.lms.service;

import com.edusphere.lms.dto.QuizDtos.*;
import com.edusphere.lms.entity.*;
import com.edusphere.lms.exception.ApiException;
import com.edusphere.lms.repository.QuizAttemptRepository;
import com.edusphere.lms.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {
    private final CourseService courseService;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final NotificationService notificationService;

    @Transactional
    public QuizResponse create(Long courseId, QuizRequest request, User instructor) {
        Course course = courseService.findCourse(courseId);
        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can edit only your course");
        }
        Quiz quiz = Quiz.builder().title(request.title()).course(course).build();
        List<Question> questions = request.questions().stream()
                .map(q -> Question.builder().text(q.text()).options(q.options()).correctAnswer(q.correctAnswer()).quiz(quiz).build())
                .toList();
        quiz.setQuestions(questions);
        Quiz saved = quizRepository.save(quiz);
        notificationService.notifyCourseStudents(
                course,
                "New quiz available",
                saved.getTitle() + " was added to " + course.getTitle() + ".",
                "/courses/" + course.getId() + "?quiz=" + saved.getId()
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> byCourse(Long courseId) {
        return quizRepository.findByCourseId(courseId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> byCourseForInstructor(Long courseId, User instructor) {
        Course course = courseService.findCourse(courseId);
        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can view quizzes only for your course");
        }
        return byCourse(courseId);
    }

    @Transactional
    public QuizAttemptResponse submit(Long quizId, QuizSubmitRequest request, User student) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Quiz not found"));
        int score = 0;
        for (Question question : quiz.getQuestions()) {
            String answer = request.answers().get(question.getId());
            if (question.getCorrectAnswer().equalsIgnoreCase(answer == null ? "" : answer.trim())) {
                score++;
            }
        }
        QuizAttempt attempt = QuizAttempt.builder()
                .student(student)
                .quiz(quiz)
                .score(score)
                .totalQuestions(quiz.getQuestions().size())
                .build();
        QuizAttempt saved = attemptRepository.save(attempt);
        return new QuizAttemptResponse(saved.getId(), quiz.getTitle(), saved.getScore(), saved.getTotalQuestions());
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptResponse> attempts(User student) {
        return attemptRepository.findByStudent(student).stream()
                .map(a -> new QuizAttemptResponse(a.getId(), a.getQuiz().getTitle(), a.getScore(), a.getTotalQuestions()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long countByCourse(Long courseId) {
        return quizRepository.countByCourseId(courseId);
    }

    private QuizResponse toResponse(Quiz quiz) {
        return new QuizResponse(quiz.getId(), quiz.getTitle(), quiz.getQuestions().stream()
                .map(q -> new QuestionResponse(q.getId(), q.getText(), new ArrayList<>(q.getOptions())))
                .toList());
    }
}
