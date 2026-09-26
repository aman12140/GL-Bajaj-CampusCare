import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import api, { tokenStore } from '../api/client';

const AuthContext = createContext(null);

export const HOME_BY_ROLE = { STUDENT: '/student', STAFF: '/staff', ADMIN: '/admin' };

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(!!tokenStore.get());

  // On page load: if a token exists, ask the server who we are (also detects deactivated accounts).
  useEffect(() => {
    if (!tokenStore.get()) return;
    api.get('/api/auth/me')
      .then((res) => setUser(res.data))
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (email, password) => {
    const res = await api.post('/api/auth/login', { email, password });
    tokenStore.set(res.data.token);
    setUser(res.data.user);
    return res.data.user;
  }, []);

  const logout = useCallback(() => {
    tokenStore.clear();
    setUser(null);
  }, []);

  const refresh = useCallback(async () => {
    const res = await api.get('/api/auth/me');
    setUser(res.data);
    return res.data;
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, refresh, setUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
