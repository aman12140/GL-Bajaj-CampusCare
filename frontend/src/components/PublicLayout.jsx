import { Link, Outlet } from 'react-router-dom';
import { useState } from 'react';
import Brand, { Logo } from './Brand';
import { HOME_BY_ROLE, useAuth } from '../context/AuthContext';
import { APP_NAME, APP_TAGLINE } from '../config/branding';
import { CloseIcon, MenuIcon } from './Icons';

const LINKS = [
  { href: '/#about', label: 'About' },
  { href: '/#features', label: 'Features' },
  { href: '/#how-it-works', label: 'How it works' },
  { href: '/#contact', label: 'Contact' },
];

export function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="footer-inner">
        <div className="footer-brand">
          <Logo height={56} />
          <div>
            <strong>{APP_NAME}</strong>
            <p>{APP_TAGLINE}</p>
          </div>
        </div>
        <p className="footer-note">
          A B.Tech college project built for the GL Bajaj campus community. Sample data shown in demo mode is fictional.
          This project is not an official GL Bajaj Group of Institutions system.
        </p>
      </div>
    </footer>
  );
}

export default function PublicLayout() {
  const { user } = useAuth();
  const [open, setOpen] = useState(false);
  return (
    <div className="public">
      <a className="skip-link" href="#main">Skip to content</a>
      <header className="public-header">
        <div className="public-header-inner">
          <Brand />
          <button type="button" className="icon-btn nav-toggle" onClick={() => setOpen(!open)} aria-expanded={open} aria-label="Toggle menu">
            {open ? <CloseIcon /> : <MenuIcon />}
          </button>
          <nav className={`public-nav ${open ? 'open' : ''}`} aria-label="Primary">
            {LINKS.map((l) => <a key={l.href} href={l.href} onClick={() => setOpen(false)}>{l.label}</a>)}
            {user ? (
              <Link to={HOME_BY_ROLE[user.role]} className="btn btn-primary">Go to dashboard</Link>
            ) : (
              <>
                <Link to="/login" className="btn btn-secondary">Login</Link>
                <Link to="/register" className="btn btn-primary">Student registration</Link>
              </>
            )}
          </nav>
        </div>
      </header>
      <main id="main"><Outlet /></main>
      <SiteFooter />
    </div>
  );
}
