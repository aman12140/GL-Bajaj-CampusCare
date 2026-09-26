import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import api, { errorMessage, fileUrl } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { useFetch } from '../../hooks/useFetch';
import PageHeader from '../../components/PageHeader';
import Modal from '../../components/Modal';
import Field from '../../components/Field';
import Timeline from '../../components/Timeline';
import StarRating from '../../components/StarRating';
import { OverdueBadge, PriorityBadge, StatusBadge } from '../../components/Badges';
import { Alert, ErrorState, Loading } from '../../components/States';
import { PRIORITIES, ROOM_HINT, ROOM_RE, fmtDate, statusLabel, validateImage } from '../../utils/format';

/** PUT /api/issues/{id}/status. Plain form fields normally; multipart only when a photo is attached. */
function putStatus(id, status, comment, photo) {
  if (photo) {
    const fd = new FormData();
    fd.append('status', status);
    if (comment) fd.append('comment', comment);
    fd.append('photo', photo);
    return api.put(`/api/issues/${id}/status`, fd);
  }
  const body = new URLSearchParams();
  body.set('status', status);
  if (comment) body.set('comment', comment);
  return api.put(`/api/issues/${id}/status`, body);
}

function Detail({ label, children }) {
  return (<div className="detail-row"><dt>{label}</dt><dd>{children || <span className="muted">-</span>}</dd></div>);
}

export default function IssueDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const location = useLocation();
  const { data: issue, loading, error, reload } = useFetch(`/api/issues/${id}`);
  const [notice, setNotice] = useState(location.state?.notice || '');
  const [actionError, setActionError] = useState('');
  const [busy, setBusy] = useState(false);
  const [modal, setModal] = useState(null);   // 'assign' | 'reject' | 'resolve' | 'reopen' | 'edit'

  useEffect(() => { setNotice(location.state?.notice || ''); }, [id]); // eslint-disable-line react-hooks/exhaustive-deps

  const role = user.role;
  const backPath = role === 'STUDENT' ? '/student/issues' : role === 'STAFF' ? '/staff/issues' : '/admin/issues';

  async function run(fn, success) {
    setBusy(true); setActionError('');
    try {
      await fn();
      setNotice(success);
      setModal(null);
      reload();
    } catch (err) { setActionError(errorMessage(err)); } finally { setBusy(false); }
  }

  if (loading && !issue) return <Loading label="Loading issue..." />;
  if (error) return <ErrorState message={error} onRetry={reload} />;
  if (!issue) return null;

  const s = issue.status;
  const isStudent = role === 'STUDENT';
  const isStaff = role === 'STAFF';
  const isAdmin = role === 'ADMIN';

  return (
    <>
      <p className="crumb"><Link to={backPath}>&larr; Back to issues</Link></p>
      <PageHeader
        title={<><span className="mono">{issue.issueNumber}</span> {issue.title}</>}
        subtitle={<span className="badge-row"><PriorityBadge priority={issue.priority} /><StatusBadge status={s} /><OverdueBadge overdue={issue.overdue} /></span>}
      />
      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <Alert type="error" onClose={() => setActionError('')}>{actionError}</Alert>

      {/* ---- role actions ---- */}
      {isStudent && s === 'RESOLVED' && (
        <section className="callout">
          <div>
            <strong>Is the problem fixed?</strong>
            <p>{issue.resolutionNote}</p>
          </div>
          <div className="callout-actions">
            <button className="btn btn-primary" disabled={busy} onClick={() => run(() => api.put(`/api/issues/${id}/confirm`), 'Thank you! The issue is now closed.')}>Yes, confirm and close</button>
            <button className="btn btn-secondary" disabled={busy} onClick={() => setModal('reopen')}>No, it is not fixed</button>
          </div>
        </section>
      )}
      {isStaff && s === 'ASSIGNED' && (
        <section className="callout">
          <div><strong>This issue is assigned to you.</strong><p>Start work to let the student know it is being handled.</p></div>
          <div className="callout-actions">
            <button className="btn btn-primary" disabled={busy} onClick={() => run(() => putStatus(id, 'IN_PROGRESS'), 'Work started.')}>Start work</button>
          </div>
        </section>
      )}
      {isStaff && s === 'IN_PROGRESS' && (
        <section className="callout">
          <div><strong>Work in progress.</strong><p>When the problem is fixed, add a resolution note. The student will then confirm.</p></div>
          <div className="callout-actions"><button className="btn btn-primary" onClick={() => setModal('resolve')}>Mark as resolved</button></div>
        </section>
      )}
      {isAdmin && ['REPORTED', 'REOPENED'].includes(s) && (
        <section className="callout">
          <div><strong>{s === 'REOPENED' ? 'Reopened by the student - needs review.' : 'New issue - waiting for assignment.'}</strong>
            <p>Suggested department: {issue.suggestedDepartment || 'none set'}</p></div>
          <div className="callout-actions">
            <button className="btn btn-primary" onClick={() => setModal('assign')}>Assign</button>
            <button className="btn btn-secondary" onClick={() => setModal('reject')}>Reject</button>
          </div>
        </section>
      )}
      {isAdmin && ['ASSIGNED', 'IN_PROGRESS'].includes(s) && (
        <section className="callout calm">
          <div><strong>Assigned to {issue.assignedStaff?.name}</strong>
            {issue.assignedStaff?.status === 'INACTIVE' && <p className="warn-text">This staff member is inactive. Reassign the issue to an active staff member.</p>}</div>
          <div className="callout-actions"><button className="btn btn-secondary" onClick={() => setModal('assign')}>Reassign</button></div>
        </section>
      )}

      <div className="detail-layout">
        <div className="detail-main">
          <section className="panel">
            <h2>Description</h2>
            <p className="prewrap">{issue.description}</p>
            {issue.imageUrl && (
              <figure className="issue-photo">
                <a href={fileUrl(issue.imageUrl)} target="_blank" rel="noreferrer"><img src={fileUrl(issue.imageUrl)} alt="Photo attached to the issue" /></a>
                <figcaption>Photo attached by the student</figcaption>
              </figure>
            )}
          </section>

          {(issue.resolutionNote || issue.resolutionImageUrl) && (
            <section className="panel">
              <h2>Resolution</h2>
              <p className="prewrap">{issue.resolutionNote}</p>
              {issue.resolutionImageUrl && (
                <figure className="issue-photo">
                  <a href={fileUrl(issue.resolutionImageUrl)} target="_blank" rel="noreferrer"><img src={fileUrl(issue.resolutionImageUrl)} alt="Photo showing the resolution" /></a>
                  <figcaption>Photo added by staff</figcaption>
                </figure>
              )}
            </section>
          )}

          {isStudent && s === 'CLOSED' && (issue.feedback
            ? <section className="panel"><h2>Your feedback</h2><StarRating value={issue.feedback.rating} />{issue.feedback.comment && <p>{issue.feedback.comment}</p>}</section>
            : <FeedbackForm onSubmit={(payload) => run(() => api.post(`/api/issues/${id}/feedback`, payload), 'Thanks for your feedback!')} busy={busy} />)}
          {!isStudent && issue.feedback && (
            <section className="panel"><h2>Student feedback</h2><StarRating value={issue.feedback.rating} />{issue.feedback.comment && <p>{issue.feedback.comment}</p>}</section>
          )}

          <section className="panel">
            <h2>Timeline</h2>
            <Timeline entries={issue.timeline} />
          </section>
        </div>

        <aside className="detail-side">
          <section className="panel">
            <h2>Details</h2>
            <dl className="details">
              <Detail label="Category">{issue.categoryPath}</Detail>
              <Detail label="Location">{issue.locationPath}</Detail>
              <Detail label="Room No.">{issue.roomNumber}</Detail>
              <Detail label="Reported">{fmtDate(issue.createdAt)}</Detail>
              <Detail label="Last update">{fmtDate(issue.updatedAt)}</Detail>
              <Detail label="Target time">{`${issue.slaHours} h (demo target) - due ${fmtDate(issue.dueAt)}`}</Detail>
              {!isStudent && <Detail label="Reported by">{`${issue.studentName} (${issue.studentId})`}</Detail>}
              {!isStudent && <Detail label="Student contact">{issue.contactNumber || issue.studentPhone}</Detail>}
              {isStudent && <Detail label="Contact number">{issue.contactNumber}</Detail>}
              <Detail label="Department">{issue.assignedDepartment}</Detail>
              <Detail label="Assigned to">{issue.assignedStaff && `${issue.assignedStaff.name}${issue.assignedStaff.status === 'INACTIVE' ? ' (inactive)' : ''}`}</Detail>
              {isAdmin && <Detail label="Suggested dept.">{issue.suggestedDepartment}</Detail>}
              {issue.closedAt && <Detail label="Closed">{fmtDate(issue.closedAt)}</Detail>}
            </dl>
            {isStudent && s === 'REPORTED' && <button className="btn btn-secondary btn-block" onClick={() => setModal('edit')}>Edit issue</button>}
          </section>
        </aside>
      </div>

      {modal === 'assign' && <AssignModal issue={issue} onClose={() => setModal(null)} busy={busy}
        onSubmit={(payload) => run(() => api.put(`/api/issues/${id}/assign`, payload), 'Issue assigned.')} error={actionError} />}
      {modal === 'reject' && <TextModal title="Reject issue" label="Reason for rejection" required minLength={5} submitLabel="Reject issue" danger
        onClose={() => setModal(null)} busy={busy} error={actionError}
        onSubmit={(text) => run(() => putStatus(id, 'REJECTED', text), 'Issue rejected.')} />}
      {modal === 'reopen' && <TextModal title="Issue not resolved" label="What is still wrong?" required minLength={5} submitLabel="Reopen issue"
        onClose={() => setModal(null)} busy={busy} error={actionError}
        onSubmit={(text) => run(() => api.put(`/api/issues/${id}/reopen`, { reason: text }), 'The issue was reopened and sent back to the admin.')} />}
      {modal === 'resolve' && <ResolveModal onClose={() => setModal(null)} busy={busy} error={actionError}
        onSubmit={(note, photo) => run(() => putStatus(id, 'RESOLVED', note, photo), 'Marked as resolved. Waiting for student confirmation.')} />}
      {modal === 'edit' && <EditModal issue={issue} onClose={() => setModal(null)} busy={busy} error={actionError}
        onSubmit={(payload) => run(() => api.put(`/api/issues/${id}`, payload), 'Issue updated.')} />}
    </>
  );
}

function FeedbackForm({ onSubmit, busy }) {
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  return (
    <section className="panel">
      <h2>How was the resolution?</h2>
      <p className="muted">Your feedback helps improve campus services.</p>
      <StarRating value={rating} onChange={setRating} />
      <Field label="Comment (optional)"><textarea rows={3} maxLength={1000} value={comment} onChange={(e) => setComment(e.target.value)} /></Field>
      <button className="btn btn-primary" disabled={busy || rating === 0} onClick={() => onSubmit({ rating, comment })}>Submit feedback</button>
    </section>
  );
}

function TextModal({ title, label, required, minLength = 0, submitLabel, danger, onClose, onSubmit, busy, error }) {
  const [text, setText] = useState('');
  const [local, setLocal] = useState('');
  function submit(e) {
    e.preventDefault();
    if (required && text.trim().length < minLength) return setLocal(`Please enter at least ${minLength} characters.`);
    onSubmit(text.trim());
  }
  return (
    <Modal title={title} onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <Alert type="error">{local || error}</Alert>
        <Field label={label}><textarea rows={4} maxLength={500} value={text} onChange={(e) => setText(e.target.value)} autoFocus /></Field>
        <div className="form-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`} disabled={busy}>{busy ? 'Saving...' : submitLabel}</button>
        </div>
      </form>
    </Modal>
  );
}

function ResolveModal({ onClose, onSubmit, busy, error }) {
  const [note, setNote] = useState('');
  const [photo, setPhoto] = useState(null);
  const [local, setLocal] = useState('');
  function submit(e) {
    e.preventDefault();
    if (note.trim().length < 5) return setLocal('Please write a resolution note (at least 5 characters).');
    onSubmit(note.trim(), photo);
  }
  return (
    <Modal title="Mark as resolved" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <Alert type="error">{local || error}</Alert>
        <Field label="Resolution note" hint="Describe what was done."><textarea rows={4} maxLength={1000} value={note} onChange={(e) => setNote(e.target.value)} autoFocus /></Field>
        <Field label="Photo of the fix (optional)" hint="JPG or PNG, up to 5 MB">
          <input type="file" accept="image/jpeg,image/png" onChange={(e) => {
            const f = e.target.files?.[0] || null; const p = validateImage(f);
            if (p) { setLocal(p); e.target.value = ''; setPhoto(null); } else { setLocal(''); setPhoto(f); }
          }} />
        </Field>
        <div className="form-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Saving...' : 'Mark as resolved'}</button>
        </div>
      </form>
    </Modal>
  );
}

function EditModal({ issue, onClose, onSubmit, busy, error }) {
  const [form, setForm] = useState({ title: issue.title, description: issue.description, priority: issue.priority, contactNumber: issue.contactNumber || '', roomNumber: issue.roomNumber || '' });
  const [local, setLocal] = useState('');
  function submit(e) {
    e.preventDefault();
    if (form.title.trim().length < 5) return setLocal('Title must be at least 5 characters.');
    if (form.description.trim().length < 10) return setLocal('Description must be at least 10 characters.');
    if (form.roomNumber && !ROOM_RE.test(form.roomNumber)) return setLocal(`Room No.: ${ROOM_HINT}.`);
    onSubmit({ ...form, contactNumber: form.contactNumber || null, roomNumber: form.roomNumber.trim() });
  }
  return (
    <Modal title="Edit issue" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <Alert type="error">{local || error}</Alert>
        <Field label="Title"><input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} maxLength={150} /></Field>
        <Field label="Description"><textarea rows={5} value={form.description} maxLength={2000} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
        <Field label="Priority"><select value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>{PRIORITIES.map((p) => <option key={p} value={p}>{statusLabel(p)}</option>)}</select></Field>
        <Field label="Contact number"><input value={form.contactNumber} onChange={(e) => setForm({ ...form, contactNumber: e.target.value })} /></Field>
        <Field label="Room No." hint={ROOM_HINT}><input value={form.roomNumber} maxLength={40} onChange={(e) => setForm({ ...form, roomNumber: e.target.value })} /></Field>
        <p className="muted">You can edit an issue only until it has been assigned.</p>
        <div className="form-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Saving...' : 'Save changes'}</button>
        </div>
      </form>
    </Modal>
  );
}

/** Admin: pick a department (defaults to the suggested one), then one of its ACTIVE staff members. */
function AssignModal({ issue, onClose, onSubmit, busy, error }) {
  const departments = useFetch('/api/departments', { activeOnly: true });
  const [departmentId, setDepartmentId] = useState(String(issue.assignedDepartmentId || issue.suggestedDepartmentId || ''));
  const staff = useFetch(departmentId ? '/api/admin/staff' : null, { departmentId, status: 'ACTIVE' }, [departmentId]);
  const [staffId, setStaffId] = useState('');
  const [comment, setComment] = useState('');
  const [local, setLocal] = useState('');
  useEffect(() => { setStaffId(''); }, [departmentId]);

  function submit(e) {
    e.preventDefault();
    if (!staffId) return setLocal('Select a staff member.');
    onSubmit({ staffId: Number(staffId), comment });
  }
  const list = departmentId ? staff.data || [] : [];
  return (
    <Modal title={issue.assignedStaff ? 'Reassign issue' : 'Assign issue'} onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <Alert type="error">{local || error}</Alert>
        <Field label="Department" hint={issue.suggestedDepartment ? `Suggested from the category: ${issue.suggestedDepartment}` : undefined}>
          <select value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
            <option value="">Select department</option>
            {(departments.data || []).map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
          </select>
        </Field>
        <Field label="Staff member">
          <select value={staffId} onChange={(e) => setStaffId(e.target.value)} disabled={!departmentId}>
            <option value="">{departmentId && !staff.loading && list.length === 0 ? 'No active staff in this department' : 'Select staff member'}</option>
            {list.map((s) => <option key={s.id} value={s.id}>{s.name} - {s.designation} ({s.openAssignedIssues} open)</option>)}
          </select>
        </Field>
        <Field label="Note for the staff member (optional)"><textarea rows={2} maxLength={500} value={comment} onChange={(e) => setComment(e.target.value)} /></Field>
        <div className="form-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Saving...' : 'Assign'}</button>
        </div>
      </form>
    </Modal>
  );
}
