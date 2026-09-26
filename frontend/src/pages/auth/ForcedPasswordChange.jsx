import { useNavigate } from 'react-router-dom';
import AuthShell from '../../components/AuthShell';
import ChangePasswordForm from '../../components/ChangePasswordForm';
import { Alert } from '../../components/States';
import { HOME_BY_ROLE, useAuth } from '../../context/AuthContext';

/** Shown to staff who still use the temporary password given by the administrator. */
export default function ForcedPasswordChange() {
  const { refresh, logout } = useAuth();
  const navigate = useNavigate();
  return (
    <AuthShell title="Set a new password" subtitle="You are using a temporary password. Please choose your own password to continue.">
      <Alert type="info">For security, the app stays locked until you change the temporary password.</Alert>
      <ChangePasswordForm submitLabel="Save and continue" onDone={async () => { const u = await refresh(); navigate(HOME_BY_ROLE[u.role], { replace: true }); }} />
      <p className="center"><button type="button" className="btn btn-ghost" onClick={() => { logout(); navigate('/login'); }}>Log out instead</button></p>
    </AuthShell>
  );
}
