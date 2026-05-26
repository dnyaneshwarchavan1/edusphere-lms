import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react';
import api from '../api/client';
import { useAuth } from './AuthContext.jsx';

const NotificationContext = createContext(null);

function notificationSocketUrl() {
  const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
  return apiBase.replace(/\/api\/?$/, '').replace(/^http/, 'ws') + '/ws/notifications';
}

export function NotificationProvider({ children }) {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [connected, setConnected] = useState(false);
  const socketRef = useRef(null);

  useEffect(() => {
    const token = localStorage.getItem('edusphere_token');
    if (!user || !token) {
      setNotifications([]);
      setUnreadCount(0);
      setConnected(false);
      socketRef.current?.close();
      socketRef.current = null;
      return;
    }

    api.get('/notifications').then((res) => setNotifications(res.data)).catch(() => setNotifications([]));
    api.get('/notifications/unread-count').then((res) => setUnreadCount(res.data)).catch(() => setUnreadCount(0));

    const socket = new WebSocket(`${notificationSocketUrl()}?token=${encodeURIComponent(token)}`);
    socketRef.current = socket;

    socket.onopen = () => setConnected(true);
    socket.onclose = () => setConnected(false);
    socket.onerror = () => setConnected(false);
    socket.onmessage = (event) => {
      try {
        const notification = JSON.parse(event.data);
        setNotifications((current) => [notification, ...current].slice(0, 20));
        setUnreadCount((current) => current + 1);
      } catch (error) {
        // Ignore malformed messages.
      }
    };

    return () => {
      socket.close();
    };
  }, [user]);

  async function markRead(id) {
    const { data } = await api.patch(`/notifications/${id}/read`);
    setNotifications((current) => current.map((item) => item.id === id ? data : item));
    setUnreadCount((current) => Math.max(0, current - 1));
    return data;
  }

  const value = useMemo(() => ({
    notifications,
    unreadCount,
    connected,
    markRead
  }), [notifications, unreadCount, connected]);

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

export function useNotifications() {
  return useContext(NotificationContext);
}
