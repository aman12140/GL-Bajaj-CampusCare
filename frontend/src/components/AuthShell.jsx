import Brand from './Brand';

/** Common frame for login / register / forgot / reset pages. */
export default function AuthShell({ title, subtitle, children, footer, wide = false }) {
  return (
    <div className="auth-page">
      <header className="auth-header"><Brand /></header>
      <main className="auth-main">
        <section className={`auth-card ${wide ? 'wide' : ''}`}>
          <h1>{title}</h1>
          {subtitle && <p className="muted">{subtitle}</p>}
          {children}
          {footer && <div className="auth-footer">{footer}</div>}
        </section>
      </main>
    </div>
  );
}
