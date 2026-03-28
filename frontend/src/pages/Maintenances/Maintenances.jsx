import { useEffect, useState } from 'react';
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
    if (!confirm('Delete this maintenance?')) return;
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
        <h2>Maintenances</h2>
        <button className="btn btn-primary" onClick={openCreate}>+ New Request</button>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <p>Loading...</p>
      ) : items.length === 0 ? (
        <div className="empty-state">No maintenances found</div>
      ) : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Lift</th>
                <th>Status</th>
                <th>Technician</th>
                <th>Requested</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map((m) => (
                <tr key={m.id}>
                  <td>{m.title}</td>
                  <td>{m.liftSerialNumber}</td>
                  <td>
                    <span className={`badge ${STATUS_BADGES[m.status] || 'badge-gray'}`}>
                      {m.status}
                    </span>
                  </td>
                  <td>{m.assignedTechnician?.username || '—'}</td>
                  <td>{formatDate(m.requestedAt)}</td>
                  <td className="actions">
                    <button className="btn btn-secondary btn-sm" onClick={() => openEdit(m)}>Edit</button>
                    <button className="btn btn-danger btn-sm" onClick={() => handleDelete(m.id)}>Delete</button>
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
            <h3>{editId ? 'Edit Maintenance' : 'New Maintenance'}</h3>
            {formError && <div className="error-msg">{formError}</div>}
            <form onSubmit={handleSave}>
              <label>
                Lift ID *
                <input name="liftId" type="number" value={form.liftId} onChange={handleChange} required />
              </label>
              <label>
                Title *
                <input name="title" value={form.title} onChange={handleChange} required />
              </label>
              <label>
                Description
                <textarea name="description" value={form.description} onChange={handleChange} />
              </label>
              <label>
                Status
                <select name="status" value={form.status} onChange={handleChange}>
                  {STATUSES.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
              </label>
              <label>
                Assigned Technician ID
                <input name="assignedTechnicianId" type="number" value={form.assignedTechnicianId} onChange={handleChange} />
              </label>
              <label>
                Requested By User ID
                <input name="requestedByUserId" type="number" value={form.requestedByUserId} onChange={handleChange} />
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
