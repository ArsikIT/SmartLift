import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/client';
import '../shared.css';
import './Scanner.css';

const STATUS_BADGES = {
  CREATED: 'badge-gray',
  INSTALLED: 'badge-blue',
  ACTIVE: 'badge-green',
  FAULTY: 'badge-red',
  IN_REPAIR: 'badge-orange',
  DECOMMISSIONED: 'badge-purple',
};

export default function Scanner() {
  const { t } = useTranslation();
  const [scanning, setScanning] = useState(false);
  const [lift, setLift] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const scannerRef = useRef(null);
  const html5QrRef = useRef(null);

  const startScanner = async () => {
    setError('');
    setLift(null);
    setScanning(true);

    try {
      const { Html5Qrcode } = await import('html5-qrcode');
      const scanner = new Html5Qrcode('qr-reader');
      html5QrRef.current = scanner;

      await scanner.start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: { width: 250, height: 250 } },
        async (decodedText) => {
          await scanner.stop();
          html5QrRef.current = null;
          setScanning(false);
          fetchLift(decodedText);
        },
        () => {}
      );
    } catch (err) {
      setScanning(false);
      setError(t('qr.cameraError'));
    }
  };

  const stopScanner = async () => {
    if (html5QrRef.current) {
      try {
        await html5QrRef.current.stop();
      } catch (e) {}
      html5QrRef.current = null;
    }
    setScanning(false);
  };

  const fetchLift = async (serialNumber) => {
    setLoading(true);
    setError('');
    try {
      const data = await api.get(`/lifts/serial/${encodeURIComponent(serialNumber)}`);
      setLift(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    return () => {
      if (html5QrRef.current) {
        html5QrRef.current.stop().catch(() => {});
      }
    };
  }, []);

  return (
    <div>
      <div className="page-header">
        <h2>{t('qr.scanTitle')}</h2>
      </div>

      {error && <div className="error-msg">{error}</div>}

      <div className="scanner-container">
        {!scanning && !lift && !loading && (
          <div className="scanner-start">
            <p>{t('qr.scanHint')}</p>
            <button className="btn btn-primary" onClick={startScanner}>
              {t('qr.startScan')}
            </button>
          </div>
        )}

        {scanning && (
          <div>
            <div id="qr-reader" className="qr-reader" />
            <button className="btn btn-secondary" onClick={stopScanner} style={{ marginTop: '12px' }}>
              {t('common.cancel')}
            </button>
          </div>
        )}

        {loading && <p>{t('common.loading')}</p>}

        {lift && (
          <div className="lift-card">
            <h3>{lift.serialNumber}</h3>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <tbody>
                {[
                  [t('lifts.model'), lift.model],
                  [t('lifts.manufacturer'), lift.manufacturer || '—'],
                  [t('lifts.status'), (
                    <span className={`badge ${STATUS_BADGES[lift.status] || 'badge-gray'}`}>
                      {t(`liftStatuses.${lift.status}`)}
                    </span>
                  )],
                  [t('lifts.serviceOrg'), lift.serviceOrganization?.name || '—'],
                ].map(([label, value], i) => (
                  <tr key={i}>
                    <td style={{ padding: '8px 12px', fontWeight: 600, color: '#555', borderBottom: '1px solid #f0f0f0' }}>{label}</td>
                    <td style={{ padding: '8px 12px', borderBottom: '1px solid #f0f0f0' }}>{value}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <button className="btn btn-primary" onClick={() => { setLift(null); }} style={{ marginTop: '16px' }}>
              {t('qr.scanAgain')}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
