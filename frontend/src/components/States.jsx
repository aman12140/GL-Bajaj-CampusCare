export function Loading({ label = 'Loading...' }) {
  return (
    <div className="state" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

export function EmptyState({ title, children }) {
  return (
    <div className="state empty">
      <strong>{title}</strong>
      {children && <p>{children}</p>}
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="state error" role="alert">
      <strong>Something went wrong</strong>
      <p>{message}</p>
      {onRetry && <button type="button" className="btn btn-secondary" onClick={onRetry}>Try again</button>}
    </div>
  );
}

export function Alert({ type = 'info', children, onClose }) {
  if (!children) return null;
  return (
    <div className={`alert alert-${type}`} role={type === 'error' ? 'alert' : 'status'}>
      <span>{children}</span>
      {onClose && <button type="button" className="alert-close" onClick={onClose} aria-label="Dismiss">&times;</button>}
    </div>
  );
}
