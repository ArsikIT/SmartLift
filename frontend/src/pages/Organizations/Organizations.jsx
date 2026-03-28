import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import '../shared.css';

const ORG_TYPES = ['MANUFACTURER', 'SERVICE', 'MANAGEMENT'];

const TYPE_BADGES = {
  MANUFACTURER: 'badge-purple',
  SERVICE: 'badge-blue',
  MANAGEMENT: 'badge-green',
};

export default function Organizations() {
  const [org, setOrg] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState({
    name: '',
    type: 'SERVICE',
    address: '',
    contactEmail: '',
    contactPhone: '',
  });
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const data = await api.get('/organizations/me');
      setOrg(data);
      setError('');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const openEdit = () => {
    setForm({
      name: org.name,
      type: org.type,
      address: org.address || '',
      contactEmail: org.contactEmail || '',
      contactPhone: org.contactPhone || '',
    });
    setFormError('');
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');

    try {
      await api.put('/organizations/me', {
        name: form.name,
        type: form.type,
        address: form.address || null,
        contactEmail: form.contactEmail || null,
        contactPhone: form.contactPhone || null,
      });
      setShowModal(false);
      fetchData();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  return (
    <div>
      <div className="page-header">
        <h2>My Organization</h2>
        {org && <button className="btn btn-primary" onClick={openEdit}>Edit</button>}
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <p>Loading...</p>
      ) : !org ? (
        <div className="empty-state">No organization found</div>
      ) : (
        <div style={{ background: '#fff', borderRadius: '8px', padding: '24px', boxShadow: '0 1px 4px rgba(0,0,0,0.06)' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <tbody>
              {[
                ['Name', org.name],
                ['Type', <span className={`badge ${TYPE_BADGES[org.type] || 'badge-gray'}`}>{org.type}</span>],
                ['Address', org.address || '—'],
                ['Email', org.contactEmail || '—'],
                ['Phone', org.contactPhone || '—'],
                ['Created', new Date(org.createdAt).toLocaleString()],
              ].map(([label, value], i) => (
                <tr key={i}>
                  <td style={{ padding: '10px 16px', fontWeight: 600, color: '#555', width: '150px', borderBottom: '1px solid #f0f0f0' }}>{label}</td>
                  <td style={{ padding: '10px 16px', borderBottom: '1px solid #f0f0f0' }}>{value}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>Edit Organization</h3>
            {formError && <div className="error-msg">{formError}</div>}
            <form onSubmit={handleSave}>
              <label>
                Name *
                <input name="name" value={form.name} onChange={handleChange} required />
              </label>
              <label>
                Type *
                <select name="type" value={form.type} onChange={handleChange}>
                  {ORG_TYPES.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </label>
              <label>
                Address
                <input name="address" value={form.address} onChange={handleChange} />
              </label>
              <label>
                Contact Email
                <input name="contactEmail" type="email" value={form.contactEmail} onChange={handleChange} />
              </label>
              <label>
                Contact Phone
                <input name="contactPhone" value={form.contactPhone} onChange={handleChange} />
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
