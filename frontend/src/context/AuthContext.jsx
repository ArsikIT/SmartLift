import { createContext, useContext, useMemo, useState } from 'react';

const AuthContext = createContext(null);

function loadStoredAuth() {
  const token = localStorage.getItem('token');
  const username = localStorage.getItem('username');
  const authUserRaw = localStorage.getItem('authUser');

  let authUser = null;
  if (authUserRaw) {
    try {
      authUser = JSON.parse(authUserRaw);
    } catch {
      localStorage.removeItem('authUser');
    }
  }

  return { token, username, authUser };
}

export function AuthProvider({ children }) {
  const [{ token: initialToken, username: initialUsername, authUser: initialAuthUser }] = useState(loadStoredAuth);
  const [token, setToken] = useState(initialToken);
  const [username, setUsername] = useState(initialUsername);
  const [authUser, setAuthUser] = useState(initialAuthUser);

  const saveAuth = (authResponse) => {
    localStorage.setItem('token', authResponse.token);
    localStorage.setItem('username', authResponse.username);
    const nextAuthUser = {
      id: authResponse.userId,
      username: authResponse.username,
      roles: authResponse.roles || [],
      organization: authResponse.organization || null,
    };
    localStorage.setItem('authUser', JSON.stringify(nextAuthUser));
    setToken(authResponse.token);
    setUsername(authResponse.username);
    setAuthUser(nextAuthUser);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('authUser');
    setToken(null);
    setUsername(null);
    setAuthUser(null);
  };

  const isAuthenticated = !!token;
  const value = useMemo(() => ({
    token,
    username,
    authUser,
    roles: authUser?.roles || [],
    organization: authUser?.organization || null,
    userId: authUser?.id || null,
    isAuthenticated,
    saveAuth,
    logout,
  }), [token, username, authUser, isAuthenticated]);

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
