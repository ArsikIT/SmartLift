import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';

export default function NotificationBell() {
  const [count, setCount] = useState(0);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchCount = () => {
      api.get('/notifications/unread-count')
        .then(setCount)
        .catch(() => {});
    };

    fetchCount();
    const interval = setInterval(fetchCount, 30000);
    return () => clearInterval(interval);
  }, []);

  return (
    <button
      className="notification-bell"
      onClick={() => navigate('/notifications')}
      title="Notifications"
      style={{
        position: 'relative',
        background: 'none',
        border: 'none',
        cursor: 'pointer',
        fontSize: '20px',
        padding: '4px',
      }}
    >
      🔔
      {count > 0 && (
        <span
          style={{
            position: 'absolute',
            top: '-4px',
            right: '-6px',
            background: '#cf1322',
            color: '#fff',
            borderRadius: '50%',
            width: '18px',
            height: '18px',
            fontSize: '11px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 700,
          }}
        >
          {count > 99 ? '99+' : count}
        </span>
      )}
    </button>
  );
}
