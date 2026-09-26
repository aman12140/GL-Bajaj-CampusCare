import { Link } from 'react-router-dom';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import { Alert, ErrorState, Loading } from '../../components/States';
import { OverdueBadge, PriorityBadge, StatusBadge } from '../../components/Badges';
import { useFetch } from '../../hooks/useFetch';
import { useAuth } from '../../context/AuthContext';
import { fmtDate } from '../../utils/format';

export function RecentIssues({ issues, basePath }) {
  if (!issues.length) return <p className="muted">No issues yet.</p>;
  return (
    <div className="table-wrap">
      <table className="table">
        <thead><tr><th>Issue</th><th>Title</th><th>Priority</th><th>Status</th><th>Reported</th></tr></thead>
        <tbody>
          {issues.map((i) => (
            <tr key={i.id}>
              <td><Link className="mono" to={`${basePath}/${i.id}`}>{i.issueNumber}</Link></td>
              <td className="cell-title"><Link to={`${basePath}/${i.id}`}>{i.title}</Link></td>
              <td><PriorityBadge priority={i.priority} /></td>
              <td><StatusBadge status={i.status} /> <OverdueBadge overdue={i.overdue} /></td>
              <td>{fmtDate(i.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function StudentDashboard() {
  const { user } = useAuth();
  const { data, loading, error, reload } = useFetch('/api/student/dashboard');
  return (
    <>
      <PageHeader title={`Welcome, ${user.name.split(' ')[0]}`} subtitle="Report campus problems and follow them until they are fixed."
                  actions={<Link className="btn btn-primary" to="/student/report">Report an issue</Link>} />
      {loading ? <Loading /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <>
          {data.awaitingConfirmation > 0 && (
            <Alert type="warning">
              {data.awaitingConfirmation} issue{data.awaitingConfirmation > 1 ? 's are' : ' is'} marked resolved and waiting for your confirmation.{' '}
              <Link to="/student/issues?status=RESOLVED">Review now</Link>
            </Alert>
          )}
          <div className="stat-row">
            <StatCard label="My issues" value={data.total} to="/student/issues" />
            <StatCard label="Pending" value={data.pending} tone="warn" to="/student/issues?group=PENDING" hint="Reported, assigned or reopened" />
            <StatCard label="In progress" value={data.inProgress} tone="info" to="/student/issues?group=IN_PROGRESS" />
            <StatCard label="Resolved" value={data.resolved} tone="good" to="/student/issues?group=DONE" hint="Resolved or closed" />
          </div>
          <section className="panel">
            <div className="panel-head"><h2>Recent issues</h2><Link to="/student/issues">View all</Link></div>
            <RecentIssues issues={data.recent} basePath="/student/issues" />
          </section>
        </>
      )}
    </>
  );
}
