import { createContext, useContext, useState, useEffect, ReactNode } from 'react';

interface AuthContextType {
  isAuthenticated: boolean;
  username: string | null;
  userId: number | null;
  login: (token: string, username: string, userId: number) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [username, setUsername] = useState<string | null>(null);
  const [userId, setUserId] = useState<number | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const storedUsername = localStorage.getItem('username');
    const storedUserId = localStorage.getItem('userId');
    if (token && storedUsername) {
      setIsAuthenticated(true);
      setUsername(storedUsername);
      setUserId(storedUserId ? Number(storedUserId) : null);
    }
  }, []);

  const login = (token: string, username: string, userId: number) => {
    localStorage.setItem('token', token);
    localStorage.setItem('username', username);
    localStorage.setItem('userId', String(userId));
    setIsAuthenticated(true);
    setUsername(username);
    setUserId(userId);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('userId');
    setIsAuthenticated(false);
    setUsername(null);
    setUserId(null);
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated, username, userId, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
