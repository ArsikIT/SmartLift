import { useTranslation } from 'react-i18next';
import { useAuth } from '../../context/AuthContext';

export default function Dashboard() {
  const { username } = useAuth();
  const { t } = useTranslation();

  return (
    <div>
      <h2>{t('dashboard.title')}</h2>
      <p>{t('dashboard.welcome', { username })}</p>
    </div>
  );
}
