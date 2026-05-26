export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="container py-5">
        <div className="row g-4">
          <div className="col-lg-4">
            <h2 className="h5 mb-3">EduSphere LMS</h2>
            <p className="footer-copy">
              A practical learning platform for students, instructors, and admins to manage courses, lessons, quizzes, and progress in one system.
            </p>
          </div>
          <div className="col-6 col-lg-2">
            <h3 className="footer-title">Platform</h3>
            <ul className="footer-list">
              <li>Courses</li>
              <li>Learning Paths</li>
              <li>Quizzes</li>
              <li>Progress</li>
            </ul>
          </div>
          <div className="col-6 col-lg-3">
            <h3 className="footer-title">For Users</h3>
            <ul className="footer-list">
              <li>Students</li>
              <li>Instructors</li>
              <li>Administrators</li>
            </ul>
          </div>
          <div className="col-lg-3">
            <h3 className="footer-title">Built With</h3>
            <ul className="footer-list">
              <li>Spring Boot</li>
              <li>MySQL</li>
              <li>React</li>
              <li>Bootstrap</li>
            </ul>
          </div>
        </div>
      </div>
    </footer>
  );
}
