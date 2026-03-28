import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import NotificationBell from '../NotificationBell';
import './Layout.css';

export default function Layout() {
  const { username, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-logo">SmartLift</div>
        <nav className="sidebar-nav">
          <NavLink to="/" end>Dashboard</NavLink>
          <NavLink to="/lifts">Lifts</NavLink>
          <NavLink to="/maintenances">Maintenances</NavLink>
          <NavLink to="/events">Events</NavLink>
          <NavLink to="/users">Users</NavLink>
          <NavLink to="/organizations">Organizations</NavLink>
          <NavLink to="/notifications">Notifications</NavLink>
        </nav>
      </aside>
      <div className="main-area">
        <header className="topbar">
          <div className="topbar-left" />
          <div className="topbar-right">
            <NotificationBell />
            <span className="topbar-user">{username}</span>
            <button className="topbar-logout" onClick={handleLogout}>Logout</button>
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
