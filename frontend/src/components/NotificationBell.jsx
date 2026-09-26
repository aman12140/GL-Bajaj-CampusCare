import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { BellIcon } from './Icons';

/** Polls the unread count every 30 seconds (no WebSockets needed for this project). */
export default function NotificationBell({ to }) {
  const [count, setCount] = useState(0);
  useEffect(() => {
    let alive = true;
    const load = () => api.get('/api/notifications/unread-count').then((r) => alive && setCount(r.data.count)).catch(() => {});
    load();
    const id = setInterval(load, 30000);
    return () => { alive = false; clearInterval(id); };
  }, []);
  return (
    <Link to={to} className="icon-btn bell" aria-label={`Notifications${count ? `, ${count} unread` : ''}`}>
      <BellIcon />
      {count > 0 && <span className="bell-count">{count > 99 ? '99+' : count}</span>}
    </Link>
  );
}
