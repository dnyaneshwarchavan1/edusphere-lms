import { Link, NavLink, useNavigate } from 'react-router-dom';
import { Bell, BookOpen, LayoutDashboard, LogOut, MessageSquare } from 'lucide-react';
import { useAuth } from '../context/AuthContext.jsx';
import { useNotifications } from '../context/NotificationContext.jsx';

export default function Navbar() {
  const { user, logout } = useAuth();
  const { unreadCount, connected } = useNotifications();
  const navigate = useNavigate();

  function dashboardPath() {
    if (user?.role === 'ADMIN') return '/admin';
    if (user?.role === 'INSTRUCTOR') return '/instructor';
    return '/student';
  }

  function handleLogout() {
    logout();
    navigate('/');
  }

  return (
    <nav className="navbar navbar-expand-lg bg-white border-bottom sticky-top">
      <div className="container">
        <Link className="navbar-brand fw-bold brand" to="/">
          <BookOpen size={22} /> EduSphere
        </Link>
        <div className="d-flex gap-2 align-items-center">
          <NavLink className="btn btn-sm btn-outline-secondary" to="/">Courses</NavLink>
          {user ? (
            <>
              <NavLink className="btn btn-sm btn-outline-secondary position-relative" to="/alerts">
                <Bell size={16} /> Alerts
                {unreadCount > 0 && <span className="notification-count">{unreadCount}</span>}
                <span className={`nav-live-indicator ${connected ? 'socket-live' : 'socket-idle'}`}>•</span>
              </NavLink>
              <NavLink className="btn btn-sm btn-outline-secondary" to="/chat">
                <MessageSquare size={16} /> Chat
              </NavLink>
              <NavLink className="btn btn-sm btn-primary" to={dashboardPath()}>
                <LayoutDashboard size={16} /> Dashboard
              </NavLink>
              <button className="btn btn-sm btn-outline-danger" onClick={handleLogout}>
                <LogOut size={16} /> Logout
              </button>
            </>
          ) : (
            <>
              <NavLink className="btn btn-sm btn-outline-primary" to="/login">Login</NavLink>
              <NavLink className="btn btn-sm btn-primary" to="/register">Register</NavLink>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
