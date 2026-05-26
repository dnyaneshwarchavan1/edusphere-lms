import { useState } from 'react';
import { Bell, Circle } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useNotifications } from '../context/NotificationContext.jsx';

function timeLabel(value) {
  const date = new Date(value);
  return date.toLocaleString();
}

export default function NotificationBell() {
  const { notifications, unreadCount, connected, markRead } = useNotifications();
  const [open, setOpen] = useState(false);

  return (
    <div className="notification-shell">
      <button type="button" className="btn btn-sm btn-outline-secondary notification-trigger" onClick={() => setOpen((current) => !current)}>
        <Bell size={16} />
        Alerts
        {unreadCount > 0 && <span className="notification-count">{unreadCount}</span>}
        <Circle size={8} className={connected ? 'socket-live' : 'socket-idle'} fill="currentColor" />
      </button>

      {open && (
        <div className="notification-popover">
          <div className="notification-head">
            <div>
              <strong>Live Notifications</strong>
              <div className="small text-muted">{connected ? 'Real-time connection active' : 'Waiting for connection'}</div>
            </div>
          </div>

          {notifications.length === 0 && <p className="text-muted mb-0">No notifications yet.</p>}

          {notifications.map((notification) => (
            <div className={`notification-item ${notification.read ? 'notification-read' : ''}`} key={notification.id}>
              <div className="d-flex justify-content-between gap-2 align-items-start">
                <div>
                  <strong>{notification.title}</strong>
                  <p className="mb-1 text-muted small">{notification.message}</p>
                  <span className="small text-muted">{timeLabel(notification.createdAt)}</span>
                </div>
                {!notification.read && (
                  <button type="button" className="btn btn-sm btn-link p-0" onClick={() => markRead(notification.id)}>
                    Mark read
                  </button>
                )}
              </div>
              {notification.link && (
                <Link className="small text-decoration-none" to={notification.link} onClick={() => setOpen(false)}>
                  Open
                </Link>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
