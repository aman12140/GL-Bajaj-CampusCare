import { useState } from 'react';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import { Alert, ErrorState, Loading } from '../../components/States';
import { CountBars, PriorityBars, StatusPie, TrendLine } from '../../components/Charts';
import { downloadFile, errorMessage } from '../../api/client';
import { useFetch } from '../../hooks/useFetch';
import { PRIORITIES, RANGES, STATUSES, cleanParams, statusLabel } from '../../utils/format';

const EMPTY = { range: '', fromDate: '', toDate: '', categoryId: '', departmentId: '', locationId: '', priority: '', status: '', staffId: '' };

export default function Analytics() {
  const [filters, setFilters] = useState(EMPTY);
  const [exportError, setExportError] = useState('');
  const [exporting, setExporting] = useState('');
  const params = cleanParams(filters);
  const { data, loading, error, reload } = useFetch('/api/admin/analytics', params, [JSON.stringify(params)]);
  const categories = useFetch('/api/categories');
  const departments = useFetch('/api/departments');
  const locations = useFetch('/api/locations');
  const staff = useFetch('/api/admin/staff');
  const set = (k) => (e) => setFilters({ ...filters, [k]: e.target.value });

  async function exportAs(kind) {
    setExportError(''); setExporting(kind);
    try { await downloadFile(`/api/admin/export/${kind}`, params, `campuscare-report.${kind}`); }
    catch (err) { setExportError(errorMessage(err)); }
    finally { setExporting(''); }
  }

  return (
    <>
      <PageHeader title="Analytics" subtitle="All figures are calculated from real issue records that match the filters."
        actions={<>
          <button className="btn btn-secondary" disabled={!!exporting} onClick={() => exportAs('csv')}>{exporting === 'csv' ? 'Preparing...' : 'Export CSV'}</button>
          <button className="btn btn-primary" disabled={!!exporting} onClick={() => exportAs('pdf')}>{exporting === 'pdf' ? 'Preparing...' : 'Export PDF'}</button>
        </>} />
      <Alert type="error">{exportError}</Alert>

      <div className="filters">
        <label>Period
          <select value={filters.range} onChange={set('range')}>{RANGES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}</select>
        </label>
        {filters.range === 'CUSTOM' && (<>
          <label>From<input type="date" value={filters.fromDate} onChange={set('fromDate')} /></label>
          <label>To<input type="date" value={filters.toDate} onChange={set('toDate')} /></label>
        </>)}
        <label>Category
          <select value={filters.categoryId} onChange={set('categoryId')}><option value="">All</option>
            {(categories.data || []).filter((c) => !c.parentId).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select>
        </label>
        <label>Department
          <select value={filters.departmentId} onChange={set('departmentId')}><option value="">All</option>
            {(departments.data || []).map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}</select>
        </label>
        <label>Building
          <select value={filters.locationId} onChange={set('locationId')}><option value="">All</option>
            {(locations.data || []).filter((l) => l.level === 'BUILDING').map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}</select>
        </label>
        <label>Priority
          <select value={filters.priority} onChange={set('priority')}><option value="">All</option>{PRIORITIES.map((p) => <option key={p} value={p}>{statusLabel(p)}</option>)}</select>
        </label>
        <label>Status
          <select value={filters.status} onChange={set('status')}><option value="">All</option>{STATUSES.map((s) => <option key={s} value={s}>{statusLabel(s)}</option>)}</select>
        </label>
        <label>Staff
          <select value={filters.staffId} onChange={set('staffId')}><option value="">All</option>
            {(staff.data || []).map((s) => <option key={s.id} value={s.id}>{s.name}{s.status === 'INACTIVE' ? ' (inactive)' : ''}</option>)}</select>
        </label>
        <button type="button" className="btn btn-ghost" onClick={() => setFilters(EMPTY)}>Reset</button>
      </div>

      {loading ? <Loading /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <>
          <p className="muted">Period: <strong>{data.rangeLabel}</strong></p>
          <div className="stat-row">
            <StatCard label="Issues" value={data.cards.total} />
            <StatCard label="Resolved / closed" value={data.cards.resolved} tone="good" />
            <StatCard label="Resolution rate" value={`${data.metrics.resolutionRatePercent}%`} tone="good" />
            <StatCard label="Avg. resolution time" value={data.metrics.averageResolutionHours == null ? '-' : `${data.metrics.averageResolutionHours} h`} />
            <StatCard label="Overdue (demo SLA)" value={data.metrics.overdueIssues} tone="danger" />
            <StatCard label="Ever reopened" value={data.metrics.everReopenedIssues} tone="violet" />
          </div>
          <section className="panel"><h2>Issues reported over time</h2><TrendLine data={data.trend} /></section>
          <div className="two-col">
            <section className="panel"><h2>By status</h2><StatusPie data={data.byStatus} /></section>
            <section className="panel"><h2>By priority</h2><PriorityBars data={data.byPriority} /></section>
            <section className="panel"><h2>By category</h2><CountBars data={data.byCategory} /></section>
            <section className="panel"><h2>By building / location</h2><CountBars data={data.byLocation} /></section>
          </div>
          <section className="panel"><h2>By department</h2><CountBars data={data.byDepartment} /></section>
          <p className="muted small">SLA targets are configurable demo values (Low 72 h, Medium 48 h, High 24 h, Critical 6 h) and are not official GL Bajaj policy.</p>
        </>
      )}
    </>
  );
}
