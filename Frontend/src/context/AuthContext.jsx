import { createContext, useContext, useState, useEffect, useCallback } from "react";
import { login as apiLogin, register as apiRegister } from "../services/api";

const AuthContext = createContext(null);

// Decode JWT payload without library
function decodeJwtPayload(token) {
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

function isTokenExpired(token) {
  const payload = decodeJwtPayload(token);
  if (!payload || !payload.exp) return true;
  // exp is in seconds, Date.now() is in ms
  return Date.now() >= payload.exp * 1000;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // Restore session from localStorage on mount
  useEffect(() => {
    try {
      const storedUser = localStorage.getItem("user");
      const storedToken = localStorage.getItem("token");
      const storedRefreshToken = localStorage.getItem("refreshToken");

      if (storedUser && storedToken && !isTokenExpired(storedToken)) {
        const parsed = JSON.parse(storedUser);
        setUser({
          ...parsed,
          accessToken: storedToken,
          refreshToken: storedRefreshToken || null,
        });
        setIsAuthenticated(true);
      } else if (storedToken && isTokenExpired(storedToken)) {
        // Token expired — clear everything
        localStorage.removeItem("token");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("user");
      }
    } catch (err) {
      console.error("Auth restore failed:", err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const login = useCallback(async (credentials) => {
    const authData = await apiLogin(credentials);
    const userData = {
      username: authData.username,
      email: authData.email || "",
      role: authData.role,
      accessToken: authData.accessToken,
      refreshToken: authData.refreshToken || null,
    };

    localStorage.setItem("token", authData.accessToken);
    if (authData.refreshToken) {
      localStorage.setItem("refreshToken", authData.refreshToken);
    }
    localStorage.setItem(
      "user",
      JSON.stringify({
        username: authData.username,
        email: authData.email || "",
        role: authData.role,
      })
    );

    setUser(userData);
    setIsAuthenticated(true);

    // Notify other tabs / Navbar
    window.dispatchEvent(new Event("storage"));

    return userData;
  }, []);

  const registerUser = useCallback(async (data) => {
    await apiRegister(data);
    // Auto-login after register
    const userData = await login({ username: data.username, password: data.password });
    return userData;
  }, [login]);

  const logout = useCallback(() => {
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
    setUser(null);
    setIsAuthenticated(false);
    window.dispatchEvent(new Event("storage"));
  }, []);

  const isDoctor = useCallback(() => {
    return user?.role === "DOCTOR";
  }, [user]);

  const isPatient = useCallback(() => {
    return user?.role === "PATIENT";
  }, [user]);

  const isAdmin = useCallback(() => {
    return user?.role === "ADMIN";
  }, [user]);

  const getRoleBasedPath = useCallback(() => {
    if (!user) return "/login";
    switch (user.role) {
      case "DOCTOR":
        return "/doctor/dashboard";
      case "PATIENT":
        return "/patient/dashboard";
      case "ADMIN":
        return "/dashboard";
      default:
        return "/dashboard";
    }
  }, [user]);

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated,
        isLoading,
        login,
        register: registerUser,
        logout,
        isDoctor,
        isPatient,
        isAdmin,
        getRoleBasedPath,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}

export default AuthContext;
