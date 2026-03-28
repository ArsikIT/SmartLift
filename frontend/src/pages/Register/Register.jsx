import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { register } from '../../api/auth';
import LanguageSwitcher from '../../components/LanguageSwitcher';
import '../Login/Login.css';

const ORG_TYPE_KEYS = ['MANUFACTURER', 'SERVICE', 'MANAGEMENT'];

export default function Register() {
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    organizationName: '',
    organizationType: 'SERVICE',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { t } = useTranslation();

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await register(form);
      navigate('/login');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit}>
        <div style={{ position: 'absolute', top: '16px', right: '16px' }}>
          <LanguageSwitcher />
        </div>
        <h1>{t('app.name')}</h1>
        <h2>{t('auth.registerTitle')}</h2>

        {error && <div className="auth-error">{error}</div>}

        <label>
          {t('auth.username')}
          <input
            type="text"
            name="username"
            value={form.username}
            onChange={handleChange}
            required
            autoFocus
          />
        </label>

        <label>
          {t('auth.email')}
          <input
            type="email"
            name="email"
            value={form.email}
            onChange={handleChange}
            required
          />
        </label>

        <label>
          {t('auth.password')}
          <input
            type="password"
            name="password"
            value={form.password}
            onChange={handleChange}
            required
            minLength={8}
          />
        </label>

        <label>
          {t('auth.orgName')}
          <input
            type="text"
            name="organizationName"
            value={form.organizationName}
            onChange={handleChange}
            required
          />
        </label>

        <label>
          {t('auth.orgType')}
          <select
            name="organizationType"
            value={form.organizationType}
            onChange={handleChange}
            required
          >
            {ORG_TYPE_KEYS.map((key) => (
              <option key={key} value={key}>
                {t(`orgTypes.${key}`)}
              </option>
            ))}
          </select>
        </label>

        <button type="submit" disabled={loading}>
          {loading ? t('auth.creatingAccount') : t('auth.register')}
        </button>

        <p className="auth-link">
          {t('auth.hasAccount')} <Link to="/login">{t('auth.loginTitle')}</Link>
        </p>
      </form>
    </div>
  );
}
