import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import AuthShell from '../../components/AuthShell';
import { Alert } from '../../components/States';
import Field from '../../components/Field';
import { errorMessage } from '../../api/client';
import { HOME_BY_ROLE, useAuth } from '../../context/AuthContext';

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user && !user.passwordChangeRequired) {
    return <Navigate to={HOME_BY_ROLE[user.role]} replace />;
  }

  async function submit(e) {
    e.preventDefault();
    setError('');

    if (!form.email.trim() || !form.password) {
      return setError('Enter your email and password.');
    }

    setBusy(true);

    try {
      const u = await login(form.email.trim(), form.password);

      navigate(
        u.passwordChangeRequired
          ? '/change-password'
          : location.state?.from || HOME_BY_ROLE[u.role],
        { replace: true }
      );
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthShell
      title="Log in"
      subtitle="Use your CampusCare account. Staff accounts are created by the administrator."
      footer={
        <>
          New student? <Link to="/register">Create an account</Link>
        </>
      }
    >
      {location.state?.message && (
        <Alert type="success">{location.state.message}</Alert>
      )}

      {params.get('expired') && (
        <Alert type="info">
          Your session has expired. Please log in again.
        </Alert>
      )}

      <Alert type="error">{error}</Alert>

      <form className="form" onSubmit={submit} noValidate>
        <Field label="Email">
          <input
            type="email"
            autoComplete="username"
            value={form.email}
            onChange={(e) =>
              setForm({ ...form, email: e.target.value })
            }
            required
          />
        </Field>

        <Field label="Password">
          <input
            type="password"
            autoComplete="current-password"
            value={form.password}
            onChange={(e) =>
              setForm({ ...form, password: e.target.value })
            }
            required
          />
        </Field>

        <button
          className="btn btn-primary btn-block"
          disabled={busy}
        >
          {busy ? 'Logging in...' : 'Log in'}
        </button>

        <p className="center">
          <Link to="/forgot-password">Forgot your password?</Link>
        </p>
      </form>
    </AuthShell>
  );
}