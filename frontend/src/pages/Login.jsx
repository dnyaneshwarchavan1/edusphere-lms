import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: location.state?.email || '', password: '' });
  const [error, setError] = useState('');
  const [success] = useState(location.state?.successMessage || '');

  async function submit(e) {
    e.preventDefault();
    try {
      const user = await login(form.email, form.password);
      navigate(user.role === 'ADMIN' ? '/admin' : user.role === 'INSTRUCTOR' ? '/instructor' : '/student');
    } catch (error) {
      if (!error.response) {
        setError('Backend server is not running. Start run-backend.bat, wait for Tomcat on port 8080, then try again.');
        return;
      }
      setError(error.response?.data?.message || 'Invalid email or password');
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={submit}>
        <h1 className="h4">Login</h1>
        {success && <div className="alert alert-success">{success}</div>}
        {error && <div className="alert alert-danger">{error}</div>}
        <label>Email</label>
        <input className="form-control" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <label>Password</label>
        <input className="form-control" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        <button className="btn btn-primary w-100 mt-3">Login</button>
        <p className="small text-muted mt-3 mb-0">Demo: admin@edusphere.com, instructor@edusphere.com, student@edusphere.com / password</p>
      </form>
    </div>
  );
}
