import { useEffect, useState } from 'react';
import api from '../api/client';
import { Link } from 'react-router-dom';
import { useNotifications } from '../context/NotificationContext.jsx';

export default function StudentDashboard() {
  const [enrollments, setEnrollments] = useState([]);
  const [attempts, setAttempts] = useState([]);
  const [availableQuizzes, setAvailableQuizzes] = useState([]);
  const [quizError, setQuizError] = useState('');
  const [publishedCourses, setPublishedCourses] = useState([]);
  const [payments, setPayments] = useState([]);
  const [certificates, setCertificates] = useState([]);
  const [certificateError, setCertificateError] = useState('');
  const [certificateBusyCourseId, setCertificateBusyCourseId] = useState(null);
  const [certificateDraft, setCertificateDraft] = useState({ courseId: null, courseTitle: '', studentFullName: '' });
  const { notifications } = useNotifications();

  useEffect(() => {
    api.get('/student/enrollments').then((res) => setEnrollments(res.data));
    api.get('/student/quiz-attempts').then((res) => setAttempts(res.data));
    api.get('/student/payments').then((res) => setPayments(res.data)).catch(() => setPayments([]));
    api.get('/student/certificates').then((res) => setCertificates(res.data)).catch(() => setCertificates([]));
    api.get('/courses').then((res) => setPublishedCourses(res.data)).catch(() => setPublishedCourses([]));
  }, []);

  useEffect(() => {
    async function loadQuizzes() {
      const enrolledCourseIds = new Set(enrollments.map((item) => item.courseId));
      const courseSource = publishedCourses.length > 0
        ? publishedCourses.map((course) => ({
            courseId: course.id,
            courseTitle: course.title,
            enrolled: enrolledCourseIds.has(course.id)
          }))
        : enrollments.map((item) => ({
            courseId: item.courseId,
            courseTitle: item.courseTitle,
            enrolled: true
          }));
      let hadRequestError = false;

      const results = await Promise.all(
        courseSource.map(async (item) => {
          try {
            const { data } = await api.get(`/student/courses/${item.courseId}/quizzes`);
            return data.map((quiz) => ({
              id: quiz.id,
              title: quiz.title,
              courseId: item.courseId,
              courseTitle: item.courseTitle,
              questionCount: quiz.questions.length,
              enrolled: item.enrolled
            }));
          } catch (error) {
            hadRequestError = true;
            return [];
          }
        })
      );
      setAvailableQuizzes(results.flat());
      setQuizError(hadRequestError ? 'Some quiz data could not be loaded.' : '');
    }

    async function loadQuizzesSafely() {
      try {
        await loadQuizzes();
      } catch (error) {
        setAvailableQuizzes([]);
        setQuizError('Unable to load quizzes right now.');
      }
    }

    if (enrollments.length > 0 || publishedCourses.length > 0) {
      loadQuizzesSafely();
    } else {
      setAvailableQuizzes([]);
      setQuizError('');
    }
  }, [enrollments, publishedCourses]);

  function downloadReceipt(payment) {
    const lines = [
      'EduSphere LMS Payment Receipt',
      '----------------------------',
      `Course: ${payment.courseTitle}`,
      `Amount: Rs ${Number(payment.amount || 0)}`,
      `Provider: ${payment.provider}`,
      `Status: ${payment.status}`,
      `Reference: ${payment.reference || '-'}`,
      `Payment ID: ${payment.paymentIdReference || '-'}`,
      `Created At: ${payment.createdAt ? new Date(payment.createdAt).toLocaleString() : '-'}`,
      `Paid At: ${payment.paidAt ? new Date(payment.paidAt).toLocaleString() : '-'}`,
      payment.failureReason ? `Failure Reason: ${payment.failureReason}` : ''
    ].filter(Boolean).join('\n');

    const blob = new Blob([lines], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `receipt-${payment.reference || payment.id}.txt`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  function openCertificateForm(courseId, courseTitle) {
    const existing = certificates.find((item) => item.courseId === courseId);
    setCertificateDraft({
      courseId,
      courseTitle,
      studentFullName: existing?.studentFullName || ''
    });
    setCertificateError('');
  }

  function closeCertificateForm() {
    setCertificateDraft({ courseId: null, courseTitle: '', studentFullName: '' });
  }

  async function generateCertificate(courseId) {
    setCertificateBusyCourseId(courseId);
    try {
      const { data } = await api.post(`/student/certificates/${courseId}`, {
        studentFullName: certificateDraft.studentFullName
      });
      setCertificates((current) => {
        const remaining = current.filter((item) => item.courseId !== data.courseId);
        return [data, ...remaining];
      });
      setCertificateError('');
      closeCertificateForm();
    } catch (error) {
      setCertificateError(error.response?.data?.message || 'Certificate could not be generated right now.');
    } finally {
      setCertificateBusyCourseId(null);
    }
  }

  async function downloadCertificate(courseId, courseTitle) {
    setCertificateBusyCourseId(courseId);
    try {
      const response = await api.get(`/student/certificates/${courseId}/download`, { responseType: 'blob' });
      const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `certificate-${courseTitle}`.replace(/[^a-z0-9-]+/gi, '-').toLowerCase() + '.pdf';
      anchor.click();
      URL.revokeObjectURL(url);
      setCertificateError('');
    } catch (error) {
      let message = 'Certificate download failed.';
      if (error.response?.data instanceof Blob) {
        try {
          const text = await error.response.data.text();
          const parsed = JSON.parse(text);
          message = parsed.message || message;
        } catch {
          message = 'Certificate download failed.';
        }
      } else if (error.response?.data?.message) {
        message = error.response.data.message;
      }
      setCertificateError(message);
    } finally {
      setCertificateBusyCourseId(null);
    }
  }

  const certificateCourseIds = new Set(certificates.map((item) => item.courseId));
  const completedCourses = enrollments.filter((item) => item.progressPercent >= 100);

  return (
    <div className="container py-4">
      <h1 className="h3">Student Dashboard</h1>
      <div className="row g-3 mt-1">
        <div className="col-lg-8">
          <div className="panel">
            <h2 className="h5">My Courses</h2>
            {enrollments.length === 0 && <p className="text-muted">No enrollments yet.</p>}
            {enrollments.map((item) => (
              <div className="dashboard-row" key={item.id}>
                <div>
                  <strong>{item.courseTitle}</strong>
                  {item.progressPercent >= 100 && <div className="small text-success mt-1">Course completed</div>}
                  <div className="progress mt-2">
                    <div className="progress-bar" style={{ width: `${item.progressPercent}%` }}>{item.progressPercent}%</div>
                  </div>
                </div>
                <div className="d-flex flex-column align-items-end gap-2">
                  <Link className="btn btn-sm btn-outline-primary" to={`/courses/${item.courseId}`}>Continue</Link>
                  {item.progressPercent >= 100 && (
                    certificateCourseIds.has(item.courseId) ? (
                      <div className="d-flex flex-column gap-2">
                        <button
                          className="btn btn-sm btn-outline-success"
                          onClick={() => downloadCertificate(item.courseId, item.courseTitle)}
                          disabled={certificateBusyCourseId === item.courseId}
                        >
                          {certificateBusyCourseId === item.courseId ? 'Preparing...' : 'Download Certificate'}
                        </button>
                        <button
                          className="btn btn-sm btn-outline-secondary"
                          onClick={() => openCertificateForm(item.courseId, item.courseTitle)}
                        >
                          Update Name
                        </button>
                      </div>
                    ) : (
                      <button
                        className="btn btn-sm btn-success"
                        onClick={() => openCertificateForm(item.courseId, item.courseTitle)}
                        disabled={certificateBusyCourseId === item.courseId}
                      >
                        Generate Certificate
                      </button>
                    )
                  )}
                </div>
              </div>
            ))}
          </div>

          <div className="panel mt-3">
            <h2 className="h5">Recent Alerts</h2>
            {notifications.length === 0 && <p className="text-muted">No alerts yet.</p>}
            {notifications.slice(0, 4).map((notification) => (
              <div className="small-stat" key={notification.id}>
                <div>
                  <span>{notification.title}</span>
                  <div className="small text-muted">{notification.message}</div>
                </div>
                <Link className="btn btn-sm btn-outline-secondary" to={notification.link || '/student'}>Open</Link>
              </div>
            ))}
          </div>
        </div>

        <div className="col-lg-4">
          <div className="panel">
            <h2 className="h5">Available Quizzes</h2>
            <p className="text-muted small">Published course quizzes appear here. Enroll in the course to take and track the quiz from your learning flow.</p>
            {quizError && <p className="text-danger">{quizError}</p>}
            {availableQuizzes.length === 0 && <p className="text-muted">No quizzes available yet.</p>}
            {availableQuizzes.map((quiz) => (
              <div className="small-stat" key={`available-${quiz.id}`}>
                <div>
                  <span>{quiz.title}</span>
                  <div className="small text-muted">
                    {quiz.courseTitle} - {quiz.questionCount} questions - {quiz.enrolled ? 'Enrolled' : 'Enroll to attempt'}
                  </div>
                </div>
                <Link className="btn btn-sm btn-outline-primary" to={`/courses/${quiz.courseId}?quiz=${quiz.id}`}>Open</Link>
              </div>
            ))}
          </div>

          <div className="panel mt-3">
            <h2 className="h5">Quiz Attempts</h2>
            {attempts.length === 0 && <p className="text-muted">No quiz attempts.</p>}
            {attempts.map((attempt) => (
              <div className="small-stat" key={attempt.id}>
                <span>{attempt.quizTitle}</span>
                <strong>{attempt.score}/{attempt.totalQuestions}</strong>
              </div>
            ))}
          </div>

          <div className="panel mt-3">
            <h2 className="h5">Payment History</h2>
            {payments.length === 0 && <p className="text-muted">No payments yet.</p>}
            {payments.slice(0, 4).map((payment) => (
              <div className="small-stat" key={payment.id}>
                <div>
                  <span>{payment.courseTitle}</span>
                  <div className="small text-muted">{payment.provider} - {payment.status}</div>
                  {payment.failureReason && <div className="small text-danger">{payment.failureReason}</div>}
                </div>
                <div className="d-flex flex-column align-items-end gap-2">
                  <strong>{`Rs ${Number(payment.amount || 0)}`}</strong>
                  <button className="btn btn-sm btn-outline-secondary" onClick={() => downloadReceipt(payment)}>Download</button>
                </div>
              </div>
            ))}
          </div>

          <div className="panel mt-3">
            <h2 className="h5">Certificates</h2>
            <p className="text-muted small">Certificates are available after the course reaches 100% completion and include the student name in the PDF.</p>
            {certificateError && <p className="text-danger">{certificateError}</p>}
            {certificateDraft.courseId && (
              <div className="certificate-form mt-3 mb-3">
                <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
                  <div>
                    <strong>{certificateDraft.courseTitle}</strong>
                    <div className="small text-muted">Enter the full student name exactly as it should appear on the certificate.</div>
                  </div>
                  <button className="btn btn-sm btn-outline-secondary" onClick={closeCertificateForm}>Close</button>
                </div>
                <label className="form-label mt-3">Student Full Name</label>
                <input
                  className="form-control"
                  value={certificateDraft.studentFullName}
                  onChange={(event) => setCertificateDraft((current) => ({ ...current, studentFullName: event.target.value }))}
                  placeholder="Enter full name"
                />
                <div className="d-flex gap-2 mt-3 flex-wrap">
                  <button
                    className="btn btn-success"
                    onClick={() => generateCertificate(certificateDraft.courseId)}
                    disabled={certificateBusyCourseId === certificateDraft.courseId || !certificateDraft.studentFullName.trim()}
                  >
                    {certificateBusyCourseId === certificateDraft.courseId ? 'Generating...' : 'Generate Certificate'}
                  </button>
                  <button className="btn btn-outline-secondary" onClick={closeCertificateForm}>Cancel</button>
                </div>
              </div>
            )}
            {certificates.map((certificate) => (
              <div className="small-stat" key={certificate.id}>
                <div>
                  <span>{certificate.courseTitle}</span>
                  <div className="small text-muted">
                    {certificate.studentFullName} - {certificate.certificateNumber}
                  </div>
                </div>
                <div className="d-flex flex-column gap-2">
                  <button
                    className="btn btn-sm btn-outline-success"
                    onClick={() => downloadCertificate(certificate.courseId, certificate.courseTitle)}
                    disabled={certificateBusyCourseId === certificate.courseId}
                  >
                    {certificateBusyCourseId === certificate.courseId ? 'Preparing...' : 'Download PDF'}
                  </button>
                  <button
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() => openCertificateForm(certificate.courseId, certificate.courseTitle)}
                  >
                    Edit Name
                  </button>
                </div>
              </div>
            ))}
            {certificates.length === 0 && completedCourses.length === 0 && (
              <p className="text-muted mb-0">Complete a course to unlock your certificate.</p>
            )}
            {certificates.length === 0 && completedCourses.length > 0 && (
              <div className="small text-muted">
                Completed courses are ready for certificate generation from the My Courses section.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
