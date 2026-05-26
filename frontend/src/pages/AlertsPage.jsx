import { Link } from 'react-router-dom';
import { useNotifications } from '../context/NotificationContext.jsx';

function timeLabel(value) {
  return new Date(value).toLocaleString();
}

export default function AlertsPage() {
  const { notifications, unreadCount, connected, markRead } = useNotifications();

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
        <div>
          <h1 className="h3 mb-1">Alerts</h1>
          <p className="text-muted mb-0">All system updates, payments, quizzes, and chat alerts in one place.</p>
        </div>
        <div className="d-flex align-items-center gap-3">
          <span className={connected ? 'socket-live' : 'socket-idle'}>{connected ? 'Live' : 'Offline'}</span>
          <span className="badge text-bg-light">{unreadCount} unread</span>
        </div>
      </div>

      <div className="panel mt-3">
        {notifications.length === 0 && <p className="text-muted mb-0">No notifications yet.</p>}
        {notifications.map((notification) => (
          <div className={`notification-item ${notification.read ? 'notification-read' : ''}`} key={notification.id}>
            <div className="d-flex justify-content-between gap-3 align-items-start flex-wrap">
              <div>
                <strong>{notification.title}</strong>
                <p className="mb-1 text-muted small">{notification.message}</p>
                <span className="small text-muted">{timeLabel(notification.createdAt)}</span>
              </div>
              <div className="d-flex align-items-center gap-2">
                {!notification.read && (
                  <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => markRead(notification.id)}>
                    Mark read
                  </button>
                )}
                {notification.link && (
                  <Link className="btn btn-sm btn-primary" to={notification.link}>
                    Open
                  </Link>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
