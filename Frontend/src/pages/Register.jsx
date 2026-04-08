import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTheme } from "../context/ThemeContext";
import ParticleBackground from "../components/ParticleBackground";
import { register } from "../services/api";
import "./Auth.css";

export default function Register() {
  const navigate = useNavigate();
  const { darkMode } = useTheme();
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleRegister = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      await register({ username, email, password });
      alert("Registration successful! Please login.");
      navigate("/login");
    } catch (err) {
      // Handle error response - check if it's an object or string
      const errorMsg = err.response?.data 
        ? (typeof err.response.data === 'string' 
            ? err.response.data 
            : err.response.data.message || JSON.stringify(err.response.data))
        : "Registration failed. Please try again.";
      setError(errorMsg);
      console.error("Registration error:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <ParticleBackground darkMode={darkMode} />

      <div className="auth-card">
        <h2 className="auth-title">Create Account</h2>

        {error && <p style={{ color: 'red', marginBottom: '10px' }}>{error}</p>}

        <input
          className="auth-input"
          type="text"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
        />

        <input
          className="auth-input"
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          autoComplete="email"
        />

        <input
          className="auth-input"
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="new-password"
        />

        <div className="auth-buttons">
          <button
            className="auth-button"
            onClick={handleRegister}
            disabled={loading}
          >
            {loading ? "Registering..." : "Register"}
          </button>

          <button
            className="auth-button cancel"
            onClick={() => navigate("/")}
            disabled={loading}
          >
            Cancel
          </button>
        </div>

        <p
          className="auth-link"
          onClick={() => navigate("/login")}
        >
          Already have an account? Login
        </p>
      </div>
    </div>
  );
}