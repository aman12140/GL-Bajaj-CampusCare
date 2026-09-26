import { useState } from 'react';
import api, { errorMessage } from '../api/client';
import { PASSWORD_HINT, PASSWORD_RE } from '../utils/format';
import { Alert } from './States';

export default function ChangePasswordForm({ onDone, submitLabel = 'Change password' }) {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setError(''); setOk('');
    if (!PASSWORD_RE.test(form.newPassword)) return setError(`New password: ${PASSWORD_HINT}.`);
    if (form.newPassword !== form.confirm) return setError('New password and confirmation do not match.');
    setBusy(true);
    try {
      await api.post('/api/auth/change-password', { currentPassword: form.currentPassword, newPassword: form.newPassword });
      setOk('Password changed successfully.');
      setForm({ currentPassword: '', newPassword: '', confirm: '' });
      onDone?.();
    } catch (err) {
      setError(errorMessage(err));
    } finally { setBusy(false); }
  }

  return (
    <form className="form" onSubmit={submit} noValidate>
      <Alert type="error">{error}</Alert>
      <Alert type="success">{ok}</Alert>
      <label>Current password
        <input type="password" autoComplete="current-password" value={form.currentPassword} onChange={set('currentPassword')} required />
      </label>
      <label>New password
        <input type="password" autoComplete="new-password" value={form.newPassword} onChange={set('newPassword')} required />
        <span className="hint">{PASSWORD_HINT}</span>
      </label>
      <label>Confirm new password
        <input type="password" autoComplete="new-password" value={form.confirm} onChange={set('confirm')} required />
      </label>
      <button className="btn btn-primary" disabled={busy}>{busy ? 'Saving...' : submitLabel}</button>
    </form>
  );
}
