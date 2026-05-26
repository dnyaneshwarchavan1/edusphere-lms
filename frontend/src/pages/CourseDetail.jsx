import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from '../context/AuthContext.jsx';

function normalizeVideoUrl(url) {
  if (!url) {
    return '';
  }

  try {
    const parsed = new URL(url);
    const host = parsed.hostname.replace(/^www\./, '');

    if (host === 'youtu.be') {
      const videoId = parsed.pathname.split('/').filter(Boolean)[0];
      return videoId ? `https://www.youtube.com/embed/${videoId}` : url;
    }

    if (host === 'youtube.com' || host === 'm.youtube.com') {
      if (parsed.pathname === '/watch') {
        const videoId = parsed.searchParams.get('v');
        return videoId ? `https://www.youtube.com/embed/${videoId}` : url;
      }

      if (parsed.pathname.startsWith('/embed/')) {
        return url;
      }
    }

    if (host === 'vimeo.com') {
      const videoId = parsed.pathname.split('/').filter(Boolean)[0];
      return videoId ? `https://player.vimeo.com/video/${videoId}` : url;
    }

    return url;
  } catch {
    return url;
  }
}

function isEmbedVideo(url) {
  if (!url) {
    return false;
  }

  return /youtube\.com\/embed|player\.vimeo\.com\/video/i.test(url);
}

function isDirectVideo(url) {
  if (!url) {
    return false;
  }

  return /\.(mp4|webm|ogg|mov)(\?.*)?$/i.test(url);
}

export default function CourseDetail() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const { user } = useAuth();
  const [detail, setDetail] = useState(null);
  const [quizzes, setQuizzes] = useState([]);
  const [enrollments, setEnrollments] = useState([]);
  const [answers, setAnswers] = useState({});
  const [message, setMessage] = useState('');
  const [submittingQuizId, setSubmittingQuizId] = useState(null);
  const [submittedQuizIds, setSubmittedQuizIds] = useState([]);
  const [activePayment, setActivePayment] = useState(null);
  const [paymentBusy, setPaymentBusy] = useState(false);
  const [paymentError, setPaymentError] = useState('');
  const [paymentReceipt, setPaymentReceipt] = useState(null);
  const [razorpayConfig, setRazorpayConfig] = useState(null);
  const selectedQuizId = Number(searchParams.get('quiz'));

  useEffect(() => {
    api.get(`/courses/${id}`).then((res) => setDetail(res.data));
  }, [id]);

  useEffect(() => {
    if (user?.role === 'STUDENT') {
      api.get(`/student/courses/${id}/quizzes`).then((res) => setQuizzes(res.data)).catch(() => setQuizzes([]));
      api.get('/student/enrollments').then((res) => setEnrollments(res.data)).catch(() => setEnrollments([]));
    }
  }, [id, user]);

  useEffect(() => {
    const coursePrice = Number(detail?.course?.price || 0);
    if (user?.role !== 'STUDENT' || coursePrice <= 0 || enrollments.some((item) => item.courseId === detail?.course?.id)) {
      return;
    }

    api.get('/student/payments/razorpay-config')
      .then((res) => setRazorpayConfig(res.data))
      .catch((error) => setPaymentError(error.response?.data?.message || 'Unable to load Razorpay checkout.'));
  }, [detail, enrollments, user]);

  async function enroll() {
    await api.post(`/student/courses/${id}/enroll`);
    api.get('/student/enrollments').then((res) => setEnrollments(res.data)).catch(() => setEnrollments([]));
    setMessage('Enrollment successful. Open your student dashboard to continue.');
  }

  async function startCheckout() {
    if (!razorpayConfig?.keyId) {
      setPaymentError('Razorpay configuration is not available.');
      return;
    }

    setPaymentBusy(true);
    setPaymentError('');
    try {
      const { data } = await api.post(`/student/courses/${id}/checkout`, {});
      setActivePayment(data);
      await openRazorpayCheckout(data, razorpayConfig.keyId);
      setMessage(data.message);
    } catch (error) {
      setPaymentError(error.response?.data?.message || error.message || 'Unable to start payment.');
      setPaymentBusy(false);
    }
  }

  async function openRazorpayCheckout(checkout, keyId) {
    await loadRazorpayScript();
    if (!window.Razorpay) {
      throw new Error('Razorpay Checkout could not be loaded.');
    }

    const razorpay = new window.Razorpay({
      key: keyId,
      amount: checkout.amountInSubunits,
      currency: checkout.currency,
      name: 'EduSphere LMS',
      description: `Course payment for ${checkout.courseTitle}`,
      order_id: checkout.razorpayOrderId,
      handler: async (response) => {
        setPaymentError('');
        try {
          const { data } = await api.post(`/student/payments/${checkout.paymentId}/verify`, {
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature
          });
          setPaymentReceipt(data);
          setActivePayment(null);
          api.get('/student/enrollments').then((res) => setEnrollments(res.data)).catch(() => setEnrollments([]));
          setMessage(data.message);
        } catch (error) {
          setPaymentError(error.response?.data?.message || 'Payment verification failed.');
        } finally {
          setPaymentBusy(false);
        }
      },
      modal: {
        ondismiss: () => {
          setPaymentBusy(false);
        }
      },
      prefill: {
        name: user?.name || '',
        email: user?.email || ''
      },
      theme: {
        color: '#2563eb'
      }
    });

    razorpay.on('payment.failed', async (response) => {
      try {
        const failure = response.error || {};
        const { data } = await api.post(`/student/payments/${checkout.paymentId}/fail`, {
          razorpayOrderId: checkout.razorpayOrderId,
          razorpayPaymentId: failure.metadata?.payment_id || '',
          code: failure.code || '',
          description: failure.description || '',
          source: failure.source || '',
          step: failure.step || '',
          reason: failure.reason || ''
        });
        setActivePayment(data);
        setPaymentReceipt(null);
        setPaymentError(data.failureReason || 'Payment failed. Enrollment was not activated.');
      } catch (error) {
        setPaymentError(error.response?.data?.message || 'Payment failed and the failure could not be recorded.');
      } finally {
        setPaymentBusy(false);
      }
    });

    razorpay.open();
  }

  function loadRazorpayScript() {
    if (window.Razorpay) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      const existing = document.querySelector('script[data-razorpay-checkout="true"]');
      if (existing) {
        if (window.Razorpay) {
          resolve();
          return;
        }
        existing.addEventListener('load', resolve, { once: true });
        existing.addEventListener('error', () => reject(new Error('Unable to load Razorpay Checkout.')), { once: true });
        return;
      }

      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.async = true;
      script.dataset.razorpayCheckout = 'true';
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Unable to load Razorpay Checkout.'));
      document.body.appendChild(script);
    });
  }

  async function completeLesson(lessonId) {
    const { data } = await api.patch(`/student/lessons/${lessonId}/complete`);
    setMessage(`Lesson completed. Course progress: ${data.progressPercent}%`);
  }

  async function submitQuiz(quizId) {
    setSubmittingQuizId(quizId);
    try {
      const { data } = await api.post(`/student/quizzes/${quizId}/submit`, { answers });
      setSubmittedQuizIds((current) => current.includes(quizId) ? current : [...current, quizId]);
      setMessage(`Quiz submitted. Score: ${data.score}/${data.totalQuestions}`);
    } finally {
      setSubmittingQuizId(null);
    }
  }

  if (!detail) return <div className="container py-5">Loading...</div>;

  const isEnrolled = enrollments.some((item) => item.courseId === detail.course.id);
  const isPaidCourse = Number(detail.course.price || 0) > 0;
  const visibleQuizzes = selectedQuizId
    ? quizzes.filter((quiz) => quiz.id === selectedQuizId)
    : quizzes;

  return (
    <div className="container py-4">
      {message && <div className="alert alert-success">{message}</div>}
      <div className="row g-4">
        <div className="col-lg-7">
          <h1 className="h2">{detail.course.title}</h1>
          <p className="text-muted">{detail.course.description}</p>
          <div className="d-flex gap-2 mb-3 flex-wrap">
            <span className="badge text-bg-primary">{detail.course.category}</span>
            <span className="badge text-bg-light">{detail.course.level}</span>
            <span className="badge text-bg-light">{detail.course.instructorName}</span>
            <span className="badge text-bg-light">{`Rs ${Number(detail.course.price || 0)}`}</span>
          </div>
          {user?.role === 'STUDENT' && !isPaidCourse && !isEnrolled && <button className="btn btn-primary" onClick={enroll}>Enroll Now</button>}
          {user?.role === 'STUDENT' && isEnrolled && <button className="btn btn-outline-success" disabled>Enrolled</button>}
        </div>
        <div className="col-lg-5">
          <img className="detail-img" src={detail.course.thumbnailUrl} alt={detail.course.title} />
        </div>
      </div>

      {user?.role === 'STUDENT' && isPaidCourse && !isEnrolled && (
        <section className="panel mt-4 payment-panel">
          <div className="d-flex justify-content-between align-items-start flex-wrap gap-3">
            <div>
              <h2 className="h5 mb-1">Secure Course Checkout</h2>
              <p className="text-muted mb-0">Pay through Razorpay. Enrollment is activated only after the backend verifies a successful payment.</p>
            </div>
            <span className="payment-amount">{`Rs ${Number(detail.course.price || 0)}`}</span>
          </div>
          {paymentError && <div className="alert alert-danger mt-3 mb-0">{paymentError}</div>}
          <div className="checkout-card mt-3">
            <div className="small text-muted mb-2">Gateway</div>
            <strong className="d-block mb-2">Razorpay Secure Checkout</strong>
            <p className="text-muted mb-3">Use UPI, card, or net banking inside Razorpay&apos;s hosted checkout. We enroll the student only after signature verification succeeds on the backend.</p>
            <button className="btn btn-primary" disabled={paymentBusy || !razorpayConfig?.keyId} onClick={startCheckout}>
              {paymentBusy ? 'Opening Razorpay...' : 'Pay with Razorpay'}
            </button>
          </div>
          {activePayment && (
            <div className="checkout-card mt-3">
              <div className="small text-muted mb-2">Latest Payment Attempt</div>
              <strong className="d-block mb-2">{activePayment.reference}</strong>
              <p className="text-muted mb-1">Order ID: {activePayment.razorpayOrderId}</p>
              <p className="text-muted mb-0">Status: {activePayment.status}</p>
              {activePayment.failureReason && <p className="text-danger mt-2 mb-0">{activePayment.failureReason}</p>}
            </div>
          )}
        </section>
      )}

      {paymentReceipt && (
        <section className="panel mt-4 payment-receipt">
          <div className="d-flex justify-content-between align-items-start flex-wrap gap-3">
            <div>
              <h2 className="h5 mb-1">Payment Receipt</h2>
              <p className="text-muted mb-0">This receipt confirms that the student was enrolled only after successful payment authorization.</p>
            </div>
            <span className="payment-amount">{paymentReceipt.status}</span>
          </div>
          <div className="row g-3 mt-1">
            <div className="col-md-6">
              <div className="checkout-card">
                <div className="small text-muted">Course</div>
                <strong>{paymentReceipt.courseTitle}</strong>
                <div className="small text-muted mt-2">Reference: {paymentReceipt.reference}</div>
              </div>
            </div>
            <div className="col-md-6">
              <div className="checkout-card">
                <div className="small text-muted">Payment Summary</div>
                <strong>{`Rs ${Number(paymentReceipt.amount || 0)}`}</strong>
                <div className="small text-muted mt-2">{paymentReceipt.provider} - {paymentReceipt.status}</div>
                <div className="small text-muted">{paymentReceipt.paymentIdReference || paymentReceipt.razorpayOrderId}</div>
              </div>
            </div>
          </div>
        </section>
      )}

      <section className="mt-4">
        <h2 className="h4">Course Content</h2>
        {detail.modules.map((module) => (
          <div className="content-block" key={module.id}>
            <h3 className="h6 mb-3">{module.position}. {module.title}</h3>
            {module.lessons.map((lesson) => (
              <div className="lesson-row" key={lesson.id}>
                <div className="lesson-body">
                  <strong>{lesson.title}</strong>
                  <p className="text-muted mb-0">{lesson.content}</p>
                  {lesson.videoUrl && (
                    <div className="lesson-media mt-3">
                      {isEmbedVideo(normalizeVideoUrl(lesson.videoUrl)) ? (
                        <iframe
                          className="lesson-video-frame"
                          src={normalizeVideoUrl(lesson.videoUrl)}
                          title={`${lesson.title} video`}
                          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                          allowFullScreen
                        />
                      ) : isDirectVideo(lesson.videoUrl) ? (
                        <video className="lesson-video-player" controls preload="metadata">
                          <source src={lesson.videoUrl} />
                          Your browser does not support the video tag.
                        </video>
                      ) : (
                        <a
                          className="btn btn-sm btn-outline-primary"
                          href={lesson.videoUrl}
                          target="_blank"
                          rel="noreferrer"
                        >
                          Open Lesson Video
                        </a>
                      )}
                    </div>
                  )}
                  {lesson.resourceUrl && (
                    <div className="lesson-links mt-2">
                      <a
                        className="btn btn-sm btn-outline-secondary"
                        href={lesson.resourceUrl}
                        target="_blank"
                        rel="noreferrer"
                      >
                        Open Resource
                      </a>
                    </div>
                  )}
                </div>
                {user?.role === 'STUDENT' && (
                  <button className="btn btn-sm btn-outline-success" onClick={() => completeLesson(lesson.id)}>Complete</button>
                )}
              </div>
            ))}
          </div>
        ))}
      </section>

      {user?.role === 'STUDENT' && visibleQuizzes.length > 0 && (
        <section className="mt-4">
          <h2 className="h4">Quizzes</h2>
          {selectedQuizId && (
            <div className="d-flex justify-content-between align-items-center mb-3">
              <p className="text-muted mb-0">Showing the selected quiz for this course.</p>
              <Link className="btn btn-sm btn-outline-secondary" to={`/courses/${id}`}>View All Quizzes</Link>
            </div>
          )}
          {visibleQuizzes.map((quiz) => (
            <div className="content-block" key={quiz.id}>
              <h3 className="h6">{quiz.title}</h3>
              {quiz.questions.map((question) => (
                <div className="quiz-question" key={question.id}>
                  <strong>{question.text}</strong>
                  <div className="mt-2">
                    {question.options.map((option) => (
                      <label className="option-line" key={option}>
                        <input
                          type="radio"
                          name={`question-${question.id}`}
                          value={option}
                          onChange={() => setAnswers({ ...answers, [question.id]: option })}
                        />
                        {option}
                      </label>
                    ))}
                  </div>
                </div>
              ))}
              <button
                className={`btn mt-2 ${submittedQuizIds.includes(quiz.id) ? 'btn-outline-success quiz-submit-done' : 'btn-success'}`}
                disabled={submittingQuizId === quiz.id || submittedQuizIds.includes(quiz.id)}
                onClick={() => submitQuiz(quiz.id)}
              >
                {submittingQuizId === quiz.id ? 'Submitting...' : submittedQuizIds.includes(quiz.id) ? 'Submitted' : 'Submit Quiz'}
              </button>
            </div>
          ))}
        </section>
      )}
    </div>
  );
}
