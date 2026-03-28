import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import '../shared.css';

const EMPTY_FORM = {
  username: '',
  email: '',
  password: '',
  enabled: true,
};

export default function Users() {
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
      const data = await api.get(`/users?page=${p}&size=10&sort=createdAt,desc`);
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

  const openEdit = (u) => {
    setEditId(u.id);
    setForm({
      username: u.username,
      email: u.email,
      password: '',
      enabled: u.enabled,
    });
    setFormError('');
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');

    const body = {
      username: form.username,
      email: form.email,
      password: form.password,
      enabled: form.enabled,
    };

    try {
      if (editId) {
        await api.put(`/users/${editId}`, body);
      } else {
        await api.post('/users', body);
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
    if (!confirm('Delete this user?')) return;
    try {
      await api.delete(`/users/${id}`);
      fetchData();
    } catch (err) {
      alert(err.message);
    }
  };

  const handleChange = (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setForm({ ...form, [e.target.name]: value });
  };

  return (
    <div>
      <div className="page-header">
        <h2>Users</h2>
        <button className="btn btn-primary" onClick={openCreate}>+ Add User</button>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <p>Loading...</p>
      ) : items.length === 0 ? (
        <div className="empty-state">No users found</div>
      ) : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>Username</th>
                <th>Email</th>
                <th>Organization</th>
                <th>Roles</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map((u) => (
                <tr key={u.id}>
                  <td>{u.username}</td>
                  <td>{u.email}</td>
                  <td>{u.organization?.name || '—'}</td>
                  <td>{u.roles ? [...u.roles].join(', ') : '—'}</td>
                  <td>
                    <span className={`badge ${u.enabled ? 'badge-green' : 'badge-red'}`}>
                      {u.enabled ? 'Active' : 'Disabled'}
                    </span>
                  </td>
                  <td className="actions">
                    <button className="btn btn-secondary btn-sm" onClick={() => openEdit(u)}>Edit</button>
                    <button className="btn btn-danger btn-sm" onClick={() => handleDelete(u.id)}>Delete</button>
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
            <h3>{editId ? 'Edit User' : 'New User'}</h3>
            {formError && <div className="error-msg">{formError}</div>}
            <form onSubmit={handleSave}>
              <label>
                Username *
                <input name="username" value={form.username} onChange={handleChange} required />
              </label>
              <label>
                Email *
                <input name="email" type="email" value={form.email} onChange={handleChange} required />
              </label>
              <label>
                Password {editId ? '(leave blank to keep)' : '*'}
                <input name="password" type="password" value={form.password} onChange={handleChange} minLength={8} {...(!editId && { required: true })} />
              </label>
              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', flexDirection: 'row' }}>
                <input name="enabled" type="checkbox" checked={form.enabled} onChange={handleChange} style={{ width: 'auto', marginTop: 0 }} />
                Enabled
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
