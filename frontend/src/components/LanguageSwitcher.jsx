import { useTranslation } from 'react-i18next';

const LANGUAGES = [
  { code: 'ru', label: 'RU' },
  { code: 'en', label: 'EN' },
  { code: 'kk', label: 'KZ' },
];

export default function LanguageSwitcher() {
  const { i18n } = useTranslation();

  const handleChange = (e) => {
    const lang = e.target.value;
    i18n.changeLanguage(lang);
    localStorage.setItem('lang', lang);
  };

  return (
    <select
      value={i18n.language}
      onChange={handleChange}
      style={{
        padding: '4px 8px',
        border: '1px solid #d9d9d9',
        borderRadius: '4px',
        fontSize: '13px',
        background: '#fff',
        cursor: 'pointer',
      }}
    >
      {LANGUAGES.map((l) => (
        <option key={l.code} value={l.code}>{l.label}</option>
      ))}
    </select>
  );
}
