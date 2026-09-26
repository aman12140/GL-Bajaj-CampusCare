import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../components/PageHeader';
import Field from '../../components/Field';
import LocationPicker from '../../components/LocationPicker';
import { PriorityBadge } from '../../components/Badges';
import { Alert, ErrorState, Loading } from '../../components/States';
import api, { errorMessage, fieldErrors } from '../../api/client';
import { useFetch } from '../../hooks/useFetch';
import { PHONE_RE, PRIORITIES, ROOM_HINT, ROOM_RE, statusLabel, validateImage } from '../../utils/format';

export default function ReportIssue() {
  const navigate = useNavigate();
  const categories = useFetch('/api/categories', { activeOnly: true });
  const locations = useFetch('/api/locations', { activeOnly: true });

  const [form, setForm] = useState({ title: '', parentId: '', categoryId: '', priority: 'MEDIUM', description: '', contactNumber: '', roomNumber: '' });
  const [locationId, setLocationId] = useState(null);
  const [photo, setPhoto] = useState(null);
  const [preview, setPreview] = useState('');
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [suggestion, setSuggestion] = useState(null);
  const [suggesting, setSuggesting] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const all = categories.data || [];
  const parents = useMemo(() => all.filter((c) => !c.parentId), [all]);
  const subs = useMemo(() => all.filter((c) => String(c.parentId) === String(form.parentId)), [all, form.parentId]);

  useEffect(() => {
    if (!photo) { setPreview(''); return undefined; }
    const url = URL.createObjectURL(photo);
    setPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [photo]);

  function onPhoto(e) {
    const file = e.target.files?.[0] || null;
    const problem = validateImage(file);
    if (problem) { setErrors((x) => ({ ...x, photo: problem })); e.target.value = ''; setPhoto(null); return; }
    setErrors((x) => ({ ...x, photo: undefined }));
    setPhoto(file);
  }

  async function suggest() {
    setSuggesting(true); setSuggestion(null);
    try {
      const res = await api.post('/api/issues/suggest-priority', {
        title: form.title, description: form.description, categoryId: form.categoryId ? Number(form.categoryId) : null,
      });
      setSuggestion(res.data);
    } catch (err) { setError(errorMessage(err)); } finally { setSuggesting(false); }
  }

  function validate() {
    const e = {};
    if (form.title.trim().length < 5) e.title = 'Title must be at least 5 characters';
    if (!form.parentId) e.parentId = 'Choose a category';
    else if (subs.length > 0 && !form.categoryId) e.categoryId = 'Choose a sub-category';
    if (!locationId) e.location = 'Choose at least a location type and building';
    if (form.description.trim().length < 10) e.description = 'Describe the problem in at least 10 characters';
    if (form.roomNumber && !ROOM_RE.test(form.roomNumber)) e.roomNumber = ROOM_HINT;
    if (form.contactNumber && !PHONE_RE.test(form.contactNumber)) e.contactNumber = 'Enter a valid 10-digit mobile number';
    return e;
  }

  async function submit(ev) {
    ev.preventDefault();
    setError('');
    const v = validate();
    setErrors(v);
    if (Object.keys(v).length) return;
    const fd = new FormData();
    fd.append('title', form.title.trim());
    fd.append('categoryId', form.categoryId || form.parentId);
    fd.append('locationId', String(locationId));
    fd.append('priority', form.priority);
    fd.append('description', form.description.trim());
    if (form.contactNumber) fd.append('contactNumber', form.contactNumber);
    if (form.roomNumber.trim()) fd.append('roomNumber', form.roomNumber.trim());
    if (photo) fd.append('photo', photo);
    setBusy(true);
    try {
      const res = await api.post('/api/issues', fd);
      navigate(`/student/issues/${res.data.id}`, { replace: true, state: { notice: `Issue ${res.data.issueNumber} reported successfully.` } });
    } catch (err) {
      setErrors((x) => ({ ...x, ...fieldErrors(err) }));
      setError(errorMessage(err));
    } finally { setBusy(false); }
  }

  if (categories.loading || locations.loading) return <Loading />;
  if (categories.error || locations.error) return <ErrorState message={categories.error || locations.error} onRetry={() => { categories.reload(); locations.reload(); }} />;

  return (
    <>
      <PageHeader title="Report an issue" subtitle="Tell us what is wrong and where. The more precise you are, the faster it gets fixed." />
      <form className="panel form report-form" onSubmit={submit} noValidate>
        <Alert type="error">{error}</Alert>
        <Field label="Issue title" error={errors.title}>
          <input value={form.title} onChange={set('title')} maxLength={150} placeholder="e.g. Wi-Fi not working in the lab" />
        </Field>

        <div className="grid-2">
          <Field label="Category" error={errors.parentId}>
            <select value={form.parentId} onChange={(e) => { setForm({ ...form, parentId: e.target.value, categoryId: '' }); }}>
              <option value="">Select category</option>
              {parents.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
          <Field label="Sub-category" error={errors.categoryId}>
            <select value={form.categoryId} onChange={set('categoryId')} disabled={!form.parentId || subs.length === 0}>
              <option value="">{form.parentId && subs.length === 0 ? 'Not needed' : 'Select sub-category'}</option>
              {subs.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
        </div>

        <LocationPicker
          locations={locations.data || []}
          onChange={setLocationId}
          onRoomChange={(value) => setForm((f) => ({ ...f, roomNumber: value }))}
          roomNumber={form.roomNumber}
          roomError={errors.roomNumber}
          error={errors.location}
        />

        <Field label="Description" error={errors.description} hint={`${form.description.length}/2000 characters`}>
          <textarea rows={5} maxLength={2000} value={form.description} onChange={set('description')} placeholder="What happened? Since when? Who is affected?" />
        </Field>

        <div className="priority-block">
          <Field label="Priority">
            <select value={form.priority} onChange={set('priority')}>
              {PRIORITIES.map((p) => <option key={p} value={p}>{statusLabel(p)}</option>)}
            </select>
          </Field>
          <div>
            <button type="button" className="btn btn-secondary" onClick={suggest} disabled={suggesting || (form.title.length < 3 && form.description.length < 3)}>
              {suggesting ? 'Checking...' : 'Suggest priority'}
            </button>
            {suggestion && (
              <div className="suggestion" role="status">
                <div>Suggested: <PriorityBadge priority={suggestion.priority} />{' '}
                  <button type="button" className="btn btn-ghost" onClick={() => setForm({ ...form, priority: suggestion.priority })}>Use this</button>
                </div>
                <ul>{suggestion.reasons.map((r) => <li key={r}>{r}</li>)}</ul>
                <small className="muted">{suggestion.note}</small>
              </div>
            )}
          </div>
        </div>

        <div className="grid-2">
          <Field label="Contact number (optional)" error={errors.contactNumber} hint="Where staff can reach you">
            <input inputMode="numeric" value={form.contactNumber} onChange={set('contactNumber')} />
          </Field>
          <Field label="Photo (optional)" error={errors.photo} hint="JPG or PNG, up to 5 MB">
            <input type="file" accept="image/jpeg,image/png" onChange={onPhoto} />
          </Field>
        </div>
        {preview && <img className="photo-preview" src={preview} alt="Selected upload preview" />}

        <div className="form-actions">
          <button type="button" className="btn btn-secondary" onClick={() => navigate('/student/issues')}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Submitting...' : 'Submit issue'}</button>
        </div>
      </form>
    </>
  );
}
