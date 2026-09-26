import { useMemo, useState } from 'react';
import api, { errorMessage, fieldErrors } from '../../api/client';
import { useFetch } from '../../hooks/useFetch';
import PageHeader from '../../components/PageHeader';
import Modal from '../../components/Modal';
import Field from '../../components/Field';
import { Alert, ErrorState, Loading } from '../../components/States';

const LEVEL_LABEL = { BUILDING: 'Building / Block', FLOOR: 'Floor', ROOM: 'Room / Area' };

export default function Locations() {
  const locations = useFetch('/api/locations');
  const types = useFetch('/api/location-types');
  const [modal, setModal] = useState(null);
  const [typeName, setTypeName] = useState('');
  const [notice, setNotice] = useState('');
  const [problem, setProblem] = useState('');

  // Depth-first ordered list so children appear under their parent.
  const ordered = useMemo(() => {
    const all = locations.data || [];
    const kids = (id) => all.filter((l) => l.parentId === id);
    const walk = (l, depth) => [{ ...l, depth }, ...kids(l.id).flatMap((k) => walk(k, depth + 1))];
    return all.filter((l) => !l.parentId).flatMap((b) => walk(b, 0));
  }, [locations.data]);

  async function toggle(l) {
    setProblem('');
    try { await api.put(`/api/locations/${l.id}/status`, { status: l.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' }); locations.reload(); }
    catch (err) { setProblem(errorMessage(err)); }
  }
  async function toggleType(t) {
    setProblem('');
    try { await api.put(`/api/location-types/${t.id}/status`, { status: t.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' }); types.reload(); }
    catch (err) { setProblem(errorMessage(err)); }
  }
  async function addType(e) {
    e.preventDefault();
    if (!typeName.trim()) return;
    setProblem('');
    try { await api.post('/api/location-types', { name: typeName.trim() }); setTypeName(''); setNotice('Location type added.'); types.reload(); }
    catch (err) { setProblem(errorMessage(err)); }
  }

  return (
    <>
      <PageHeader title="Campus locations" subtitle="Building/Block, then Floor, then Room/Area. Students pick from these when they report an issue. Use the Add room button on a building or floor to add rooms."
                  actions={<button className="btn btn-primary" onClick={() => setModal({ isNew: true })}>Add location</button>} />
      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <Alert type="error" onClose={() => setProblem('')}>{problem}</Alert>
      <p className="muted">No room numbers are pre-filled. Students can also type a Room No. themselves when reporting an issue, so adding every room here is optional.</p>

      {locations.loading ? <Loading /> : locations.error ? <ErrorState message={locations.error} onRetry={locations.reload} /> : (
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Name</th><th>Level</th><th>Type</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {ordered.map((l) => (
                <tr key={l.id} className={l.status === 'INACTIVE' ? 'row-muted' : ''}>
                  <td style={{ paddingLeft: `${12 + l.depth * 22}px` }}>{l.depth === 0 ? <strong>{l.name}</strong> : l.name}</td>
                  <td>{LEVEL_LABEL[l.level]}</td><td>{l.type}</td>
                  <td><span className={`badge ${l.status === 'ACTIVE' ? 'status-resolved' : 'status-closed'}`}>{l.status === 'ACTIVE' ? 'Active' : 'Inactive'}</span></td>
                  <td className="actions">
                    <button className="btn btn-ghost" onClick={() => setModal(l)}>Edit</button>
                    {l.level !== 'ROOM' && <button className="btn btn-ghost" onClick={() => setModal({ isNew: true, preset: { level: 'ROOM', parentId: l.id } })}>Add room</button>}
                    <button className={`btn btn-ghost ${l.status === 'ACTIVE' ? 'danger' : ''}`} onClick={() => toggle(l)}>{l.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <section className="panel">
        <h2>Location types</h2>
        {types.loading ? <Loading /> : (
          <>
            <ul className="chip-list">
              {(types.data || []).map((t) => (
                <li key={t.id} className={t.status === 'INACTIVE' ? 'muted' : ''}>
                  {t.name}
                  <button type="button" className="chip-btn" onClick={() => toggleType(t)}>{t.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button>
                </li>
              ))}
            </ul>
            <form className="inline-form" onSubmit={addType}>
              <input value={typeName} maxLength={80} placeholder="New location type" aria-label="New location type" onChange={(e) => setTypeName(e.target.value)} />
              <button className="btn btn-secondary">Add type</button>
            </form>
          </>
        )}
      </section>

      {modal && <LocationForm location={modal.isNew ? null : modal} preset={modal.preset} all={locations.data || []} types={(types.data || []).filter((t) => t.status === 'ACTIVE')}
        onClose={() => setModal(null)} onSaved={(m) => { setModal(null); setNotice(m); locations.reload(); }} />}
    </>
  );
}

function LocationForm({ location, preset, all, types, onClose, onSaved }) {
  const [form, setForm] = useState({ name: location?.name || '', level: location?.level || preset?.level || 'BUILDING', type: location?.type || '', parentId: location?.parentId ? String(location.parentId) : preset?.parentId ? String(preset.parentId) : '', floor: location?.floor || '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const parentOptions = all.filter((l) => (form.level === 'FLOOR' ? l.level === 'BUILDING' : form.level === 'ROOM' ? l.level !== 'ROOM' : false) && l.id !== location?.id);

  async function submit(e) {
    e.preventDefault();
    const v = {};
    if (!form.name.trim()) v.name = 'Name is required';
    if (form.level === 'BUILDING' && !form.type) v.type = 'Choose a location type';
    if (form.level !== 'BUILDING' && !form.parentId) v.parentId = 'Choose where it is located';
    setErrors(v);
    if (Object.keys(v).length) return;
    setBusy(true); setError('');
    const payload = { name: form.name, level: form.level, type: form.level === 'BUILDING' ? form.type : null, parentId: form.level === 'BUILDING' ? null : Number(form.parentId), floor: form.level === 'FLOOR' ? form.floor || form.name : null };
    try {
      if (location) await api.put(`/api/locations/${location.id}`, payload); else await api.post('/api/locations', payload);
      onSaved(`Location "${form.name}" saved.`);
    } catch (err) { setErrors(fieldErrors(err)); setError(errorMessage(err)); } finally { setBusy(false); }
  }

  return (
    <Modal title={location ? 'Edit location' : 'Add location'} onClose={onClose}>
      <form className="form" onSubmit={submit} noValidate>
        <Alert type="error">{error}</Alert>
        <Field label="Level">
          <select value={form.level} onChange={(e) => setForm({ ...form, level: e.target.value, parentId: '' })} disabled={!!location}>
            {Object.entries(LEVEL_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </Field>
        <Field label="Name" error={errors.name}><input value={form.name} maxLength={120} onChange={set('name')} /></Field>
        {form.level === 'BUILDING' ? (
          <Field label="Location type" error={errors.type}>
            <select value={form.type} onChange={set('type')}><option value="">Select type</option>{types.map((t) => <option key={t.id} value={t.name}>{t.name}</option>)}</select>
          </Field>
        ) : (
          <Field label={form.level === 'FLOOR' ? 'Inside building' : 'Inside building or floor'} error={errors.parentId}>
            <select value={form.parentId} onChange={set('parentId')}><option value="">Select</option>{parentOptions.map((p) => <option key={p.id} value={p.id}>{p.path}</option>)}</select>
          </Field>
        )}
        <div className="form-actions"><button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button><button className="btn btn-primary" disabled={busy}>{busy ? 'Saving...' : 'Save'}</button></div>
      </form>
    </Modal>
  );
}
