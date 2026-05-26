import { useEffect, useState } from 'react';
import api from '../api/client';
import CourseCard from '../components/CourseCard.jsx';

export default function CourseCatalog() {
  const [courses, setCourses] = useState([]);

  useEffect(() => {
    api.get('/courses').then((res) => setCourses(res.data));
  }, []);

  return (
    <>
      <section className="hero-band">
        <div className="container py-5">
          <div className="row align-items-center g-4">
            <div className="col-lg-7">
              <h1>Learn job-ready skills with structured courses.</h1>
              <p>EduSphere gives students, instructors, and admins a complete learning workflow with courses, lessons, quizzes, progress, and approvals.</p>
            </div>
            <div className="col-lg-5">
              <img className="hero-img" src="https://images.unsplash.com/photo-1523580846011-d3a5bc25702b?auto=format&fit=crop&w=1200&q=80" alt="Students learning" />
            </div>
          </div>
        </div>
      </section>
      <section className="container py-4">
        <div className="d-flex justify-content-between align-items-end mb-3">
          <div>
            <h2 className="h4 mb-1">Published Courses</h2>
            <p className="text-muted mb-0">Start from the seeded Java full stack course or add more as an instructor.</p>
          </div>
        </div>
        <div className="row g-3">
          {courses.map((course) => (
            <div className="col-md-6 col-xl-4" key={course.id}>
              <CourseCard course={course} />
            </div>
          ))}
        </div>
      </section>
    </>
  );
}
