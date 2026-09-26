import { useNavigate } from 'react-router-dom';
import api from '../../api/client';
import { useAuth, HOME_BY_ROLE } from '../../context/AuthContext';
import { useFetch } from '../../hooks/useFetch';
import PageHeader from '../../components/PageHeader';
import { EmptyState, ErrorState, Loading } from '../../components/States';
import { fmtDate } from '../../utils/format';

export default function Notifications() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { data, loading, error, reload } = useFetch('/api/notifications');

  async function open(n) {
    if (!n.read) await api.put(`/api/notifications/${n.id}/read`).catch(() => {});
    if (n.issueId) navigate(`${HOME_BY_ROLE[user.role]}/issues/${n.issueId}`);
    else reload();
  }

  return (
    <>
      <PageHeader title="Notifications" subtitle="Updates about your issues."
        actions={data?.some((n) => !n.read) && <button className="btn btn-secondary" onClick={async () => { await api.put('/api/notifications/read-all'); reload(); }}>Mark all as read</button>} />
      {loading ? <Loading /> : error ? <ErrorState message={error} onRetry={reload} /> : data.length === 0 ? (
        <EmptyState title="No notifications yet">You will see updates here when something changes.</EmptyState>
      ) : (
        <ul className="notif-list">
          {data.map((n) => (
            <li key={n.id} className={n.read ? '' : 'unread'}>
              <button type="button" onClick={() => open(n)}>
                <strong>{n.title}</strong>
                <span>{n.message}</span>
                <small className="muted">{fmtDate(n.createdAt)}</small>
              </button>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
