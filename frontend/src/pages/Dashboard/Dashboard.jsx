import { useAuth } from '../../context/AuthContext';

export default function Dashboard() {
  const { username } = useAuth();

  return (
    <div>
      <h2>Dashboard</h2>
      <p>Welcome, {username}!</p>
    </div>
  );
}
