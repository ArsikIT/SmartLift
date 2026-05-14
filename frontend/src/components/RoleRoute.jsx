import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole } from '../auth/permissions';

export default function RoleRoute({ allowedRoles, children }) {
  const auth = useAuth();

  if (!hasAnyRole(auth, allowedRoles)) {
    return <Navigate to="/" replace />;
  }

  return children;
}
