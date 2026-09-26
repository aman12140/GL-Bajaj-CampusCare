import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import AuthShell from '../../components/AuthShell';
import Field from '../../components/Field';
import { Alert } from '../../components/States';
import api, { errorMessage } from '../../api/client';
import { PASSWORD_HINT, PASSWORD_RE } from '../../utils/format';

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const navigate = useNavigate();
  const [form, setForm] = useState({ password: '', confirm: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setError('');
    if (!PASSWORD_RE.test(form.password)) return setError(`New password: ${PASSWORD_HINT}.`);
    if (form.password !== form.confirm) return setError('Passwords do not match.');
    setBusy(true);
    try {
      await api.post('/api/auth/reset-password', { token, newPassword: form.password });
      navigate('/login', { replace: true, state: { message: 'Password updated. You can now log in with your new password.' } });
    } catch (err) { setError(errorMessage(err)); } finally { setBusy(false); }
  }

  return (
    <AuthShell title="Choose a new password" footer={<Link to="/login">Back to log in</Link>}>
      {!token && <Alert type="error">This reset link is incomplete. Request a new one from the forgot-password page.</Alert>}
      <Alert type="error">{error}</Alert>
      <form className="form" onSubmit={submit} noValidate>
        <Field label="New password" hint={PASSWORD_HINT}><input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} autoComplete="new-password" /></Field>
        <Field label="Confirm new password"><input type="password" value={form.confirm} onChange={(e) => setForm({ ...form, confirm: e.target.value })} autoComplete="new-password" /></Field>
        <button className="btn btn-primary btn-block" disabled={busy || !token}>{busy ? 'Saving...' : 'Update password'}</button>
      </form>
    </AuthShell>
  );
}
