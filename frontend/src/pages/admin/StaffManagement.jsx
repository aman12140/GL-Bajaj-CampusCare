import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import api, { errorMessage, fieldErrors } from '../../api/client';
import { useDebounced, useFetch } from '../../hooks/useFetch';
import PageHeader from '../../components/PageHeader';
import Modal from '../../components/Modal';
import Field from '../../components/Field';
import { Alert, EmptyState, ErrorState, Loading } from '../../components/States';
import { PASSWORD_HINT, PASSWORD_RE, PHONE_RE, cleanParams } from '../../utils/format';

const BLANK = { name: '', employeeId: '', email: '', phone: '', departmentId: '', designation: '', temporaryPassword: '' };

export default function StaffManagement() {
  const [searchParams] = useSearchParams();
  const [q, setQ] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [status, setStatus] = useState(searchParams.get('status') || '');
  const dq = useDebounced(q);
  const departments = useFetch('/api/departments');
  const { data, loading, error, reload } = useFetch('/api/admin/staff', cleanParams({ q: dq, departmentId, status }), [dq, departmentId, status]);
  const [notice, setNotice] = useState('');
  const [modal, setModal] = useState(null);       // { type: 'form', staff? } | { type: 'confirm', staff }

  async function toggle(staff) {
    try {
      const res = await api.put(`/api/admin/staff/${staff.id}/${staff.status === 'ACTIVE' ? 'deactivate' : 'activate'}`);
      setModal(null);
      setNotice(staff.status === 'ACTIVE'
        ? `${staff.name} was deactivated and can no longer log in.${staff.openAssignedIssues ? ` ${staff.openAssignedIssues} open issue(s) are still assigned - reassign them from "All issues".` : ''}`
        : `${staff.name} is active again.`);
      reload();
      return res;
    } catch (err) { setModal({ type: 'confirm', staff, error: errorMessage(err) }); return null; }
  }

  return (
    <>
      <PageHeader title="Staff management" subtitle="Create staff accounts and deactivate people who leave. Staff are never deleted, so issue history stays intact."
                  actions={<button className="btn btn-primary" onClick={() => setModal({ type: 'form' })}>Add staff member</button>} />
      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <div className="filters">
        <label className="grow">Search<input type="search" placeholder="Name, employee ID or email" value={q} onChange={(e) => setQ(e.target.value)} /></label>
        <label>Department
          <select value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}><option value="">All</option>
            {(departments.data || []).map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}</select>
        </label>
        <label>Status
          <select value={status} onChange={(e) => setStatus(e.target.value)}><option value="">All</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select>
        </label>
      </div>

      {loading ? <Loading /> : error ? <ErrorState message={error} onRetry={reload} /> : data.length === 0 ? (
        <EmptyState title="No staff found">Try different filters or add a staff member.</EmptyState>
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Name</th><th>Employee ID</th><th>Department</th><th>Designation</th><th>Contact</th><th>Open issues</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {data.map((s) => (
                <tr key={s.id} className={s.status === 'INACTIVE' ? 'row-muted' : ''}>
                  <td><strong>{s.name}</strong></td>
                  <td className="mono">{s.employeeId}</td>
                  <td>{s.departmentName}</td>
                  <td>{s.designation}</td>
                  <td>{s.email}<br /><span className="muted">{s.phone}</span></td>
                  <td>{s.openAssignedIssues > 0 ? <Link to={`/admin/issues?staffId=${s.id}`}>{s.openAssignedIssues} open</Link> : '0'}
                    <span className="muted"> / {s.totalAssignedIssues} total</span></td>
                  <td><span className={`badge ${s.status === 'ACTIVE' ? 'status-resolved' : 'status-closed'}`}>{s.status === 'ACTIVE' ? 'Active' : 'Inactive'}</span></td>
                  <td className="actions">
                    <button className="btn btn-ghost" onClick={() => setModal({ type: 'form', staff: s })}>Edit</button>
                    {s.status === 'ACTIVE'
                      ? <button className="btn btn-ghost danger" onClick={() => setModal({ type: 'confirm', staff: s })}>Deactivate</button>
                      : <button className="btn btn-ghost" onClick={() => toggle(s)}>Activate</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {modal?.type === 'form' && (
        <StaffForm staff={modal.staff} departments={(departments.data || []).filter((d) => d.status === 'ACTIVE' || d.id === modal.staff?.departmentId)}
          onClose={() => setModal(null)} onSaved={(msg) => { setModal(null); setNotice(msg); reload(); }} />
      )}
      {modal?.type === 'confirm' && (
        <Modal title="Deactivate staff member?" onClose={() => setModal(null)}>
          <Alert type="error">{modal.error}</Alert>
          <p><strong>{modal.staff.name}</strong> will no longer be able to log in or receive new assignments.</p>
          {modal.staff.openAssignedIssues > 0 && <Alert type="warning">{modal.staff.openAssignedIssues} open issue(s) are assigned to this person. They stay assigned until you reassign them.</Alert>}
          <p className="muted">The account and all history are kept. You can activate the account again later.</p>
          <div className="form-actions">
            <button className="btn btn-secondary" onClick={() => setModal(null)}>Cancel</button>
            <button className="btn btn-danger" onClick={() => toggle(modal.staff)}>Deactivate</button>
          </div>
        </Modal>
      )}
    </>
  );
}

function StaffForm({ staff, departments, onClose, onSaved }) {
  const editing = !!staff;
  const [form, setForm] = useState(editing ? { ...BLANK, name: staff.name, employeeId: staff.employeeId, email: staff.email, phone: staff.phone || '', departmentId: String(staff.departmentId), designation: staff.designation } : BLANK);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setError('');
    const v = {};
    if (!form.name.trim()) v.name = 'Name is required';
    if (!form.employeeId.trim()) v.employeeId = 'Employee ID is required';
    if (!/^\S+@\S+\.\S+$/.test(form.email)) v.email = 'Enter a valid email';
    if (!PHONE_RE.test(form.phone)) v.phone = 'Enter a valid 10-digit mobile number';
    if (!form.departmentId) v.departmentId = 'Select a department';
    if (!form.designation.trim()) v.designation = 'Designation is required';
    if (!editing && !PASSWORD_RE.test(form.temporaryPassword)) v.temporaryPassword = PASSWORD_HINT;
    setErrors(v);
    if (Object.keys(v).length) return;
    setBusy(true);
    try {
      const payload = { ...form, departmentId: Number(form.departmentId) };
      if (editing) { delete payload.temporaryPassword; await api.put(`/api/admin/staff/${staff.id}`, payload); onSaved(`${form.name} was updated.`); }
      else { await api.post('/api/admin/staff', payload); onSaved(`${form.name} was created. They must change the temporary password at first login.`); }
    } catch (err) { setErrors(fieldErrors(err)); setError(errorMessage(err)); } finally { setBusy(false); }
  }

  return (
    <Modal title={editing ? 'Edit staff member' : 'Add staff member'} onClose={onClose} wide>
      <form className="form grid-2" onSubmit={submit} noValidate>
        <div className="span-2"><Alert type="error">{error}</Alert></div>
        <Field label="Full name" error={errors.name}><input value={form.name} onChange={set('name')} /></Field>
        <Field label="Employee ID" error={errors.employeeId}><input value={form.employeeId} onChange={set('employeeId')} /></Field>
        <Field label="Email" error={errors.email}><input type="email" value={form.email} onChange={set('email')} /></Field>
        <Field label="Mobile number" error={errors.phone}><input value={form.phone} onChange={set('phone')} /></Field>
        <Field label="Department" error={errors.departmentId}>
          <select value={form.departmentId} onChange={set('departmentId')}><option value="">Select department</option>
            {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}</select>
        </Field>
        <Field label="Designation" error={errors.designation}><input value={form.designation} onChange={set('designation')} /></Field>
        {!editing && (
          <Field label="Temporary password" error={errors.temporaryPassword} hint={`${PASSWORD_HINT}. The staff member must change it at first login.`} className="span-2">
            <input type="text" autoComplete="off" value={form.temporaryPassword} onChange={set('temporaryPassword')} />
          </Field>
        )}
        <div className="span-2 form-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Saving...' : editing ? 'Save changes' : 'Create staff account'}</button>
        </div>
      </form>
    </Modal>
  );
}
