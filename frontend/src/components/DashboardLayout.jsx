import { useEffect, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import Brand, { Logo } from './Brand';
import NotificationBell from './NotificationBell';
import { HOME_BY_ROLE, useAuth } from '../context/AuthContext';
import { APP_NAME } from '../config/branding';
import { statusLabel } from '../utils/format';
import { LogoutIcon, MenuIcon } from './Icons';

const NAV = {
  STUDENT: [
    { to: '/student', label: 'Dashboard', exact: true },
    { to: '/student/report', label: 'Report an issue' },
    { to: '/student/issues', label: 'My issues' },
    { to: '/student/notifications', label: 'Notifications' },
    { to: '/student/profile', label: 'Profile' },
  ],
  STAFF: [
    { to: '/staff', label: 'Dashboard', exact: true },
    { to: '/staff/issues', label: 'My assigned issues' },
    { to: '/staff/issues?group=IN_PROGRESS', label: 'In progress' },
    { to: '/staff/issues?group=DONE', label: 'Resolved' },
    { to: '/staff/notifications', label: 'Notifications' },
    { to: '/staff/profile', label: 'Profile' },
  ],
  ADMIN: [
    { to: '/admin', label: 'Dashboard', exact: true },
    { to: '/admin/issues', label: 'All issues' },
    { to: '/admin/staff', label: 'Staff management' },
    { to: '/admin/departments', label: 'Departments' },
    { to: '/admin/categories', label: 'Categories' },
    { to: '/admin/locations', label: 'Locations' },
    { to: '/admin/analytics', label: 'Analytics' },
    { to: '/admin/notifications', label: 'Notifications' },
    { to: '/admin/profile', label: 'Profile' },
  ],
};

function isActive(item, loc) {
  const [path, query] = item.to.split('?');
  if (query) return loc.pathname === path && loc.search === `?${query}`;
  if (item.exact) return loc.pathname === path;
  const under = loc.pathname === path || loc.pathname.startsWith(`${path}/`);
  if (path === '/staff/issues') return under && !loc.search.includes('group=');
  return under;
}

export default function DashboardLayout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const home = HOME_BY_ROLE[user.role];

  useEffect(() => { setOpen(false); }, [location.pathname, location.search]);

  return (
    <div className="app-shell">
      <a className="skip-link" href="#content">Skip to content</a>
      <aside className={`sidebar ${open ? 'open' : ''}`} aria-label="Sidebar">
        <div className="sidebar-brand"><Brand to={home} logoHeight={46} /></div>
        <nav aria-label="Main navigation">
          {NAV[user.role].map((item) => (
            <Link key={item.to} to={item.to} className={`nav-link ${isActive(item, location) ? 'active' : ''}`}
                  aria-current={isActive(item, location) ? 'page' : undefined}>
              {item.label}
            </Link>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className="who">
            <strong>{user.name}</strong>
            <span>{statusLabel(user.role)}</span>
          </div>
          <button type="button" className="btn btn-secondary btn-block" onClick={() => { logout(); navigate('/login'); }}>
            <LogoutIcon /> Log out
          </button>
        </div>
      </aside>
      {open && <div className="scrim" onClick={() => setOpen(false)} aria-hidden="true" />}

      <div className="main">
        <header className="topbar">
          <button type="button" className="icon-btn menu-btn" onClick={() => setOpen(true)} aria-label="Open menu"><MenuIcon /></button>
          <span className="topbar-title">{APP_NAME}</span>
          <div className="topbar-actions">
            <NotificationBell to={`${home}/notifications`} />
          </div>
        </header>
        <main id="content" className="content"><Outlet /></main>
        <footer className="app-footer">
          <Logo height={28} />
          <span>GL Bajaj CampusCare - college project. Demo data is fictional.</span>
        </footer>
      </div>
    </div>
  );
}
