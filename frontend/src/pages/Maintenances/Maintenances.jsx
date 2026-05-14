import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/client';
import { hasAnyRole, hasRole } from '../../auth/permissions';
import { useAuth } from '../../context/AuthContext';
import '../shared.css';

const STATUS_BADGES = {
  PENDING: 'badge-orange',
  IN_PROGRESS: 'badge-blue',
  DONE: 'badge-green',
};

const STATUSES = ['PENDING', 'IN_PROGRESS', 'DONE'];

const EMPTY_FORM = {
  liftId: '',
  title: '',
  description: '',
  status: 'PENDING',
  assignedTechnicianId: '',
  requestedByUserId: '',
};

export default function Maintenances() {
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
      const data = await api.get(`/maintenances?page=${p}&size=10&sort=requestedAt,desc`);
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
      requestedByUserId: isAdmin ? '' : String(auth.userId || ''),
    });
    setFormError('');
    setShowModal(true);
  };

  const openEdit = (maintenance) => {
    if (!canWrite) return;
    setEditId(maintenance.id);
    setForm({
      liftId: String(maintenance.liftId),
      title: maintenance.title,
      description: maintenance.description || '',
      status: maintenance.status,
      assignedTechnicianId: maintenance.assignedTechnician?.id ? String(maintenance.assignedTechnician.id) : '',
      requestedByUserId: maintenance.requestedBy?.id ? String(maintenance.requestedBy.id) : '',
    });
    setFormError('');
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');

    const assignedTechnicianId = isAdmin
      ? (form.assignedTechnicianId ? Number(form.assignedTechnicianId) : null)
      : (form.assignedTechnicianId ? Number(form.assignedTechnicianId) : (form.status !== 'PENDING' ? auth.userId : null));

    const requestedByUserId = isAdmin
      ? (form.requestedByUserId ? Number(form.requestedByUserId) : null)
      : (auth.userId || null);

    const body = {
      liftId: Number(form.liftId),
      title: form.title,
      description: form.description || null,
      status: form.status,
      assignedTechnicianId,
      requestedByUserId,
    };

    try {
      if (editId) {
        await api.put(`/maintenances/${editId}`, body);
      } else {
        await api.post('/maintenances', body);
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
    if (!confirm(t('maintenances.confirmDelete'))) return;
    try {
      await api.delete(`/maintenances/${id}`);
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
        <h2>{t('maintenances.title')}</h2>
        {canWrite && (
          <button className="btn btn-primary" onClick={openCreate}>{t('maintenances.add')}</button>
        )}
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <p>{t('common.loading')}</p>
      ) : items.length === 0 ? (
        <div className="empty-state">{t('maintenances.empty')}</div>
      ) : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>{t('maintenances.titleField')}</th>
                <th>{t('maintenances.lift')}</th>
                <th>{t('maintenances.status')}</th>
                <th>{t('maintenances.technician')}</th>
                <th>{t('maintenances.requested')}</th>
                <th>{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {items.map((maintenance) => (
                <tr key={maintenance.id}>
                  <td>{maintenance.title}</td>
                  <td>{maintenance.liftSerialNumber}</td>
                  <td>
                    <span className={`badge ${STATUS_BADGES[maintenance.status] || 'badge-gray'}`}>
                      {t(`maintenanceStatuses.${maintenance.status}`)}
                    </span>
                  </td>
                  <td>{maintenance.assignedTechnician?.username || '-'}</td>
                  <td>{formatDate(maintenance.requestedAt)}</td>
                  <td className="actions">
                    {canWrite && (
                      <button className="btn btn-secondary btn-sm" onClick={() => openEdit(maintenance)}>{t('common.edit')}</button>
                    )}
                    {canWrite && (
                      <button className="btn btn-danger btn-sm" onClick={() => handleDelete(maintenance.id)}>{t('common.delete')}</button>
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
            <h3>{editId ? t('maintenances.editTitle') : t('maintenances.new')}</h3>
            {formError && <div className="error-msg">{formError}</div>}
            <form onSubmit={handleSave}>
              <label>
                {t('maintenances.liftId')} *
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
                {t('maintenances.titleField')} *
                <input name="title" value={form.title} onChange={handleChange} required />
              </label>
              <label>
                {t('maintenances.description')}
                <textarea name="description" value={form.description} onChange={handleChange} />
              </label>
              <label>
                {t('maintenances.status')}
                <select name="status" value={form.status} onChange={handleChange}>
                  {STATUSES.map((status) => (
                    <option key={status} value={status}>{t(`maintenanceStatuses.${status}`)}</option>
                  ))}
                </select>
              </label>
              {isAdmin ? (
                <>
                  <label>
                    {t('maintenances.technicianId')}
                    <select name="assignedTechnicianId" value={form.assignedTechnicianId} onChange={handleChange}>
                      <option value="">- {t('common.noData')} -</option>
                      {users.map((user) => (
                        <option key={user.id} value={user.id}>{user.username}</option>
                      ))}
                    </select>
                  </label>
                  <label>
                    {t('maintenances.requestedById')}
                    <select name="requestedByUserId" value={form.requestedByUserId} onChange={handleChange}>
                      <option value="">- {t('common.noData')} -</option>
                      {users.map((user) => (
                        <option key={user.id} value={user.id}>{user.username}</option>
                      ))}
                    </select>
                  </label>
                </>
              ) : (
                <>
                  <label>
                    {t('maintenances.technician')}
                    <input value={auth.username || ''} disabled />
                  </label>
                  <label>
                    {t('maintenances.requestedBy')}
                    <input value={auth.username || ''} disabled />
                  </label>
                </>
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
