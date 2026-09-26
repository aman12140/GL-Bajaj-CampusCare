import { Link } from 'react-router-dom';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import { ErrorState, Loading } from '../../components/States';
import { useFetch } from '../../hooks/useFetch';
import { useAuth } from '../../context/AuthContext';
import { RecentIssues } from '../student/StudentDashboard';

export default function StaffDashboard() {
  const { user } = useAuth();
  const { data, loading, error, reload } = useFetch('/api/staff/dashboard');
  return (
    <>
      <PageHeader title={`Welcome, ${user.name.split(' ')[0]}`} subtitle={user.department ? `${user.department} - ${user.designation}` : 'Issues assigned to you'} />
      {loading ? <Loading /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <>
          <div className="stat-row">
            <StatCard label="Assigned to me" value={data.total} to="/staff/issues" />
            <StatCard label="Waiting to start" value={data.pending} tone="warn" to="/staff/issues?status=ASSIGNED" />
            <StatCard label="In progress" value={data.inProgress} tone="info" to="/staff/issues?group=IN_PROGRESS" />
            <StatCard label="Resolved" value={data.resolved} tone="good" to="/staff/issues?group=DONE" hint="Resolved or closed" />
          </div>
          <section className="panel">
            <div className="panel-head"><h2>Recently assigned</h2><Link to="/staff/issues">View all</Link></div>
            <RecentIssues issues={data.recent} basePath="/staff/issues" />
          </section>
        </>
      )}
    </>
  );
}
