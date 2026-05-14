import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/client';
import { hasAnyRole, hasRole } from '../../auth/permissions';
import { useAuth } from '../../context/AuthContext';
import '../shared.css';

const EVENT_TYPES = ['CREATED', 'INSTALLED', 'ACTIVATED', 'FAULT', 'REPAIR', 'DECOMMISSIONED'];

const TYPE_BADGES = {
  CREATED: 'badge-gray',
  INSTALLED: 'badge-blue',
  ACTIVATED: 'badge-green',
  FAULT: 'badge-red',
  REPAIR: 'badge-orange',
  DECOMMISSIONED: 'badge-purple',
};

const EMPTY_FORM = {
  liftId: '',
  type: 'CREATED',
  description: '',
  performedByUserId: '',
};

export default function Events() {
  const { t } = useTranslation();
  const auth = useAuth();
  const canWrite = hasAnyRole(auth, ['ADMIN', 'SERVICE']);
  const isAdmin = hasRole(auth, 'ADMIN');
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [showModal, setShowModal] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const [lifts, setLifts] = useState([]);
  const [users, setUsers] = useState([]);

  const fetchData = async (p = page) => {
    setLoading(true);
    try {
      const data = await api.get(`/events?page=${p}&size=10&sort=eventAt,desc`);
      setItems(data.content);
      setTotalPages(data.totalPages);
      setError('');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchReferenceData = async () => {
    try {
      const liftsResponse = await api.get('/lifts?page=0&size=100&sort=createdAt,desc');
      setLifts(liftsResponse.content || []);
    } catch (err) {
      console.error('Failed to load lifts', err);
    }

    if (!isAdmin) {
      setUsers([]);
      return;
    }

    try {
      const usersResponse = await api.get('/users?page=0&size=100&sort=createdAt,desc');
      setUsers(usersResponse.content || []);
    } catch (err) {
      console.error('Failed to load users', err);
    }
  };

  useEffect(() => {
    fetchData();
  }, [page]);

  useEffect(() => {
    fetchReferenceData();
  }, [isAdmin]);

  const openCreate = () => {
    if (!canWrite) return;
    setEditId(null);
    setForm({
      ...EMPTY_FORM,
      performedByUserId: isAdmin ? '' : String(auth.userId || ''),
    });
    setFormError('');
    setShowModal(true);
  };

  const openEdit = (event) => {
    if (!canWrite) return;
    setEditId(event.id);
    setForm({
      liftId: String(event.liftId),
      type: event.type,
      description: event.description,
      performedByUserId: event.performedBy?.id ? String(event.performedBy.id) : '',
    });
    setFormError('');
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');

    const body = {
      liftId: Number(form.liftId),
      type: form.type,
      description: form.description,
      performedByUserId: isAdmin
        ? (form.performedByUserId ? Number(form.performedByUserId) : null)
        : (auth.userId || null),
    };

    try {
      if (editId) {
        await api.put(`/events/${editId}`, body);
      } else {
        await api.post('/events', body);
      }
      setShowModal(false);
      fetchData();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    if (!canWrite) return;
    if (!confirm(t('events.confirmDelete'))) return;
    try {
      await api.delete(`/events/${id}`);
      fetchData();
    } catch (err) {
      alert(err.message);
    }
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const formatDate = (value) => (value ? new Date(value).toLocaleString() : '-');

  return (
    <div>
      <div className="page-header">
        <h2>{t('events.title')}</h2>
        {canWrite && (
          <button className="btn btn-primary" onClick={openCreate}>{t('events.add')}</button>
        )}
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <p>{t('common.loading')}</p>
      ) : items.length === 0 ? (
        <div className="empty-state">{t('events.empty')}</div>
      ) : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>{t('events.lift')}</th>
                <th>{t('events.type')}</th>
                <th>{t('events.description')}</th>
                <th>{t('events.performedBy')}</th>
                <th>{t('events.date')}</th>
                <th>{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {items.map((event) => (
                <tr key={event.id}>
                  <td>{event.liftSerialNumber}</td>
                  <td>
                    <span className={`badge ${TYPE_BADGES[event.type] || 'badge-gray'}`}>
                      {t(`eventTypes.${event.type}`)}
                    </span>
                  </td>
                  <td style={{ maxWidth: '250px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {event.description}
                  </td>
                  <td>{event.performedBy?.username || '-'}</td>
                  <td>{formatDate(event.eventAt)}</td>
                  <td className="actions">
                    {canWrite && (
                      <button className="btn btn-secondary btn-sm" onClick={() => openEdit(event)}>{t('common.edit')}</button>
                    )}
                    {canWrite && (
                      <button className="btn btn-danger btn-sm" onClick={() => handleDelete(event.id)}>{t('common.delete')}</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="pagination">
            <button disabled={page === 0} onClick={() => setPage(page - 1)}>{t('common.prev')}</button>
            <span>{t('common.page', { current: page + 1, total: totalPages })}</span>
            <button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>{t('common.next')}</button>
          </div>
        </>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>{editId ? t('events.editTitle') : t('events.new')}</h3>
            {formError && <div className="error-msg">{formError}</div>}
            <form onSubmit={handleSave}>
              <label>
                {t('events.liftId')} *
                <select name="liftId" value={form.liftId} onChange={handleChange} required>
                  <option value="">- {t('common.noData')} -</option>
                  {lifts.map((lift) => (
                    <option key={lift.id} value={lift.id}>
                      {lift.serialNumber} - {lift.model}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                {t('events.type')} *
                <select name="type" value={form.type} onChange={handleChange}>
                  {EVENT_TYPES.map((eventType) => (
                    <option key={eventType} value={eventType}>{t(`eventTypes.${eventType}`)}</option>
                  ))}
                </select>
              </label>
              <label>
                {t('events.description')} *
                <textarea name="description" value={form.description} onChange={handleChange} required maxLength={500} />
              </label>
              {isAdmin ? (
                <label>
                  {t('events.performedById')}
                  <select name="performedByUserId" value={form.performedByUserId} onChange={handleChange}>
                    <option value="">- {t('common.noData')} -</option>
                    {users.map((user) => (
                      <option key={user.id} value={user.id}>{user.username}</option>
                    ))}
                  </select>
                </label>
              ) : (
                <label>
                  {t('events.performedBy')}
                  <input value={auth.username || ''} disabled />
                </label>
              )}
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>{t('common.cancel')}</button>
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? t('common.saving') : t('common.save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
