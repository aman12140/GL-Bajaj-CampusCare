import { useMemo, useState } from 'react';
import api, { errorMessage, fieldErrors } from '../../api/client';
import { useFetch } from '../../hooks/useFetch';
import PageHeader from '../../components/PageHeader';
import Modal from '../../components/Modal';
import Field from '../../components/Field';
import { Alert, ErrorState, Loading } from '../../components/States';

export default function Categories() {
  const { data, loading, error, reload } = useFetch('/api/categories');
  const departments = useFetch('/api/departments');
  const [modal, setModal] = useState(null);
  const [notice, setNotice] = useState('');
  const [problem, setProblem] = useState('');

  const grouped = useMemo(() => {
    const all = data || [];
    return all.filter((c) => !c.parentId).map((p) => ({ parent: p, subs: all.filter((c) => c.parentId === p.id) }));
  }, [data]);

  async function toggle(c) {
    setProblem('');
    try { await api.put(`/api/categories/${c.id}/status`, { status: c.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' }); reload(); }
    catch (err) { setProblem(errorMessage(err)); }
  }

  const row = (c, isSub) => (
    <tr key={c.id} className={c.status === 'INACTIVE' ? 'row-muted' : ''}>
      <td className={isSub ? 'indent' : ''}>{isSub ? c.name : <strong>{c.name}</strong>}</td>
      <td>{c.departmentName || <span className="muted">-</span>}</td>
      <td><span className={`badge ${c.status === 'ACTIVE' ? 'status-resolved' : 'status-closed'}`}>{c.status === 'ACTIVE' ? 'Active' : 'Inactive'}</span></td>
      <td className="actions">
        <button className="btn btn-ghost" onClick={() => setModal(c)}>Edit</button>
        <button className={`btn btn-ghost ${c.status === 'ACTIVE' ? 'danger' : ''}`} onClick={() => toggle(c)}>{c.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button>
      </td>
    </tr>
  );

  return (
    <>
      <PageHeader title="Categories" subtitle="Each category (or sub-category) suggests a department. The admin can always choose a different one when assigning."
                  actions={<button className="btn btn-primary" onClick={() => setModal({ isNew: true })}>Add category</button>} />
      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <Alert type="error" onClose={() => setProblem('')}>{problem}</Alert>
      {loading ? <Loading /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Category</th><th>Suggested department</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>{grouped.flatMap(({ parent, subs }) => [row(parent, false), ...subs.map((s) => row(s, true))])}</tbody>
          </table>
        </div>
      )}
      {modal && <CategoryForm category={modal.isNew ? null : modal} parents={(data || []).filter((c) => !c.parentId)} departments={(departments.data || []).filter((d) => d.status === 'ACTIVE')}
        onClose={() => setModal(null)} onSaved={(m) => { setModal(null); setNotice(m); reload(); }} />}
    </>
  );
}

function CategoryForm({ category, parents, departments, onClose, onSaved }) {
  const [form, setForm] = useState({ name: category?.name || '', description: category?.description || '', parentId: category?.parentId ? String(category.parentId) : '', departmentId: category?.departmentId ? String(category.departmentId) : '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  async function submit(e) {
    e.preventDefault();
    if (!form.name.trim()) return setErrors({ name: 'Category name is required' });
    setBusy(true); setError('');
    const payload = { name: form.name, description: form.description, parentId: form.parentId ? Number(form.parentId) : null, departmentId: form.departmentId ? Number(form.departmentId) : null };
    try {
      if (category) await api.put(`/api/categories/${category.id}`, payload); else await api.post('/api/categories', payload);
      onSaved(`Category "${form.name}" saved.`);
    } catch (err) { setErrors(fieldErrors(err)); setError(errorMessage(err)); } finally { setBusy(false); }
  }
  return (
    <Modal title={category ? 'Edit category' : 'Add category'} onClose={onClose}>
      <form className="form" onSubmit={submit} noValidate>
        <Alert type="error">{error}</Alert>
        <Field label="Name" error={errors.name}><input value={form.name} maxLength={100} onChange={set('name')} /></Field>
        <Field label="Parent category" hint="Leave empty for a top-level category">
          <select value={form.parentId} onChange={set('parentId')}><option value="">None (top-level)</option>
            {parents.filter((p) => p.id !== category?.id).map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}</select>
        </Field>
        <Field label="Suggested department">
          <select value={form.departmentId} onChange={set('departmentId')}><option value="">None</option>
            {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}</select>
        </Field>
        <Field label="Description"><textarea rows={2} maxLength={300} value={form.description} onChange={set('description')} /></Field>
        <div className="form-actions"><button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button><button className="btn btn-primary" disabled={busy}>{busy ? 'Saving...' : 'Save'}</button></div>
      </form>
    </Modal>
  );
}
