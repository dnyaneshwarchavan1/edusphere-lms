package com.edusphere.lms.service;

import com.edusphere.lms.dto.CourseDtos.*;
import com.edusphere.lms.entity.*;
import com.edusphere.lms.exception.ApiException;
import com.edusphere.lms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<CourseResponse> publishedCourses() {
        return courseRepository.findByStatus(CourseStatus.PUBLISHED).stream().map(this::toCourseResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> instructorCourses(User instructor) {
        return courseRepository.findByInstructor(instructor).stream().map(this::toCourseResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> allCourses() {
        return courseRepository.findAll().stream().map(this::toCourseResponse).toList();
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse detail(Long id) {
        Course course = findCourse(id);
        return new CourseDetailResponse(toCourseResponse(course), course.getModules().stream().map(module ->
                new ModuleResponse(module.getId(), module.getTitle(), module.getPosition(), module.getLessons().stream()
                        .map(lesson -> new LessonResponse(lesson.getId(), lesson.getTitle(), lesson.getContent(), lesson.getVideoUrl(), lesson.getResourceUrl(), lesson.getPosition()))
                        .toList())).toList());
    }

    public CourseResponse create(CourseRequest request, User instructor) {
        Course course = Course.builder()
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .level(request.level())
                .thumbnailUrl(request.thumbnailUrl())
                .price(request.price() == null ? BigDecimal.ZERO : request.price())
                .status(CourseStatus.PENDING)
                .instructor(instructor)
                .build();
        Course saved = courseRepository.save(course);
        userRepository.findAllByRole(Role.ADMIN).forEach(admin ->
                notificationService.notifyUser(
                        admin,
                        "New course approval request",
                        instructor.getName() + " submitted " + saved.getTitle() + " for approval.",
                        NotificationType.COURSE_SUBMITTED,
                        "/admin"
                ));
        return toCourseResponse(saved);
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request, User instructor) {
        Course course = findCourse(id);
        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can update only your course");
        }
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setCategory(request.category());
        course.setLevel(request.level());
        course.setThumbnailUrl(request.thumbnailUrl());
        course.setPrice(request.price() == null ? BigDecimal.ZERO : request.price());
        course.setStatus(CourseStatus.PENDING);
        return toCourseResponse(courseRepository.save(course));
    }

    @Transactional
    public ModuleResponse addModule(Long courseId, ModuleRequest request, User instructor) {
        Course course = findCourse(courseId);
        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can edit only your course");
        }
        CourseModule module = CourseModule.builder()
                .title(request.title())
                .position(request.position() == null ? course.getModules().size() + 1 : request.position())
                .course(course)
                .build();
        CourseModule saved = moduleRepository.save(module);
        return new ModuleResponse(saved.getId(), saved.getTitle(), saved.getPosition(), List.of());
    }

    @Transactional
    public LessonResponse addLesson(Long moduleId, LessonRequest request, User instructor) {
        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Module not found"));
        if (!module.getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can edit only your course");
        }
        Lesson lesson = Lesson.builder()
                .title(request.title())
                .content(request.content())
                .videoUrl(request.videoUrl())
                .resourceUrl(request.resourceUrl())
                .position(request.position() == null ? module.getLessons().size() + 1 : request.position())
                .module(module)
                .build();
        Lesson saved = lessonRepository.save(lesson);
        return new LessonResponse(saved.getId(), saved.getTitle(), saved.getContent(), saved.getVideoUrl(), saved.getResourceUrl(), saved.getPosition());
    }

    @Transactional
    public CourseResponse approve(Long courseId) {
        Course course = findCourse(courseId);
        course.setStatus(CourseStatus.PUBLISHED);
        Course saved = courseRepository.save(course);
        notificationService.notifyUser(
                saved.getInstructor(),
                "Course approved",
                saved.getTitle() + " is now published and visible to students.",
                NotificationType.COURSE_APPROVED,
                "/instructor"
        );
        return toCourseResponse(saved);
    }

    @Transactional(readOnly = true)
    public Course findCourse(Long id) {
        return courseRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    private CourseResponse toCourseResponse(Course course) {
        return new CourseResponse(course.getId(), course.getTitle(), course.getDescription(), course.getCategory(), course.getLevel(),
                course.getThumbnailUrl(), course.getPrice(), course.getStatus(), course.getInstructor().getName(), course.getModules().size());
    }
}
