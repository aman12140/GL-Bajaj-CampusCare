import { useState } from 'react';
import api, { errorMessage, fieldErrors } from '../../api/client';
import { useFetch } from '../../hooks/useFetch';
import PageHeader from '../../components/PageHeader';
import Modal from '../../components/Modal';
import Field from '../../components/Field';
import { Alert, ErrorState, Loading } from '../../components/States';

export default function Departments() {
  const { data, loading, error, reload } = useFetch('/api/departments');
  const [modal, setModal] = useState(null);
  const [notice, setNotice] = useState('');
  const [problem, setProblem] = useState('');

  async function toggle(d) {
    setProblem('');
    try { await api.put(`/api/departments/${d.id}/status`, { status: d.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' }); reload(); }
    catch (err) { setProblem(errorMessage(err)); }
  }

  return (
    <>
      <PageHeader title="Departments" subtitle="Operational departments that handle campus issues. Inactive departments cannot receive new assignments."
                  actions={<button className="btn btn-primary" onClick={() => setModal({})}>Add department</button>} />
      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <Alert type="error" onClose={() => setProblem('')}>{problem}</Alert>
      {loading ? <Loading /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Name</th><th>Group</th><th>Staff</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {data.map((d) => (
                <tr key={d.id} className={d.status === 'INACTIVE' ? 'row-muted' : ''}>
                  <td><strong>{d.name}</strong></td><td>{d.description}</td><td>{d.staffCount}</td>
                  <td><span className={`badge ${d.status === 'ACTIVE' ? 'status-resolved' : 'status-closed'}`}>{d.status === 'ACTIVE' ? 'Active' : 'Inactive'}</span></td>
                  <td className="actions">
                    <button className="btn btn-ghost" onClick={() => setModal(d)}>Edit</button>
                    <button className={`btn btn-ghost ${d.status === 'ACTIVE' ? 'danger' : ''}`} onClick={() => toggle(d)}>{d.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {modal && <DeptForm dept={modal.id ? modal : null} onClose={() => setModal(null)} onSaved={(m) => { setModal(null); setNotice(m); reload(); }} />}
    </>
  );
}

function DeptForm({ dept, onClose, onSaved }) {
  const [form, setForm] = useState({ name: dept?.name || '', description: dept?.description || '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault();
    if (!form.name.trim()) return setErrors({ name: 'Department name is required' });
    setBusy(true); setError('');
    try {
      if (dept) await api.put(`/api/departments/${dept.id}`, form); else await api.post('/api/departments', form);
      onSaved(`Department "${form.name}" saved.`);
    } catch (err) { setErrors(fieldErrors(err)); setError(errorMessage(err)); } finally { setBusy(false); }
  }
  return (
    <Modal title={dept ? 'Edit department' : 'Add department'} onClose={onClose}>
      <form className="form" onSubmit={submit} noValidate>
        <Alert type="error">{error}</Alert>
        <Field label="Name" error={errors.name}><input value={form.name} maxLength={100} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
        <Field label="Description" error={errors.description}><textarea rows={3} maxLength={300} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
        <div className="form-actions"><button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button><button className="btn btn-primary" disabled={busy}>{busy ? 'Saving...' : 'Save'}</button></div>
      </form>
    </Modal>
  );
}
