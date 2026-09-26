import { useEffect, useRef } from 'react';
import { CloseIcon } from './Icons';

/** Simple accessible dialog: Esc closes it, focus moves into it, background does not scroll. */
export default function Modal({ title, onClose, children, wide = false }) {
  const ref = useRef(null);
  useEffect(() => {
    const previous = document.activeElement;
    const onKey = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', onKey);
    document.body.style.overflow = 'hidden';
    ref.current?.focus();
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
      previous?.focus?.();
    };
  }, [onClose]);

  return (
    <div className="modal-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className={`modal ${wide ? 'modal-wide' : ''}`} role="dialog" aria-modal="true" aria-label={title} tabIndex={-1} ref={ref}>
        <div className="modal-head">
          <h2>{title}</h2>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close dialog"><CloseIcon /></button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
}
