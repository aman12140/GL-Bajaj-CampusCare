import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AuthShell from '../../components/AuthShell';
import Field from '../../components/Field';
import { Alert } from '../../components/States';
import api, { errorMessage, fieldErrors } from '../../api/client';
import { PASSWORD_HINT, PASSWORD_RE, PHONE_RE } from '../../utils/format';

const INITIAL = { name: '', studentId: '', email: '', phone: '', course: 'B.Tech', branch: '', year: '1', section: '', password: '', confirmPassword: '' };

function validate(f) {
  const e = {};
  if (!f.name.trim()) e.name = 'Full name is required';
  if (!f.studentId.trim()) e.studentId = 'Student ID is required';
  if (!/^\S+@\S+\.\S+$/.test(f.email)) e.email = 'Enter a valid email address';
  if (!PHONE_RE.test(f.phone)) e.phone = 'Enter a valid 10-digit mobile number';
  if (!f.course.trim()) e.course = 'Course is required';
  if (!f.branch.trim()) e.branch = 'Branch is required';
  if (!f.section.trim()) e.section = 'Section is required';
  if (!PASSWORD_RE.test(f.password)) e.password = PASSWORD_HINT;
  if (f.password !== f.confirmPassword) e.confirmPassword = 'Passwords do not match';
  return e;
}

export default function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState(INITIAL);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setError('');
    const v = validate(form);
    setErrors(v);
    if (Object.keys(v).length) return;
    setBusy(true);
    try {
      await api.post('/api/auth/register', { ...form, year: Number(form.year) });
      navigate('/login', { replace: true, state: { message: 'Registration successful. You can now log in.' } });
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err));
    } finally { setBusy(false); }
  }

  return (
    <AuthShell title="Student registration" subtitle="Only students can register here. Staff accounts are created by the administrator."
               wide footer={<>Already registered? <Link to="/login">Log in</Link></>}>
      <Alert type="error">{error}</Alert>
      <form className="form grid-2" onSubmit={submit} noValidate>
        <Field label="Full name" error={errors.name}><input value={form.name} onChange={set('name')} autoComplete="name" /></Field>
        <Field label="Student ID" error={errors.studentId}><input value={form.studentId} onChange={set('studentId')} /></Field>
        <Field label="Email" error={errors.email}><input type="email" value={form.email} onChange={set('email')} autoComplete="email" /></Field>
        <Field label="Mobile number" error={errors.phone} hint="10 digits, e.g. 9876543210"><input inputMode="numeric" value={form.phone} onChange={set('phone')} autoComplete="tel" /></Field>
        <Field label="Course" error={errors.course}><input value={form.course} onChange={set('course')} /></Field>
        <Field label="Branch" error={errors.branch}><input value={form.branch} onChange={set('branch')} placeholder="e.g. Computer Science" /></Field>
        <Field label="Year" error={errors.year}>
          <select value={form.year} onChange={set('year')}>{[1, 2, 3, 4].map((y) => <option key={y} value={y}>{y}</option>)}</select>
        </Field>
        <Field label="Section" error={errors.section}><input value={form.section} onChange={set('section')} maxLength={10} /></Field>
        <Field label="Password" error={errors.password} hint={PASSWORD_HINT}><input type="password" value={form.password} onChange={set('password')} autoComplete="new-password" /></Field>
        <Field label="Confirm password" error={errors.confirmPassword}><input type="password" value={form.confirmPassword} onChange={set('confirmPassword')} autoComplete="new-password" /></Field>
        <div className="span-2"><button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Creating account...' : 'Create account'}</button></div>
      </form>
    </AuthShell>
  );
}
