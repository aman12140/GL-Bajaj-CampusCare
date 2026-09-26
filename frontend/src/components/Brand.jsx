import { Link } from 'react-router-dom';
import { APP_NAME, APP_TAGLINE, LOGO_ALT, LOGO_HEIGHT, LOGO_URL, LOGO_WIDTH } from '../config/branding';

/** The supplied logo, shown at a fixed height; width follows automatically so proportions are never changed. */
export function Logo({ height = 48, className = '' }) {
  return (
    <img
      className={`brand-logo ${className}`}
      src={LOGO_URL}
      alt={LOGO_ALT}
      width={LOGO_WIDTH}
      height={LOGO_HEIGHT}
      style={{ height, width: 'auto' }}
    />
  );
}

/** Logo + application name (+ subtitle). */
export default function Brand({ to = '/', logoHeight = 48, showTagline = true }) {
  return (
    <Link to={to} className="brand" aria-label={`${APP_NAME} home`}>
      <Logo height={logoHeight} />
      <span className="brand-text">
        <strong>{APP_NAME}</strong>
        {showTagline && <small>{APP_TAGLINE}</small>}
      </span>
    </Link>
  );
}
