import React, { createContext, useContext, useMemo, useState } from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [credentials, setCredentials] = useState(null);

  const value = useMemo(
    () => ({
      credentials,
      isAuthenticated: Boolean(credentials),
      login: (username, password) => setCredentials({ username, password }),
      logout: () => setCredentials(null),
    }),
    [credentials]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
