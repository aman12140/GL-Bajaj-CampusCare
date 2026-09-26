import { Link } from 'react-router-dom';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import { ErrorState, Loading } from '../../components/States';
import { PriorityBars, StatusPie, TrendLine } from '../../components/Charts';
import { useFetch } from '../../hooks/useFetch';
import { RecentIssues } from '../student/StudentDashboard';

export default function AdminDashboard() {
  const { data, loading, error, reload } = useFetch('/api/admin/dashboard');
  const recent = useFetch('/api/issues', { size: 6 });
  return (
    <>
      <PageHeader title="Admin dashboard" subtitle="Live overview of campus issues, calculated from the database."
                  actions={<Link className="btn btn-secondary" to="/admin/analytics">Open analytics</Link>} />
      {loading ? <Loading /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <>
          <div className="stat-row">
            <StatCard label="Total issues" value={data.cards.total} to="/admin/issues" />
            <StatCard label="Pending" value={data.cards.pending} tone="warn" to="/admin/issues?group=PENDING" hint="Reported, assigned or reopened" />
            <StatCard label="In progress" value={data.cards.inProgress} tone="info" to="/admin/issues?group=IN_PROGRESS" />
            <StatCard label="Resolved" value={data.cards.resolved} tone="good" to="/admin/issues?group=DONE" hint="Resolved or closed" />
            <StatCard label="Reopened" value={data.cards.reopened} tone="violet" to="/admin/issues?status=REOPENED" />
            <StatCard label="Critical (open)" value={data.cards.critical} tone="danger" />
          </div>
          <div className="stat-row small">
            <StatCard label="Overdue (demo SLA)" value={data.metrics.overdueIssues} tone="danger" />
            <StatCard label="Resolution rate" value={`${data.metrics.resolutionRatePercent}%`} tone="good" />
            <StatCard label="Avg. resolution time" value={data.metrics.averageResolutionHours == null ? '-' : `${data.metrics.averageResolutionHours} h`} />
            <StatCard label="Active staff" value={data.cards.activeStaff} to="/admin/staff?status=ACTIVE" />
            <StatCard label="Inactive staff" value={data.cards.inactiveStaff} to="/admin/staff?status=INACTIVE" />
          </div>
          <div className="two-col">
            <section className="panel"><h2>Issues by status</h2><StatusPie data={data.byStatus} /></section>
            <section className="panel"><h2>Issues by priority</h2><PriorityBars data={data.byPriority} /></section>
          </div>
          <section className="panel"><h2>Issues reported over time</h2><TrendLine data={data.trend} /></section>
          <section className="panel">
            <div className="panel-head"><h2>Latest issues</h2><Link to="/admin/issues">View all</Link></div>
            {recent.data ? <RecentIssues issues={recent.data.content} basePath="/admin/issues" /> : <Loading />}
          </section>
        </>
      )}
    </>
  );
}
