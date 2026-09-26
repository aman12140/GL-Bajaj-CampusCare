import { Navigate, useLocation } from 'react-router-dom';
import { HOME_BY_ROLE, useAuth } from '../context/AuthContext';
import { Loading } from './States';

/**
 * Front-end route guard: it only decides what the user SEES. The real protection is the Spring Security
 * configuration on the server - every API call is checked again there.
 */
export default function ProtectedRoute({ role, children }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <Loading label="Checking your session..." />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  if (user.passwordChangeRequired && location.pathname !== '/change-password') return <Navigate to="/change-password" replace />;
  if (role && user.role !== role) return <Navigate to={HOME_BY_ROLE[user.role]} replace />;
  return children;
}
