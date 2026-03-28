import { useEffect, useState, useRef, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { QRCodeCanvas } from 'qrcode.react';
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
  const { t } = useTranslation();
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

  const [orgs, setOrgs] = useState({ SERVICE: [], MANUFACTURER: [], MANAGEMENT: [] });
  const [qrLift, setQrLift] = useState(null);
  const qrRef = useRef(null);

  const downloadQR = useCallback(() => {
    if (!qrRef.current || !qrLift) return;
    const canvas = qrRef.current.querySelector('canvas');
    if (!canvas) return;
    const url = canvas.toDataURL('image/png');
    const link = document.createElement('a');
    link.download = `qr-${qrLift.serialNumber}.png`;
    link.href = url;
    link.click();
  }, [qrLift]);

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

  const fetchOrgs = async () => {
    try {
      const data = await api.get('/organizations?size=100');
      const grouped = { SERVICE: [], MANUFACTURER: [], MANAGEMENT: [] };
      for (const org of data.content) {
        if (grouped[org.type]) {
          grouped[org.type].push(org);
        }
      }
      setOrgs(grouped);
    } catch (err) {
      console.error('Failed to load organizations', err);
    }
  };

  useEffect(() => { fetchLifts(); }, [page]);

  const openCreate = () => {
    setEditId(null);
    setForm(EMPTY_FORM);
    setFormError('');
    fetchOrgs();
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
    fetchOrgs();
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
    if (!confirm(t('lifts.confirmDelete'))) return;
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

  const renderOrgSelect = (name, type) => (
    <label>
      {t(`lifts.${name === 'serviceOrganizationId' ? 'serviceOrgId' : name === 'manufacturerOrganizationId' ? 'manufacturerOrgId' : 'managementOrgId'}`)}
      <select name={name} value={form[name]} onChange={handleChange}>
        <option value="">— {t('common.noData')} —</option>
        {orgs[type].map((org) => (
          <option key={org.id} value={org.id}>{org.name}</option>
        ))}
      </select>
    </label>
  );

  return (
    <div>
      <div className="page-header">
        <h2>{t('lifts.title')}</h2>
        <button className="btn btn-primary" onClick={openCreate}>{t('lifts.add')}</button>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <p>{t('common.loading')}</p>
      ) : lifts.length === 0 ? (
        <div className="empty-state">{t('lifts.empty')}</div>
      ) : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>{t('lifts.serialNumber')}</th>
                <th>{t('lifts.model')}</th>
                <th>{t('lifts.manufacturer')}</th>
                <th>{t('lifts.status')}</th>
                <th>{t('lifts.serviceOrg')}</th>
                <th>{t('common.actions')}</th>
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
                      {t(`liftStatuses.${lift.status}`)}
                    </span>
                  </td>
                  <td>{lift.serviceOrganization?.name || '—'}</td>
                  <td className="actions">
                    <button className="btn btn-secondary btn-sm" onClick={() => setQrLift(lift)} title="QR">QR</button>
                    <button className="btn btn-secondary btn-sm" onClick={() => openEdit(lift)}>{t('common.edit')}</button>
                    <button className="btn btn-danger btn-sm" onClick={() => handleDelete(lift.id)}>{t('common.delete')}</button>
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

      {qrLift && (
        <div className="modal-overlay" onClick={() => setQrLift(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()} style={{ textAlign: 'center' }}>
            <h3>QR — {qrLift.serialNumber}</h3>
            <div ref={qrRef} style={{ margin: '20px 0' }}>
              <QRCodeCanvas value={qrLift.serialNumber} size={256} level="H" includeMargin />
            </div>
            <p style={{ fontSize: '13px', color: '#666', marginBottom: '16px' }}>
              {qrLift.model} {qrLift.manufacturer ? `(${qrLift.manufacturer})` : ''}
            </p>
            <div className="modal-actions" style={{ justifyContent: 'center' }}>
              <button className="btn btn-primary" onClick={downloadQR}>{t('qr.download')}</button>
              <button className="btn btn-secondary" onClick={() => setQrLift(null)}>{t('common.cancel')}</button>
            </div>
          </div>
        </div>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>{editId ? t('lifts.editTitle') : t('lifts.new')}</h3>
            {formError && <div className="error-msg">{formError}</div>}
            <form onSubmit={handleSave}>
              <label>
                {t('lifts.serialNumber')} *
                <input name="serialNumber" value={form.serialNumber} onChange={handleChange} required />
              </label>
              <label>
                {t('lifts.model')} *
                <input name="model" value={form.model} onChange={handleChange} required />
              </label>
              <label>
                {t('lifts.manufacturer')}
                <input name="manufacturer" value={form.manufacturer} onChange={handleChange} />
              </label>
              {renderOrgSelect('serviceOrganizationId', 'SERVICE')}
              {renderOrgSelect('manufacturerOrganizationId', 'MANUFACTURER')}
              {renderOrgSelect('managementOrganizationId', 'MANAGEMENT')}
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
