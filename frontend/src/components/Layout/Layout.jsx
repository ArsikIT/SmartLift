import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../context/AuthContext';
import NotificationBell from '../NotificationBell';
import LanguageSwitcher from '../LanguageSwitcher';
import './Layout.css';

export default function Layout() {
  const { username, logout } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-logo">{t('app.name')}</div>
        <nav className="sidebar-nav">
          <NavLink to="/" end>{t('nav.dashboard')}</NavLink>
          <NavLink to="/lifts">{t('nav.lifts')}</NavLink>
          <NavLink to="/maintenances">{t('nav.maintenances')}</NavLink>
          <NavLink to="/events">{t('nav.events')}</NavLink>
          <NavLink to="/users">{t('nav.users')}</NavLink>
          <NavLink to="/organizations">{t('nav.organizations')}</NavLink>
          <NavLink to="/notifications">{t('nav.notifications')}</NavLink>
          <NavLink to="/scan">{t('nav.scanner')}</NavLink>
        </nav>
      </aside>
      <div className="main-area">
        <header className="topbar">
          <div className="topbar-left" />
          <div className="topbar-right">
            <LanguageSwitcher />
            <NotificationBell />
            <span className="topbar-user">{username}</span>
            <button className="topbar-logout" onClick={handleLogout}>{t('auth.logout')}</button>
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
