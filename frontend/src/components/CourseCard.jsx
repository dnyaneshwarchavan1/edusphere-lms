import { Link } from 'react-router-dom';

export default function CourseCard({ course }) {
  return (
    <div className="course-card h-100">
      <img src={course.thumbnailUrl || 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1200&q=80'} alt={course.title} />
      <div className="p-3">
        <div className="d-flex justify-content-between mb-2">
          <span className="badge text-bg-light">{course.category || 'General'}</span>
          <span className="small text-muted">{course.level || 'All levels'}</span>
        </div>
        <h5>{course.title}</h5>
        <p className="text-muted line-clamp">{course.description}</p>
        <div className="d-flex justify-content-between align-items-center">
          <span className="fw-semibold">{`Rs ${Number(course.price || 0)}`}</span>
          <Link className="btn btn-sm btn-dark" to={`/courses/${course.id}`}>View</Link>
        </div>
      </div>
    </div>
  );
}
