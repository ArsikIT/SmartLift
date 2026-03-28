import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/client';
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

  useEffect(() => { fetchData(); }, [page]);

  const openCreate = () => {
    setEditId(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setShowModal(true);
  };

  const openEdit = (ev) => {
    setEditId(ev.id);
    setForm({
      liftId: ev.liftId,
      type: ev.type,
      description: ev.description,
      performedByUserId: ev.performedBy?.id || '',
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
      performedByUserId: form.performedByUserId ? Number(form.performedByUserId) : null,
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

  const formatDate = (d) => d ? new Date(d).toLocaleString() : '—';

  return (
    <div>
      <div className="page-header">
        <h2>{t('events.title')}</h2>
        <button className="btn btn-primary" onClick={openCreate}>{t('events.add')}</button>
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
              {items.map((ev) => (
                <tr key={ev.id}>
                  <td>{ev.liftSerialNumber}</td>
                  <td>
                    <span className={`badge ${TYPE_BADGES[ev.type] || 'badge-gray'}`}>
                      {t(`eventTypes.${ev.type}`)}
                    </span>
                  </td>
                  <td style={{ maxWidth: '250px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {ev.description}
                  </td>
                  <td>{ev.performedBy?.username || '—'}</td>
                  <td>{formatDate(ev.eventAt)}</td>
                  <td className="actions">
                    <button className="btn btn-secondary btn-sm" onClick={() => openEdit(ev)}>{t('common.edit')}</button>
                    <button className="btn btn-danger btn-sm" onClick={() => handleDelete(ev.id)}>{t('common.delete')}</button>
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
                <input name="liftId" type="number" value={form.liftId} onChange={handleChange} required />
              </label>
              <label>
                {t('events.type')} *
                <select name="type" value={form.type} onChange={handleChange}>
                  {EVENT_TYPES.map((et) => (
                    <option key={et} value={et}>{t(`eventTypes.${et}`)}</option>
                  ))}
                </select>
              </label>
              <label>
                {t('events.description')} *
                <textarea name="description" value={form.description} onChange={handleChange} required maxLength={500} />
              </label>
              <label>
                {t('events.performedById')}
                <input name="performedByUserId" type="number" value={form.performedByUserId} onChange={handleChange} />
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
