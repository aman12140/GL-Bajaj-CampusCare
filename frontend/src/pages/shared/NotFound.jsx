import { Link } from 'react-router-dom';
import { EmptyState } from '../../components/States';

export default function NotFound() {
  return (
    <div className="center-page">
      <EmptyState title="Page not found">The page you are looking for does not exist.</EmptyState>
      <p className="center"><Link className="btn btn-primary" to="/">Go to the home page</Link></p>
    </div>
  );
}
