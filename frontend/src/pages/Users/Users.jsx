import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/client';
import '../shared.css';

const EMPTY_FORM = {
  username: '',
  email: '',
  password: '',
  enabled: true,
};

export default function Users() {
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

  useEffect(() => {
    fetchData();
  }, [page]);

  const openCreate = () => {
    setEditId(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setShowModal(true);
  };

  const openEdit = (user) => {
    setEditId(user.id);
    setForm({
      username: user.username,
      email: user.email,
      password: '',
      enabled: user.enabled,
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
    if (!confirm(t('users.confirmDelete'))) return;
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
        <h2>{t('users.title')}</h2>
        <button className="btn btn-primary" onClick={openCreate}>{t('users.add')}</button>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <p>{t('common.loading')}</p>
      ) : items.length === 0 ? (
        <div className="empty-state">{t('users.empty')}</div>
      ) : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>{t('users.username')}</th>
                <th>{t('users.email')}</th>
                <th>{t('users.organization')}</th>
                <th>{t('users.roles')}</th>
                <th>{t('users.status')}</th>
                <th>{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {items.map((user) => (
                <tr key={user.id}>
                  <td>{user.username}</td>
                  <td>{user.email}</td>
                  <td>{user.organization?.name || '-'}</td>
                  <td>{user.roles ? [...user.roles].join(', ') : '-'}</td>
                  <td>
                    <span className={`badge ${user.enabled ? 'badge-green' : 'badge-red'}`}>
                      {user.enabled ? t('users.active') : t('users.disabled')}
                    </span>
                  </td>
                  <td className="actions">
                    <button className="btn btn-secondary btn-sm" onClick={() => openEdit(user)}>{t('common.edit')}</button>
                    <button className="btn btn-danger btn-sm" onClick={() => handleDelete(user.id)}>{t('common.delete')}</button>
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
            <h3>{editId ? t('users.editTitle') : t('users.new')}</h3>
            {formError && <div className="error-msg">{formError}</div>}
            <form onSubmit={handleSave}>
              <label>
                {t('users.username')} *
                <input name="username" value={form.username} onChange={handleChange} required />
              </label>
              <label>
                {t('users.email')} *
                <input name="email" type="email" value={form.email} onChange={handleChange} required />
              </label>
              <label>
                {t('users.password')} *
                <input name="password" type="password" value={form.password} onChange={handleChange} minLength={8} required />
              </label>
              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', flexDirection: 'row' }}>
                <input name="enabled" type="checkbox" checked={form.enabled} onChange={handleChange} style={{ width: 'auto', marginTop: 0 }} />
                {t('users.enabled')}
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
