import { useEffect, useState } from 'react';
import api from '../api/client';

const emptyCourse = {
  title: '',
  description: '',
  category: 'Programming',
  level: 'Beginner',
  thumbnailUrl: '',
  pricingType: 'FREE',
  price: 0
};

const emptyModule = { courseId: '', title: '', position: 1 };
const emptyLesson = { moduleId: '', title: '', content: '', videoUrl: '', resourceUrl: '', position: 1 };
const emptyQuiz = {
  courseId: '',
  title: '',
  questions: [{ text: '', options: ['', '', '', ''], correctAnswer: '' }]
};

export default function InstructorDashboard() {
  const [courses, setCourses] = useState([]);
  const [selectedCourseId, setSelectedCourseId] = useState('');
  const [selectedCourseDetail, setSelectedCourseDetail] = useState(null);
  const [selectedCourseQuizzes, setSelectedCourseQuizzes] = useState([]);
  const [courseForm, setCourseForm] = useState(emptyCourse);
  const [moduleForm, setModuleForm] = useState(emptyModule);
  const [lessonForm, setLessonForm] = useState(emptyLesson);
  const [quizForm, setQuizForm] = useState(emptyQuiz);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  function loadCourses() {
    api.get('/instructor/courses')
      .then((res) => {
        setCourses(res.data);
        setError('');
      })
      .catch((err) => {
        setError(err.response?.data?.message || 'Unable to load instructor courses.');
      });
  }

  function loadCourseDetail(courseId) {
    if (!courseId) {
      setSelectedCourseDetail(null);
      setSelectedCourseQuizzes([]);
      return;
    }

    api.get(`/courses/${courseId}`)
      .then((res) => {
        setSelectedCourseDetail(res.data);
        setError('');
      })
      .catch((err) => {
        setError(err.response?.data?.message || 'Unable to load course details.');
      });

    api.get(`/instructor/courses/${courseId}/quizzes`)
      .then((res) => setSelectedCourseQuizzes(res.data))
      .catch(() => setSelectedCourseQuizzes([]));
  }

  useEffect(loadCourses, []);

  useEffect(() => {
    loadCourseDetail(selectedCourseId);
  }, [selectedCourseId]);

  function refreshAll(courseId = selectedCourseId) {
    loadCourses();
    if (courseId) {
      loadCourseDetail(courseId);
    }
  }

  function pickCourse(courseId) {
    setSelectedCourseId(courseId);
    setModuleForm((current) => ({ ...current, courseId }));
    setQuizForm((current) => ({ ...current, courseId }));
    setLessonForm((current) => ({ ...current, moduleId: '' }));
  }

  async function createCourse(e) {
    e.preventDefault();
    const payload = {
      ...courseForm,
      price: courseForm.pricingType === 'FREE' ? 0 : Number(courseForm.price || 0)
    };
    const { data } = await api.post('/instructor/courses', payload);
    setCourseForm(emptyCourse);
    pickCourse(String(data.id));
    setMessage('Course submitted for admin approval.');
    refreshAll(String(data.id));
  }

  async function addModule(e) {
    e.preventDefault();
    const { data } = await api.post(`/instructor/courses/${moduleForm.courseId}/modules`, {
      title: moduleForm.title,
      position: Number(moduleForm.position)
    });
    setModuleForm((current) => ({ ...emptyModule, courseId: current.courseId }));
    setLessonForm((current) => ({ ...current, moduleId: String(data.id) }));
    setMessage('Module added.');
    refreshAll(moduleForm.courseId);
  }

  async function addLesson(e) {
    e.preventDefault();
    await api.post(`/instructor/modules/${lessonForm.moduleId}/lessons`, {
      title: lessonForm.title,
      content: lessonForm.content,
      videoUrl: lessonForm.videoUrl,
      resourceUrl: lessonForm.resourceUrl,
      position: Number(lessonForm.position)
    });
    setLessonForm((current) => ({ ...emptyLesson, moduleId: current.moduleId }));
    setMessage('Lesson added.');
    refreshAll(selectedCourseId);
  }

  async function createQuiz(e) {
    e.preventDefault();
    await api.post(`/instructor/courses/${quizForm.courseId}/quizzes`, {
      title: quizForm.title,
      questions: quizForm.questions
    });
    setQuizForm((current) => ({ ...emptyQuiz, courseId: current.courseId }));
    setMessage('Quiz created.');
    refreshAll(selectedCourseId);
  }

  function updateQuestion(index, field, value) {
    setQuizForm((current) => ({
      ...current,
      questions: current.questions.map((question, qIndex) =>
        qIndex === index ? { ...question, [field]: value } : question
      )
    }));
  }

  function updateQuestionOption(questionIndex, optionIndex, value) {
    setQuizForm((current) => ({
      ...current,
      questions: current.questions.map((question, qIndex) =>
        qIndex === questionIndex
          ? { ...question, options: question.options.map((option, oIndex) => (oIndex === optionIndex ? value : option)) }
          : question
      )
    }));
  }

  function addQuestion() {
    setQuizForm((current) => ({
      ...current,
      questions: [...current.questions, { text: '', options: ['', '', '', ''], correctAnswer: '' }]
    }));
  }

  return (
    <div className="container py-4">
      <h1 className="h3">Instructor Dashboard</h1>
      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="row g-3">
        <div className="col-lg-5">
          <form className="panel" onSubmit={createCourse}>
            <h2 className="h5">Create Course</h2>
            <input className="form-control mb-2" placeholder="Title" value={courseForm.title} onChange={(e) => setCourseForm({ ...courseForm, title: e.target.value })} />
            <textarea className="form-control mb-2" placeholder="Description" value={courseForm.description} onChange={(e) => setCourseForm({ ...courseForm, description: e.target.value })} />
            <input className="form-control mb-2" placeholder="Category" value={courseForm.category} onChange={(e) => setCourseForm({ ...courseForm, category: e.target.value })} />
            <input className="form-control mb-2" placeholder="Level" value={courseForm.level} onChange={(e) => setCourseForm({ ...courseForm, level: e.target.value })} />
            <input className="form-control mb-2" placeholder="Thumbnail URL" value={courseForm.thumbnailUrl} onChange={(e) => setCourseForm({ ...courseForm, thumbnailUrl: e.target.value })} />
            <select className="form-select mb-2" value={courseForm.pricingType} onChange={(e) => setCourseForm({ ...courseForm, pricingType: e.target.value, price: e.target.value === 'FREE' ? 0 : courseForm.price })}>
              <option value="FREE">Free Course</option>
              <option value="PAID">Paid Course</option>
            </select>
            <input
              className="form-control mb-2"
              type="number"
              min="0"
              placeholder="Course fee"
              value={courseForm.price}
              disabled={courseForm.pricingType === 'FREE'}
              onChange={(e) => setCourseForm({ ...courseForm, price: e.target.value })}
            />
            <button className="btn btn-primary w-100">Submit Course</button>
          </form>

          <form className="panel mt-3" onSubmit={addModule}>
            <h2 className="h5">Add Module</h2>
            <select className="form-select mb-2" value={moduleForm.courseId} onChange={(e) => pickCourse(e.target.value)}>
              <option value="">Select course</option>
              {courses.map((course) => <option key={course.id} value={course.id}>{course.title}</option>)}
            </select>
            <input className="form-control mb-2" placeholder="Module title" value={moduleForm.title} onChange={(e) => setModuleForm({ ...moduleForm, title: e.target.value })} />
            <input className="form-control mb-2" type="number" placeholder="Position" value={moduleForm.position} onChange={(e) => setModuleForm({ ...moduleForm, position: e.target.value })} />
            <button className="btn btn-outline-primary w-100">Add Module</button>
          </form>

          <form className="panel mt-3" onSubmit={addLesson}>
            <h2 className="h5">Add Lesson</h2>
            <select className="form-select mb-2" value={lessonForm.moduleId} onChange={(e) => setLessonForm({ ...lessonForm, moduleId: e.target.value })}>
              <option value="">Select module</option>
              {selectedCourseDetail?.modules.map((module) => (
                <option key={module.id} value={module.id}>{module.position}. {module.title}</option>
              ))}
            </select>
            <input className="form-control mb-2" placeholder="Lesson title" value={lessonForm.title} onChange={(e) => setLessonForm({ ...lessonForm, title: e.target.value })} />
            <textarea className="form-control mb-2" placeholder="Lesson content" value={lessonForm.content} onChange={(e) => setLessonForm({ ...lessonForm, content: e.target.value })} />
            <input className="form-control mb-2" placeholder="Video URL" value={lessonForm.videoUrl} onChange={(e) => setLessonForm({ ...lessonForm, videoUrl: e.target.value })} />
            <input className="form-control mb-2" placeholder="Resource URL" value={lessonForm.resourceUrl} onChange={(e) => setLessonForm({ ...lessonForm, resourceUrl: e.target.value })} />
            <button className="btn btn-outline-primary w-100">Add Lesson</button>
          </form>

          <form className="panel mt-3" onSubmit={createQuiz}>
            <h2 className="h5">Create Quiz</h2>
            <select className="form-select mb-2" value={quizForm.courseId} onChange={(e) => pickCourse(e.target.value)}>
              <option value="">Select course</option>
              {courses.map((course) => <option key={course.id} value={course.id}>{course.title}</option>)}
            </select>
            <input className="form-control mb-2" placeholder="Quiz title" value={quizForm.title} onChange={(e) => setQuizForm({ ...quizForm, title: e.target.value })} />
            {quizForm.questions.map((question, index) => (
              <div className="quiz-builder" key={index}>
                <input className="form-control mb-2" placeholder={`Question ${index + 1}`} value={question.text} onChange={(e) => updateQuestion(index, 'text', e.target.value)} />
                {question.options.map((option, optionIndex) => (
                  <input
                    key={optionIndex}
                    className="form-control mb-2"
                    placeholder={`Option ${optionIndex + 1}`}
                    value={option}
                    onChange={(e) => updateQuestionOption(index, optionIndex, e.target.value)}
                  />
                ))}
                <input className="form-control mb-2" placeholder="Correct answer" value={question.correctAnswer} onChange={(e) => updateQuestion(index, 'correctAnswer', e.target.value)} />
              </div>
            ))}
            <button type="button" className="btn btn-outline-secondary w-100 mb-2" onClick={addQuestion}>Add Another Question</button>
            <button className="btn btn-outline-primary w-100">Create Quiz</button>
          </form>
        </div>

        <div className="col-lg-7">
          <div className="panel">
            <h2 className="h5">My Courses</h2>
            {courses.length === 0 && <p className="text-muted mb-0">No courses yet.</p>}
            {courses.map((course) => (
              <button
                type="button"
                className="dashboard-row dashboard-button"
                key={course.id}
                onClick={() => pickCourse(String(course.id))}
              >
                <div>
                  <strong>{course.title}</strong>
                  <p className="text-muted mb-0">{course.status} - {course.moduleCount} modules</p>
                </div>
                <span className="badge text-bg-light">{course.category}</span>
              </button>
            ))}
          </div>

          {selectedCourseDetail && (
            <div className="panel mt-3">
              <h2 className="h5">{selectedCourseDetail.course.title}</h2>
              {selectedCourseDetail.modules.length === 0 && <p className="text-muted mb-0">No modules added yet.</p>}
              {selectedCourseDetail.modules.map((module) => (
                <div className="dashboard-row" key={module.id}>
                  <div>
                    <strong>{module.position}. {module.title}</strong>
                    <p className="text-muted mb-0">{module.lessons.length} lessons</p>
                  </div>
                  <span className="badge text-bg-light">Module ID {module.id}</span>
                </div>
              ))}
            </div>
          )}

          {selectedCourseDetail && (
            <div className="panel mt-3">
              <h2 className="h5">Course Quizzes</h2>
              {selectedCourseQuizzes.length === 0 && <p className="text-muted mb-0">No quizzes added yet.</p>}
              {selectedCourseQuizzes.map((quiz) => (
                <div className="dashboard-row" key={quiz.id}>
                  <div>
                    <strong>{quiz.title}</strong>
                    <p className="text-muted mb-0">{quiz.questions.length} questions</p>
                  </div>
                  <span className="badge text-bg-light">Quiz</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
