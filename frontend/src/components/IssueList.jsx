import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import api, { errorMessage } from '../api/client';
import { useDebounced, useFetch } from '../hooks/useFetch';
import { cleanParams, fmtDate, PRIORITIES, RANGES, STATUSES, statusLabel } from '../utils/format';
import { OverdueBadge, PriorityBadge, StatusBadge } from './Badges';
import { EmptyState, ErrorState, Loading } from './States';
import Pagination from './Pagination';

const EMPTY = { q: '', status: '', priority: '', categoryId: '', departmentId: '', locationId: '', staffId: '', range: '', fromDate: '', toDate: '' };

/**
 * One list component for all roles.
 *  mode = 'student' | 'staff' | 'admin' - picks the columns and which filters are shown.
 *  The server decides which issues each role may see; the filters only narrow the list further.
 */
export default function IssueList({ endpoint, basePath, mode }) {
  const [searchParams] = useSearchParams();
  const group = searchParams.get('group') || '';
  const urlStaffId = searchParams.get('staffId') || '';
  const urlStatus = searchParams.get('status') || '';
  const [filters, setFilters] = useState({ ...EMPTY, staffId: urlStaffId, status: urlStatus });
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tick, setTick] = useState(0);
  const admin = mode === 'admin';

  const categories = useFetch('/api/categories');
  const departments = useFetch(admin ? '/api/departments' : null);
  const locations = useFetch(admin ? '/api/locations' : null);
  const staff = useFetch(admin ? '/api/admin/staff' : null);

  useEffect(() => { setFilters((f) => ({ ...f, staffId: urlStaffId, status: urlStatus })); setPage(0); }, [group, urlStaffId, urlStatus]);

  const q = useDebounced(filters.q);
  const params = useMemo(
    () => cleanParams({ ...filters, q, group, page, size: 10 }),
    [filters, q, group, page]
  );

  useEffect(() => {
    let cancelled = false;
    setLoading(true); setError('');
    api.get(endpoint, { params })
      .then((r) => { if (!cancelled) setResult(r.data); })
      .catch((e) => { if (!cancelled) setError(errorMessage(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [endpoint, params, tick]);

  const set = (k) => (e) => { setFilters({ ...filters, [k]: e.target.value }); setPage(0); };
  const topCategories = (categories.data || []).filter((c) => !c.parentId);
  const buildings = (locations.data || []).filter((l) => l.level === 'BUILDING');
  const hasFilters = Object.values(filters).some(Boolean) || group;

  return (
    <div>
      <div className="filters" role="search">
        <label className="grow">Search
          <input type="search" placeholder="Issue number, title, description or room no." value={filters.q} onChange={set('q')} />
        </label>
        <label>Status
          <select value={filters.status} onChange={set('status')}>
            <option value="">{group ? `Group: ${statusLabel(group)}` : 'All'}</option>
            {STATUSES.map((s) => <option key={s} value={s}>{statusLabel(s)}</option>)}
          </select>
        </label>
        <label>Priority
          <select value={filters.priority} onChange={set('priority')}>
            <option value="">All</option>
            {PRIORITIES.map((p) => <option key={p} value={p}>{statusLabel(p)}</option>)}
          </select>
        </label>
        <label>Category
          <select value={filters.categoryId} onChange={set('categoryId')}>
            <option value="">All</option>
            {topCategories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>
        {admin && (
          <>
            <label>Department
              <select value={filters.departmentId} onChange={set('departmentId')}>
                <option value="">All</option>
                {(departments.data || []).map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
              </select>
            </label>
            <label>Building
              <select value={filters.locationId} onChange={set('locationId')}>
                <option value="">All</option>
                {buildings.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
              </select>
            </label>
            <label>Assigned staff
              <select value={filters.staffId} onChange={set('staffId')}>
                <option value="">All</option>
                {(staff.data || []).map((s) => <option key={s.id} value={s.id}>{s.name}{s.status === 'INACTIVE' ? ' (inactive)' : ''}</option>)}
              </select>
            </label>
          </>
        )}
        <label>Reported
          <select value={filters.range} onChange={set('range')}>
            {RANGES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
          </select>
        </label>
        {filters.range === 'CUSTOM' && (
          <>
            <label>From<input type="date" value={filters.fromDate} onChange={set('fromDate')} /></label>
            <label>To<input type="date" value={filters.toDate} onChange={set('toDate')} /></label>
          </>
        )}
        {hasFilters && (
          <button type="button" className="btn btn-ghost" onClick={() => { setFilters({ ...EMPTY }); setPage(0); }}>Clear filters</button>
        )}
      </div>

      {group && <p className="muted">Showing: <strong>{statusLabel(group)}</strong> issues. Use "Clear filters" or the sidebar to see all.</p>}

      {loading ? <Loading label="Loading issues..." /> : error ? <ErrorState message={error} onRetry={() => setTick((t) => t + 1)} /> :
        !result || result.content.length === 0 ? (
          <EmptyState title="No issues found">
            {hasFilters ? 'Try changing or clearing the filters.' : mode === 'student' ? 'You have not reported any issue yet.' : 'Nothing to show here yet.'}
          </EmptyState>
        ) : (
          <>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Issue</th><th>Title</th>
                    {admin && <th>Student</th>}
                    <th>Category</th><th>Location</th><th>Priority</th><th>Status</th>
                    {admin && <th>Assigned to</th>}
                    <th>Reported</th>
                  </tr>
                </thead>
                <tbody>
                  {result.content.map((i) => (
                    <tr key={i.id}>
                      <td><Link to={`${basePath}/${i.id}`} className="mono">{i.issueNumber}</Link></td>
                      <td className="cell-title"><Link to={`${basePath}/${i.id}`}>{i.title}</Link></td>
                      {admin && <td>{i.studentName}</td>}
                      <td>{i.categoryName}</td>
                      <td>{i.locationPath}</td>
                      <td><PriorityBadge priority={i.priority} /></td>
                      <td><StatusBadge status={i.status} /> <OverdueBadge overdue={i.overdue} /></td>
                      {admin && <td>{i.assignedStaffName || <span className="muted">Unassigned</span>}</td>}
                      <td>{fmtDate(i.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage} />
          </>
        )}
    </div>
  );
}
