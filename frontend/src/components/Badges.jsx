import { statusLabel } from '../utils/format';

export function StatusBadge({ status }) {
  return <span className={`badge status-${(status || '').toLowerCase()}`}>{statusLabel(status)}</span>;
}

export function PriorityBadge({ priority }) {
  return <span className={`badge prio-${(priority || '').toLowerCase()}`}>{statusLabel(priority)}</span>;
}

export function OverdueBadge({ overdue }) {
  return overdue ? <span className="badge overdue" title="Past its target resolution time (demo SLA)">Overdue</span> : null;
}
