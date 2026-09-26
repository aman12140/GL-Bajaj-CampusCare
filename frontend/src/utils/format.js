export function fmtDate(iso) {
  if (!iso) return '-';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '-';
  return d.toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

export function fmtDay(iso) {
  if (!iso) return '-';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '-' : d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

export function statusLabel(s) {
  if (!s) return '';
  return s.toLowerCase().split('_').map((w) => w[0].toUpperCase() + w.slice(1)).join(' ');
}

export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
export const STATUSES = ['REPORTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED', 'REOPENED'];
export const RANGES = [
  { value: '', label: 'All time' },
  { value: 'TODAY', label: 'Today' },
  { value: 'WEEK', label: 'This week' },
  { value: 'MONTH', label: 'This month' },
  { value: 'QUARTER', label: 'Last 3 months' },
  { value: 'CUSTOM', label: 'Custom range' },
];

/** Removes empty values so they are not sent as query parameters. */
export function cleanParams(obj) {
  const out = {};
  Object.entries(obj).forEach(([k, v]) => { if (v !== '' && v !== null && v !== undefined) out[k] = v; });
  return out;
}

export const PHONE_RE = /^(\+91[- ]?)?[6-9]\d{9}$/;
export const ROOM_RE = /^[A-Za-z0-9][A-Za-z0-9 ./-]{0,39}$/;
export const ROOM_HINT = 'Letters, numbers, space, - / . only (max 40)';
export const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/;
export const PASSWORD_HINT = 'At least 8 characters with a letter and a number';

export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

/** Returns an error message, or '' when the file is an acceptable JPG/PNG up to 5 MB. */
export function validateImage(file) {
  if (!file) return '';
  if (!['image/jpeg', 'image/png'].includes(file.type)) return 'Only JPG or PNG images are allowed.';
  if (file.size > MAX_IMAGE_BYTES) return 'The image must be 5 MB or smaller.';
  return '';
}
