import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('edusphere_user');
    return raw ? JSON.parse(raw) : null;
  });

  useEffect(() => {
    const token = localStorage.getItem('edusphere_token');
    if (token) {
      api.get('/auth/me').then((res) => setUser(res.data)).catch(() => logout());
    }
  }, []);

  async function login(email, password) {
    const { data } = await api.post('/auth/login', { email, password });
    localStorage.setItem('edusphere_token', data.token);
    localStorage.setItem('edusphere_user', JSON.stringify(data));
    setUser(data);
    return data;
  }

  async function register(payload) {
    const { data } = await api.post('/auth/register', payload);
    return data;
  }

  function logout() {
    localStorage.removeItem('edusphere_token');
    localStorage.removeItem('edusphere_user');
    setUser(null);
  }

  const value = useMemo(() => ({ user, login, register, logout }), [user]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
