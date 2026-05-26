package com.edusphere.lms.config;

import com.edusphere.lms.entity.*;
import com.edusphere.lms.repository.CourseRepository;
import com.edusphere.lms.repository.QuizRepository;
import com.edusphere.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = userRepository.save(User.builder()
                .name("Admin User")
                .email("admin@edusphere.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        User instructor = userRepository.save(User.builder()
                .name("dnyaneswar Instructor")
                .email("dnyaneswar@edusphere.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.INSTRUCTOR)
                .enabled(true)
                .build());

        userRepository.save(User.builder()
                .name("Rahul Student")
                .email("student@edusphere.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .enabled(true)
                .build());

        Course course = Course.builder()
                .title("Java Full Stack Development")
                .description("Learn Spring Boot, React, REST APIs, authentication, and deployment through a project-based curriculum.")
                .category("Programming")
                .level("Beginner to Intermediate")
                .thumbnailUrl("https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80")
                .price(BigDecimal.ZERO)
                .status(CourseStatus.PUBLISHED)
                .instructor(instructor)
                .build();

        CourseModule module = CourseModule.builder().title("Spring Boot Basics").position(1).course(course).build();
        Lesson lesson = Lesson.builder()
                .title("Build Your First REST API")
                .content("Create controllers, services, repositories, and test your endpoint.")
                .videoUrl("https://www.youtube.com/embed/9SGDpanrc8U")
                .resourceUrl("https://spring.io/projects/spring-boot")
                .position(1)
                .module(module)
                .build();
        module.setLessons(List.of(lesson));
        course.setModules(List.of(module));
        Course savedCourse = courseRepository.save(course);

        Quiz quiz = Quiz.builder().title("Spring Boot Basics Quiz").course(savedCourse).build();
        quiz.setQuestions(List.of(
                Question.builder()
                        .quiz(quiz)
                        .text("Which annotation starts a Spring Boot application?")
                        .options(List.of("@SpringBootApplication", "@Entity", "@Repository", "@Autowired"))
                        .correctAnswer("@SpringBootApplication")
                        .build(),
                Question.builder()
                        .quiz(quiz)
                        .text("Which layer usually contains business logic?")
                        .options(List.of("Controller", "Service", "Entity", "DTO"))
                        .correctAnswer("Service")
                        .build()
        ));
        quizRepository.save(quiz);
    }
}
