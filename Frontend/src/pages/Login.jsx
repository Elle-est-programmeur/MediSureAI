import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import ParticleBackground from "../components/ParticleBackground";
import LoadingSpinner from "../components/ui/LoadingSpinner";

export default function Login() {
  const navigate = useNavigate();
  const { login, getRoleBasedPath } = useAuth();
  const { addToast } = useToast();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError("Please fill in all fields.");
      return;
    }
    setError("");
    setLoading(true);

    try {
      const userData = await login({ username, password });
      addToast(`Welcome back, ${userData.username}!`, "success");

      // Role-based redirect
      switch (userData.role) {
        case "DOCTOR":
          navigate("/doctor/dashboard");
          break;
        case "PATIENT":
          navigate("/patient/dashboard");
          break;
        default:
          navigate("/dashboard");
      }
    } catch (err) {
      const errorMsg = err.response?.data
        ? typeof err.response.data === "string"
          ? err.response.data
          : err.response.data.message || JSON.stringify(err.response.data)
        : "Login failed. Please check your credentials.";
      setError(errorMsg);
      addToast(errorMsg, "error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex-1 flex items-center justify-center relative overflow-hidden">
      <ParticleBackground darkMode={true} />

      <motion.div
        initial={{ opacity: 0, y: 30, scale: 0.95 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
        className="relative z-10 w-full max-w-md mx-4"
      >
        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-8 shadow-2xl">
          {/* Header */}
          <div className="text-center mb-8">
            <h2
              className="text-2xl font-bold text-white mb-2"
              style={{ fontFamily: "Orbitron, sans-serif" }}
            >
              Welcome Back
            </h2>
            <p className="text-sm text-slate-400">Sign in to your MediSure AI account</p>
          </div>

          {/* Error */}
          {error && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-sm"
            >
              {error}
            </motion.div>
          )}

          <form onSubmit={handleLogin} className="flex flex-col gap-4">
            <input
              type="text"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              required
              className="w-full bg-white/5 border border-white/20 rounded-xl px-4 py-3 text-white placeholder-slate-500 focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 outline-none transition"
            />

            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
              className="w-full bg-white/5 border border-white/20 rounded-xl px-4 py-3 text-white placeholder-slate-500 focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 outline-none transition"
            />

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-semibold hover:scale-[1.02] transition-transform shadow-lg shadow-cyan-500/25 disabled:opacity-50 disabled:hover:scale-100 flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <LoadingSpinner size="sm" />
                  Signing In...
                </>
              ) : (
                "Sign In"
              )}
            </button>

            <button
              type="button"
              onClick={() => navigate("/")}
              disabled={loading}
              className="w-full py-3 rounded-xl bg-white/10 border border-white/20 text-white font-medium hover:bg-white/20 transition disabled:opacity-50"
            >
              Cancel
            </button>
          </form>

          <div className="mt-6 text-center">
            <p
              className="text-sm text-slate-400 hover:text-cyan-400 cursor-pointer transition-colors"
              onClick={() => navigate("/register")}
            >
              Don't have an account?{" "}
              <span className="text-cyan-400 font-medium">Register</span>
            </p>
          </div>
        </div>
      </motion.div>
    </div>
  );
}