import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTheme } from "../context/ThemeContext";
import ParticleBackground from "../components/ParticleBackground";
import { login } from "../services/api";
import "./Auth.css";

export default function Login() {
  const navigate = useNavigate();
  const { darkMode } = useTheme();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const authData = await login({ username, password });
      
      // Store token and user info correctly
      localStorage.setItem("token", authData.accessToken);
      localStorage.setItem("user", JSON.stringify({ username: authData.username, role: authData.role }));

      // Notify navbar
      window.dispatchEvent(new Event("storage"));

      navigate("/dashboard");
    } catch (err) {
      // Handle error response - check if it's an object or string
      const errorMsg = err.response?.data 
        ? (typeof err.response.data === 'string' 
            ? err.response.data 
            : err.response.data.message || JSON.stringify(err.response.data))
        : "Login failed. Please check your credentials.";
      setError(errorMsg);
      console.error("Login error:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <ParticleBackground darkMode={darkMode} />

      <div className="auth-card">
        <h2 className="auth-title">Welcome Back</h2>

        {error && <p style={{ color: 'red', marginBottom: '10px' }}>{error}</p>}

        <form onSubmit={handleLogin}>
          <input
            className="auth-input"
            type="text"
            placeholder="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
          />

          <input
            className="auth-input"
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />

          <div className="auth-buttons">
            <button
              className="auth-button"
              type="submit"
              disabled={loading}
            >
              {loading ? "Logging in..." : "Login"}
            </button>

            <button
              className="auth-button cancel"
              type="button"
              onClick={() => navigate("/")}
              disabled={loading}
            >
              Cancel
            </button>
          </div>
        </form>

        <p
          className="auth-link"
          onClick={() => alert("Forgot Password coming soon 🚀")}
        >
          Forgot Password?
        </p>

        <p
          className="auth-link"
          onClick={() => navigate("/register")}
        >
          Don’t have an account? Register
        </p>
      </div>
    </div>
  );
}