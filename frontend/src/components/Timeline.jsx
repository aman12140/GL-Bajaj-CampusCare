import { fmtDate, statusLabel } from '../utils/format';

/** Vertical history of an issue. The dot colour follows the status colour used everywhere else. */
export default function Timeline({ entries = [] }) {
  if (!entries.length) return <p className="muted">No history yet.</p>;
  return (
    <ol className="timeline">
      {entries.map((e, i) => (
        <li key={i} className={`tl-item status-dot-${e.newStatus.toLowerCase()}`}>
          <div className="tl-head">
            <strong>{e.oldStatus && e.oldStatus === e.newStatus ? 'Update' : statusLabel(e.newStatus)}</strong>
            <span className="muted">{fmtDate(e.changedAt)}</span>
          </div>
          <div className="tl-actor">{e.actorName} <span className="muted">({statusLabel(e.actorRole)})</span></div>
          {e.comment && <p className="tl-comment">{e.comment}</p>}
        </li>
      ))}
    </ol>
  );
}
