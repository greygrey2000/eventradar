import React, { createContext, useState, useEffect, ReactNode } from "react";
import api from "../api/apiClient";
import { postWithCsrf } from "../api/csrf";

// UserProfile type (an dein Backend anpassen)
export interface UserProfile {
  name: string;
  email: string;
  location: string;
  interests: string[];
}

interface AuthContextType {
  user: UserProfile | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get("/users/me")
      .then((res: { data: UserProfile }) => setUser(res.data))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  const login = async (email: string, password: string) => {
    await api.post("/auth/login", { email, password });
    const res = await api.get("/users/me");
    setUser(res.data);
  };

  const logout = async () => {
    await postWithCsrf("/auth/logout");
    setUser(null);
    // CSRF-Cookie im Browser löschen (sofern möglich)
    document.cookie = "csrfToken=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
    // Optional: Seite neu laden, um alle States zu resetten
    // window.location.reload();
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
