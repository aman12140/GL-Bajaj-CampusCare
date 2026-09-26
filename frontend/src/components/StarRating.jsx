import { StarIcon } from './Icons';

export default function StarRating({ value = 0, onChange, size = 22 }) {
  return (
    <div className="stars" role={onChange ? 'radiogroup' : 'img'} aria-label={`${value} out of 5 stars`}>
      {[1, 2, 3, 4, 5].map((n) => (
        onChange ? (
          <button type="button" key={n} className={`star ${n <= value ? 'on' : ''}`} onClick={() => onChange(n)}
                  role="radio" aria-checked={n === value} aria-label={`${n} star${n > 1 ? 's' : ''}`}>
            <StarIcon filled={n <= value} width={size} height={size} />
          </button>
        ) : (
          <span key={n} className={`star ${n <= value ? 'on' : ''}`}><StarIcon filled={n <= value} width={size} height={size} /></span>
        )
      ))}
    </div>
  );
}
