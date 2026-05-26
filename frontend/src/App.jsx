import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar.jsx';
import Footer from './components/Footer.jsx';
import CourseCatalog from './pages/CourseCatalog.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import StudentDashboard from './pages/StudentDashboard.jsx';
import InstructorDashboard from './pages/InstructorDashboard.jsx';
import AdminDashboard from './pages/AdminDashboard.jsx';
import CourseDetail from './pages/CourseDetail.jsx';
import AlertsPage from './pages/AlertsPage.jsx';
import ChatPage from './pages/ChatPage.jsx';
import { useAuth } from './context/AuthContext.jsx';

function Protected({ children, roles }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <main className="app-shell">
        <Routes>
          <Route path="/" element={<CourseCatalog />} />
          <Route path="/courses/:id" element={<CourseDetail />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/student" element={<Protected roles={['STUDENT', 'ADMIN']}><StudentDashboard /></Protected>} />
          <Route path="/instructor" element={<Protected roles={['INSTRUCTOR', 'ADMIN']}><InstructorDashboard /></Protected>} />
          <Route path="/admin" element={<Protected roles={['ADMIN']}><AdminDashboard /></Protected>} />
          <Route path="/alerts" element={<Protected roles={['STUDENT', 'INSTRUCTOR', 'ADMIN']}><AlertsPage /></Protected>} />
          <Route path="/chat" element={<Protected roles={['STUDENT', 'INSTRUCTOR', 'ADMIN']}><ChatPage /></Protected>} />
        </Routes>
      </main>
      <Footer />
    </BrowserRouter>
  );
}
