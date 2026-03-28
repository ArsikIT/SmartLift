import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../../api/auth';
import '../Login/Login.css';

const ORG_TYPES = [
  { value: 'MANUFACTURER', label: 'Manufacturer' },
  { value: 'SERVICE', label: 'Service company' },
  { value: 'MANAGEMENT', label: 'Management company' },
];

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
        <h1>SmartLift</h1>
        <h2>Register</h2>

        {error && <div className="auth-error">{error}</div>}

        <label>
          Username
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
          Email
          <input
            type="email"
            name="email"
            value={form.email}
            onChange={handleChange}
            required
          />
        </label>

        <label>
          Password
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
          Organization name
          <input
            type="text"
            name="organizationName"
            value={form.organizationName}
            onChange={handleChange}
            required
          />
        </label>

        <label>
          Organization type
          <select
            name="organizationType"
            value={form.organizationType}
            onChange={handleChange}
            required
          >
            {ORG_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
        </label>

        <button type="submit" disabled={loading}>
          {loading ? 'Creating account...' : 'Create account'}
        </button>

        <p className="auth-link">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </form>
    </div>
  );
}
