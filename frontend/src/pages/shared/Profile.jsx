import { useState } from 'react';
import api, { errorMessage, fieldErrors } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import PageHeader from '../../components/PageHeader';
import Field from '../../components/Field';
import ChangePasswordForm from '../../components/ChangePasswordForm';
import { Alert } from '../../components/States';
import { PHONE_RE, statusLabel } from '../../utils/format';

export default function Profile() {
  const { user, setUser } = useAuth();
  const [form, setForm] = useState({ name: user.name, phone: user.phone || '', course: user.course || '', branch: user.branch || '', year: user.year || 1, section: user.section || '' });
  const [errors, setErrors] = useState({});
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const student = user.role === 'STUDENT';

  async function save(e) {
    e.preventDefault();
    setMsg(''); setError('');
    const v = {};
    if (!form.name.trim()) v.name = 'Name is required';
    if (form.phone && !PHONE_RE.test(form.phone)) v.phone = 'Enter a valid 10-digit mobile number';
    setErrors(v);
    if (Object.keys(v).length) return;
    setBusy(true);
    try {
      const res = await api.put('/api/profile', { ...form, year: student ? Number(form.year) : null });
      setUser(res.data);
      setMsg('Profile updated.');
    } catch (err) { setErrors(fieldErrors(err)); setError(errorMessage(err)); } finally { setBusy(false); }
  }

  return (
    <>
      <PageHeader title="My profile" subtitle={`${statusLabel(user.role)} account`} />
      <div className="two-col">
        <section className="panel">
          <h2>Details</h2>
          <Alert type="success">{msg}</Alert>
          <Alert type="error">{error}</Alert>
          <form className="form" onSubmit={save} noValidate>
            <Field label="Email"><input value={user.email} disabled /></Field>
            <Field label="Full name" error={errors.name}><input value={form.name} onChange={set('name')} /></Field>
            <Field label="Mobile number" error={errors.phone}><input value={form.phone} onChange={set('phone')} /></Field>
            {student && (
              <>
                <Field label="Student ID"><input value={user.studentId || ''} disabled /></Field>
                <div className="grid-2">
                  <Field label="Course"><input value={form.course} onChange={set('course')} /></Field>
                  <Field label="Branch"><input value={form.branch} onChange={set('branch')} /></Field>
                  <Field label="Year"><select value={form.year} onChange={set('year')}>{[1, 2, 3, 4].map((y) => <option key={y} value={y}>{y}</option>)}</select></Field>
                  <Field label="Section"><input value={form.section} onChange={set('section')} maxLength={10} /></Field>
                </div>
              </>
            )}
            {user.role === 'STAFF' && (
              <>
                <Field label="Employee ID"><input value={user.employeeId || ''} disabled /></Field>
                <Field label="Department"><input value={user.department || ''} disabled /></Field>
                <Field label="Designation"><input value={user.designation || ''} disabled /></Field>
                <p className="muted">Department, designation and employee ID are managed by the administrator.</p>
              </>
            )}
            <button className="btn btn-primary" disabled={busy}>{busy ? 'Saving...' : 'Save changes'}</button>
          </form>
        </section>
        <section className="panel">
          <h2>Change password</h2>
          <ChangePasswordForm />
        </section>
      </div>
    </>
  );
}
