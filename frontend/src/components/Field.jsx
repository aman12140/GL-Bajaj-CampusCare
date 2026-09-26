/** Label + control + hint + error message, wired for accessibility. */
export default function Field({ label, error, hint, children, className = '' }) {
  return (
    <label className={`field ${error ? 'has-error' : ''} ${className}`}>
      <span className="field-label">{label}</span>
      {children}
      {hint && !error && <span className="hint">{hint}</span>}
      {error && <span className="field-error" role="alert">{error}</span>}
    </label>
  );
}
