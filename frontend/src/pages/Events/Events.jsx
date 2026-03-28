import { useEffect, useState } from 'react';
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
    if (!confirm('Delete this event?')) return;
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
        <h2>Lift Events</h2>
        <button className="btn btn-primary" onClick={openCreate}>+ Add Event</button>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <p>Loading...</p>
      ) : items.length === 0 ? (
        <div className="empty-state">No events found</div>
      ) : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>Lift</th>
                <th>Type</th>
                <th>Description</th>
                <th>Performed By</th>
                <th>Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map((ev) => (
                <tr key={ev.id}>
                  <td>{ev.liftSerialNumber}</td>
                  <td>
                    <span className={`badge ${TYPE_BADGES[ev.type] || 'badge-gray'}`}>
                      {ev.type}
                    </span>
                  </td>
                  <td style={{ maxWidth: '250px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {ev.description}
                  </td>
                  <td>{ev.performedBy?.username || '—'}</td>
                  <td>{formatDate(ev.eventAt)}</td>
                  <td className="actions">
                    <button className="btn btn-secondary btn-sm" onClick={() => openEdit(ev)}>Edit</button>
                    <button className="btn btn-danger btn-sm" onClick={() => handleDelete(ev.id)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="pagination">
            <button disabled={page === 0} onClick={() => setPage(page - 1)}>Prev</button>
            <span>Page {page + 1} of {totalPages}</span>
            <button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>Next</button>
          </div>
        </>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>{editId ? 'Edit Event' : 'New Event'}</h3>
            {formError && <div className="error-msg">{formError}</div>}
            <form onSubmit={handleSave}>
              <label>
                Lift ID *
                <input name="liftId" type="number" value={form.liftId} onChange={handleChange} required />
              </label>
              <label>
                Type *
                <select name="type" value={form.type} onChange={handleChange}>
                  {EVENT_TYPES.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </label>
              <label>
                Description *
                <textarea name="description" value={form.description} onChange={handleChange} required maxLength={500} />
              </label>
              <label>
                Performed By User ID
                <input name="performedByUserId" type="number" value={form.performedByUserId} onChange={handleChange} />
              </label>
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? 'Saving...' : 'Save'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
