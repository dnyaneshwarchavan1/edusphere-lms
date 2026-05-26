import { useEffect, useState } from 'react';
import api from '../api/client';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';

export default function AdminDashboard() {
  const [analytics, setAnalytics] = useState({
    summary: {
      users: 0,
      courses: 0,
      enrollments: 0,
      activeCourses: 0,
      totalRevenue: 'Rs 0',
      quizAttempts: 0,
      averageQuizScore: '0.0%'
    },
    monthlyEnrollments: [],
    quizBreakdown: []
  });
  const [users, setUsers] = useState([]);
  const [courses, setCourses] = useState([]);
  const [enrollments, setEnrollments] = useState([]);
  const [courseQuizCounts, setCourseQuizCounts] = useState({});
  const [quizError, setQuizError] = useState('');
  const [analyticsError, setAnalyticsError] = useState('');

  function load() {
    api.get('/admin/analytics')
      .then((res) => {
        setAnalytics(res.data);
        setAnalyticsError('');
      })
      .catch(() => {
        setAnalyticsError('Analytics could not be loaded right now.');
      });
    api.get('/admin/users').then((res) => setUsers(res.data));
    api.get('/admin/enrollments').then((res) => setEnrollments(res.data)).catch(() => setEnrollments([]));
    api.get('/admin/courses').then(async (res) => {
      setCourses(res.data);
      try {
        const quizEntries = await Promise.all(
          res.data.map(async (course) => {
            const { data } = await api.get(`/admin/courses/${course.id}/quiz-count`);
            return [course.id, data];
          })
        );
        setCourseQuizCounts(Object.fromEntries(quizEntries));
        setQuizError('');
      } catch (error) {
        setCourseQuizCounts({});
        setQuizError('Unable to load quiz counts right now.');
      }
    });
  }

  useEffect(load, []);

  async function approve(id) {
    await api.patch(`/admin/courses/${id}/approve`);
    load();
  }

  const { summary, monthlyEnrollments, quizBreakdown } = analytics;

  const metricCards = [
    { label: 'Total Students', value: summary.users, note: 'Registered learners on the platform' },
    { label: 'Active Courses', value: summary.activeCourses, note: 'Published courses available now' },
    { label: 'Revenue', value: summary.totalRevenue, note: 'Successful payment collections' },
    { label: 'Quiz Attempts', value: summary.quizAttempts, note: 'Student quiz submissions recorded' },
    { label: 'Monthly Enrollments', value: monthlyEnrollments.reduce((total, item) => total + item.enrollments, 0), note: 'Last 6 months combined' },
    { label: 'Average Quiz Score', value: summary.averageQuizScore, note: 'Across all submitted quizzes' }
  ];

  const overviewBars = [
    { name: 'Students', value: summary.users },
    { name: 'Courses', value: summary.courses },
    { name: 'Active', value: summary.activeCourses },
    { name: 'Enrollments', value: summary.enrollments },
    { name: 'Quiz Attempts', value: summary.quizAttempts }
  ];

  const pieColors = ['#0f766e', '#2563eb', '#f59e0b'];
  const hasQuizData = quizBreakdown.some((item) => item.value > 0);

  return (
    <div className="container py-4">
      <h1 className="h3">Admin Dashboard</h1>
      <p className="text-muted mb-0">Previous admin controls stay here, with the new analytics view layered in on top.</p>
      {analyticsError && <div className="alert alert-warning mt-3 mb-0">{analyticsError}</div>}

      <div className="row g-3 mt-3">
        {metricCards.map((item) => (
          <div className="col-md-6 col-xl-4" key={item.label}>
            <div className="metric">
              <span>{item.label}</span>
              <strong>{item.value}</strong>
              <div className="analytics-caption">{item.note}</div>
            </div>
          </div>
        ))}
      </div>

      <div className="row g-3 mt-3">
        <div className="col-xl-5">
          <div className="panel">
            <div className="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-2">
              <div>
                <h2 className="h5 mb-1">Platform Overview</h2>
                <p className="text-muted mb-0">Students, courses, active catalog, enrollments, and quiz usage.</p>
              </div>
              <div className="payment-amount">{summary.totalRevenue}</div>
            </div>
            <div style={{ height: 280 }}>
              <ResponsiveContainer>
                <BarChart data={overviewBars} barSize={32}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="value" fill="#0f766e" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
        <div className="col-xl-7">
          <div className="panel">
            <div className="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-2">
              <div>
                <h2 className="h5 mb-1">Enrollment Trend</h2>
                <p className="text-muted mb-0">Monthly enrollments for the last six months.</p>
              </div>
              <span className="badge text-bg-light border">Total {summary.enrollments}</span>
            </div>
            <div style={{ height: 280 }}>
              <ResponsiveContainer>
                <LineChart data={monthlyEnrollments}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="month" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Line type="monotone" dataKey="enrollments" stroke="#2563eb" strokeWidth={3} dot={{ r: 4 }} activeDot={{ r: 6 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>

      <div className="row g-3 mt-1">
        <div className="col-lg-5">
          <div className="panel">
            <div className="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-2">
              <div>
                <h2 className="h5 mb-1">Quiz Statistics</h2>
                <p className="text-muted mb-0">Performance distribution from student attempts.</p>
              </div>
              <span className="badge text-bg-light border">{summary.averageQuizScore} average</span>
            </div>
            <div style={{ height: 280 }}>
              {hasQuizData ? (
                <ResponsiveContainer>
                  <PieChart>
                    <Pie data={quizBreakdown} dataKey="value" nameKey="label" innerRadius={60} outerRadius={95} paddingAngle={2}>
                      {quizBreakdown.map((entry, index) => (
                        <Cell key={entry.label} fill={pieColors[index % pieColors.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-100 d-flex align-items-center justify-content-center text-muted">
                  No quiz attempt data yet.
                </div>
              )}
            </div>
          </div>
        </div>
        <div className="col-lg-7">
          <div className="panel">
            <h2 className="h5">Course Approval</h2>
            <p className="text-muted">Pending and published courses stay manageable here, along with live quiz totals.</p>
            {quizError && <p className="text-danger">{quizError}</p>}
            {courses.map((course) => (
              <div className="dashboard-row" key={course.id}>
                <div>
                  <strong>{course.title}</strong>
                  <p className="text-muted mb-0">
                    {course.instructorName} - {course.status} - {courseQuizCounts[course.id] || 0} quizzes
                  </p>
                </div>
                {course.status !== 'PUBLISHED' && (
                  <button className="btn btn-sm btn-success" onClick={() => approve(course.id)}>Approve</button>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="panel mt-3">
        <h2 className="h5">Confirmed Enrollments</h2>
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th>Student</th>
                <th>Course</th>
                <th>Progress</th>
                <th>Payment</th>
                <th>Reference</th>
              </tr>
            </thead>
            <tbody>
              {enrollments.map((item) => (
                <tr key={item.enrollmentId}>
                  <td>
                    <strong>{item.studentName}</strong>
                    <div className="small text-muted">{item.studentEmail}</div>
                  </td>
                  <td>{item.courseTitle}</td>
                  <td>{item.progressPercent}%</td>
                  <td>{item.paymentStatus} ({item.paymentMethod})</td>
                  <td>{item.paymentReference}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {enrollments.length === 0 && <p className="text-muted mb-0">No enrolled students yet.</p>}
        </div>
      </div>

      <div className="panel mt-3">
        <h2 className="h5">Users</h2>
        <p className="text-muted">Operational user list stays available under the new analytics layer.</p>
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th></tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.name}</td>
                  <td>{user.email}</td>
                  <td>{user.role}</td>
                  <td>{user.enabled ? 'Active' : 'Disabled'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
