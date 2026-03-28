import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/client';
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

  useEffect(() => { fetchData(); }, [page]);

  const openCreate = () => {
    setEditId(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setShowModal(true);
  };

  const openEdit = (m) => {
    setEditId(m.id);
    setForm({
      liftId: m.liftId,
      title: m.title,
      description: m.description || '',
      status: m.status,
      assignedTechnicianId: m.assignedTechnician?.id || '',
      requestedByUserId: m.requestedBy?.id || '',
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
      title: form.title,
      description: form.description || null,
      status: form.status,
      assignedTechnicianId: form.assignedTechnicianId ? Number(form.assignedTechnicianId) : null,
      requestedByUserId: form.requestedByUserId ? Number(form.requestedByUserId) : null,
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

  const formatDate = (d) => d ? new Date(d).toLocaleString() : '—';

  return (
    <div>
      <div className="page-header">
        <h2>{t('maintenances.title')}</h2>
        <button className="btn btn-primary" onClick={openCreate}>{t('maintenances.add')}</button>
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
              {items.map((m) => (
                <tr key={m.id}>
                  <td>{m.title}</td>
                  <td>{m.liftSerialNumber}</td>
                  <td>
                    <span className={`badge ${STATUS_BADGES[m.status] || 'badge-gray'}`}>
                      {t(`maintenanceStatuses.${m.status}`)}
                    </span>
                  </td>
                  <td>{m.assignedTechnician?.username || '—'}</td>
                  <td>{formatDate(m.requestedAt)}</td>
                  <td className="actions">
                    <button className="btn btn-secondary btn-sm" onClick={() => openEdit(m)}>{t('common.edit')}</button>
                    <button className="btn btn-danger btn-sm" onClick={() => handleDelete(m.id)}>{t('common.delete')}</button>
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
                <input name="liftId" type="number" value={form.liftId} onChange={handleChange} required />
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
                  {STATUSES.map((s) => (
                    <option key={s} value={s}>{t(`maintenanceStatuses.${s}`)}</option>
                  ))}
                </select>
              </label>
              <label>
                {t('maintenances.technicianId')}
                <input name="assignedTechnicianId" type="number" value={form.assignedTechnicianId} onChange={handleChange} />
              </label>
              <label>
                {t('maintenances.requestedById')}
                <input name="requestedByUserId" type="number" value={form.requestedByUserId} onChange={handleChange} />
              </label>
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
