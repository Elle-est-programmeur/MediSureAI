import { useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { useTheme } from "../context/ThemeContext";

export default function Navbar() {
  const navigate = useNavigate();
  const { darkMode, setDarkMode } = useTheme();

  const [isLoggedIn, setIsLoggedIn] = useState(false);

  // Sync login state from localStorage
  useEffect(() => {
    const checkAuth = () => {
      const user = localStorage.getItem("user");
      setIsLoggedIn(!!user);
    };

    checkAuth();

    // Listen for login/logout changes
    window.addEventListener("storage", checkAuth);

    return () => {
      window.removeEventListener("storage", checkAuth);
    };
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("user");
    localStorage.removeItem("token");
    setIsLoggedIn(false);
    navigate("/");
  };

  return (
    <nav
      className={`shrink-0 w-full h-16 flex justify-between items-center px-10 z-50 transition-all duration-300 ${
        darkMode
          ? "bg-slate-900 text-white border-b border-white/10"
          : "bg-white text-gray-900 border-b border-black/10"
      }`}
    >
      <div
        className="flex items-center gap-2 text-lg font-semibold cursor-pointer"
        onClick={() => navigate("/")}
      >
        🏥 <span>MediSure AI</span>
      </div>

      <div className="flex items-center gap-4">
        <button
          onClick={() => setDarkMode(!darkMode)}
          className="px-3 py-1 border rounded-md transition"
        >
          {darkMode ? "🌙" : "☀"}
        </button>

        {!isLoggedIn ? (
          <button
            onClick={() => navigate("/login")}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:opacity-90 transition"
          >
            Login
          </button>
        ) : (
          <button
            onClick={handleLogout}
            className="px-4 py-2 bg-red-500 text-white rounded-md hover:opacity-90 transition"
          >
            Logout
          </button>
        )}
      </div>
    </nav>
  );
}