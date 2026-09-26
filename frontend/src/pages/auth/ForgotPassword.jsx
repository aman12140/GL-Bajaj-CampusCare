import { useState } from 'react';
import { Link } from 'react-router-dom';
import AuthShell from '../../components/AuthShell';
import Field from '../../components/Field';
import { Alert } from '../../components/States';
import api, { errorMessage } from '../../api/client';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setError(''); setResult(null);
    if (!/^\S+@\S+\.\S+$/.test(email)) return setError('Enter a valid email address.');
    setBusy(true);
    try {
      const res = await api.post('/api/auth/forgot-password', { email: email.trim() });
      setResult(res.data);
    } catch (err) { setError(errorMessage(err)); } finally { setBusy(false); }
  }

  return (
    <AuthShell title="Forgot password" subtitle="Enter your account email and we will generate a reset link."
               footer={<Link to="/login">Back to log in</Link>}>
      <Alert type="error">{error}</Alert>
      {result && (
        <Alert type="success">
          {result.message}
          {result.resetLink && (
            <span className="dev-link"><br /><strong>Development mode:</strong> no email is sent in this college project. <a href={result.resetLink}>Open the reset page</a></span>
          )}
        </Alert>
      )}
      <form className="form" onSubmit={submit} noValidate>
        <Field label="Email"><input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" /></Field>
        <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Please wait...' : 'Send reset link'}</button>
      </form>
    </AuthShell>
  );
}
