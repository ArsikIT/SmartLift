import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/client';
import '../shared.css';

export default function Notifications() {
  const { t } = useTranslation();
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchData = async (p = page) => {
    setLoading(true);
    try {
      const data = await api.get(`/notifications?page=${p}&size=20`);
      setItems(data.content);
      setTotalPages(data.totalPages);
      setError('');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, [page]);

  const markAsRead = async (id) => {
    try {
      await api.patch(`/notifications/${id}/read`);
      setItems(items.map((n) => n.id === id ? { ...n, read: true } : n));
    } catch (err) {
      alert(err.message);
    }
  };

  const markAllAsRead = async () => {
    try {
      await api.patch('/notifications/read-all');
      setItems(items.map((n) => ({ ...n, read: true })));
    } catch (err) {
      alert(err.message);
    }
  };

  const formatDate = (d) => d ? new Date(d).toLocaleString() : '—';

  return (
    <div>
      <div className="page-header">
        <h2>{t('notifications.title')}</h2>
        <button className="btn btn-secondary" onClick={markAllAsRead}>{t('common.markAllRead')}</button>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <p>{t('common.loading')}</p>
      ) : items.length === 0 ? (
        <div className="empty-state">{t('notifications.empty')}</div>
      ) : (
        <>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {items.map((n) => (
              <div
                key={n.id}
                style={{
                  background: n.read ? '#fff' : '#f0f4ff',
                  border: n.read ? '1px solid #e5e7eb' : '1px solid #b3c6ff',
                  borderRadius: '8px',
                  padding: '14px 18px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                }}
              >
                <div>
                  <div style={{ fontWeight: n.read ? 400 : 600, fontSize: '14px', marginBottom: '4px' }}>
                    {n.title}
                  </div>
                  <div style={{ fontSize: '13px', color: '#666', marginBottom: '4px' }}>
                    {n.message}
                  </div>
                  <div style={{ fontSize: '12px', color: '#999' }}>
                    {formatDate(n.createdAt)}
                    {n.liftId && ` · ${t('nav.lifts')} #${n.liftId}`}
                    {n.maintenanceId && ` · ${t('nav.maintenances')} #${n.maintenanceId}`}
                  </div>
                </div>
                {!n.read && (
                  <button className="btn btn-secondary btn-sm" onClick={() => markAsRead(n.id)}>
                    {t('common.markRead')}
                  </button>
                )}
              </div>
            ))}
          </div>

          <div className="pagination">
            <button disabled={page === 0} onClick={() => setPage(page - 1)}>{t('common.prev')}</button>
            <span>{t('common.page', { current: page + 1, total: totalPages })}</span>
            <button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>{t('common.next')}</button>
          </div>
        </>
      )}
    </div>
  );
}
