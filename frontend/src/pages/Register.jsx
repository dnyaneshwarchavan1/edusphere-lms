import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'STUDENT' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  async function submit(e) {
    e.preventDefault();
    try {
      await register(form);
      setError('');
      setSuccess('Registration successful. Please log in with your email and password.');
      setForm({ name: '', email: '', password: '', role: 'STUDENT' });
      setTimeout(() => {
        navigate('/login', {
          state: {
            successMessage: 'Account created successfully. Now log in to access the application.',
            email: form.email
          }
        });
      }, 700);
    } catch (err) {
      setSuccess('');
      if (!err.response) {
        setError('Backend server is not running. Start run-backend.bat first, then try again.');
        return;
      }
      setError(err.response?.data?.message || 'Registration failed');
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={submit}>
        <h1 className="h4">Create Account</h1>
        {success && <div className="alert alert-success">{success}</div>}
        {error && <div className="alert alert-danger">{error}</div>}
        <label>Name</label>
        <input className="form-control" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <label>Email</label>
        <input className="form-control" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <label>Password</label>
        <input className="form-control" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        <label>Role</label>
        <select className="form-select" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
          <option value="STUDENT">Student</option>
          <option value="INSTRUCTOR">Instructor</option>
        </select>
        <button className="btn btn-primary w-100 mt-3">Register</button>
      </form>
    </div>
  );
}
