import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import '../shared.css';

const STATUS_BADGES = {
  CREATED: 'badge-gray',
  INSTALLED: 'badge-blue',
  ACTIVE: 'badge-green',
  FAULTY: 'badge-red',
  IN_REPAIR: 'badge-orange',
  DECOMMISSIONED: 'badge-purple',
};

const EMPTY_FORM = {
  serialNumber: '',
  model: '',
  manufacturer: '',
  serviceOrganizationId: '',
  manufacturerOrganizationId: '',
  managementOrganizationId: '',
};

export default function Lifts() {
  const [lifts, setLifts] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [showModal, setShowModal] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const fetchLifts = async (p = page) => {
    setLoading(true);
    try {
      const data = await api.get(`/lifts?page=${p}&size=10&sort=createdAt,desc`);
      setLifts(data.content);
      setTotalPages(data.totalPages);
      setError('');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchLifts(); }, [page]);

  const openCreate = () => {
    setEditId(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setShowModal(true);
  };

  const openEdit = (lift) => {
    setEditId(lift.id);
    setForm({
      serialNumber: lift.serialNumber,
      model: lift.model,
      manufacturer: lift.manufacturer || '',
      serviceOrganizationId: lift.serviceOrganization?.id || '',
      manufacturerOrganizationId: lift.manufacturerOrganization?.id || '',
      managementOrganizationId: lift.managementOrganization?.id || '',
    });
    setFormError('');
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');

    const body = {
      serialNumber: form.serialNumber,
      model: form.model,
      manufacturer: form.manufacturer || null,
      serviceOrganizationId: form.serviceOrganizationId ? Number(form.serviceOrganizationId) : null,
      manufacturerOrganizationId: form.manufacturerOrganizationId ? Number(form.manufacturerOrganizationId) : null,
      managementOrganizationId: form.managementOrganizationId ? Number(form.managementOrganizationId) : null,
    };

    try {
      if (editId) {
        await api.put(`/lifts/${editId}`, body);
      } else {
        await api.post('/lifts', body);
      }
      setShowModal(false);
      fetchLifts();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this lift?')) return;
    try {
      await api.delete(`/lifts/${id}`);
      fetchLifts();
    } catch (err) {
      alert(err.message);
    }
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  return (
    <div>
      <div className="page-header">
        <h2>Lifts</h2>
        <button className="btn btn-primary" onClick={openCreate}>+ Add Lift</button>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <p>Loading...</p>
      ) : lifts.length === 0 ? (
        <div className="empty-state">No lifts found</div>
      ) : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>Serial Number</th>
                <th>Model</th>
                <th>Manufacturer</th>
                <th>Status</th>
                <th>Service Org</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {lifts.map((lift) => (
                <tr key={lift.id}>
                  <td>{lift.serialNumber}</td>
                  <td>{lift.model}</td>
                  <td>{lift.manufacturer || '—'}</td>
                  <td>
                    <span className={`badge ${STATUS_BADGES[lift.status] || 'badge-gray'}`}>
                      {lift.status}
                    </span>
                  </td>
                  <td>{lift.serviceOrganization?.name || '—'}</td>
                  <td className="actions">
                    <button className="btn btn-secondary btn-sm" onClick={() => openEdit(lift)}>Edit</button>
                    <button className="btn btn-danger btn-sm" onClick={() => handleDelete(lift.id)}>Delete</button>
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
            <h3>{editId ? 'Edit Lift' : 'New Lift'}</h3>
            {formError && <div className="error-msg">{formError}</div>}
            <form onSubmit={handleSave}>
              <label>
                Serial Number *
                <input name="serialNumber" value={form.serialNumber} onChange={handleChange} required />
              </label>
              <label>
                Model *
                <input name="model" value={form.model} onChange={handleChange} required />
              </label>
              <label>
                Manufacturer
                <input name="manufacturer" value={form.manufacturer} onChange={handleChange} />
              </label>
              <label>
                Service Organization ID
                <input name="serviceOrganizationId" type="number" value={form.serviceOrganizationId} onChange={handleChange} />
              </label>
              <label>
                Manufacturer Organization ID
                <input name="manufacturerOrganizationId" type="number" value={form.manufacturerOrganizationId} onChange={handleChange} />
              </label>
              <label>
                Management Organization ID
                <input name="managementOrganizationId" type="number" value={form.managementOrganizationId} onChange={handleChange} />
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
