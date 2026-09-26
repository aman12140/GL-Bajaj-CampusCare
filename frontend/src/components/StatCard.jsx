import { Link } from 'react-router-dom';

/** A dashboard figure. When "to" is given the whole tile is a link to the matching filtered list. */
export default function StatCard({ label, value, tone = 'neutral', to, hint }) {
  const body = (
    <>
      <span className="stat-value">{value ?? '-'}</span>
      <span className="stat-label">{label}</span>
      {hint && <span className="stat-hint">{hint}</span>}
    </>
  );
  return to ? <Link to={to} className={`stat tone-${tone}`}>{body}</Link> : <div className={`stat tone-${tone}`}>{body}</div>;
}
