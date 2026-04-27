import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import ParticleBackground from "../components/ParticleBackground";
import LoadingSpinner from "../components/ui/LoadingSpinner";

const ROLES = [
  {
    value: "PATIENT",
    label: "I'm a Patient",
    desc: "Access your health records, billing & AI chat",
    icon: "🧑‍💻",
  },
  {
    value: "DOCTOR",
    label: "I'm a Doctor",
    desc: "Manage patients, records & billing",
    icon: "🩺",
  },
];

export default function Register() {
  const navigate = useNavigate();
  const { register } = useAuth();
  const { addToast } = useToast();

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("PATIENT");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleRegister = async (e) => {
    e.preventDefault();
    if (!username.trim() || !email.trim() || !password.trim()) {
      setError("Please fill in all fields.");
      return;
    }
    if (password.length < 4) {
      setError("Password must be at least 4 characters.");
      return;
    }
    setError("");
    setLoading(true);

    try {
      const userData = await register({ username, email, password, role });
      addToast("Registration successful! Welcome aboard.", "success");

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
        : "Registration failed. Please try again.";
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
          <div className="text-center mb-6">
            <h2
              className="text-2xl font-bold text-white mb-2"
              style={{ fontFamily: "Orbitron, sans-serif" }}
            >
              Create Account
            </h2>
            <p className="text-sm text-slate-400">Join MediSure AI today</p>
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

          <form onSubmit={handleRegister} className="flex flex-col gap-4">
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
              type="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="email"
              required
              className="w-full bg-white/5 border border-white/20 rounded-xl px-4 py-3 text-white placeholder-slate-500 focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 outline-none transition"
            />

            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="new-password"
              required
              className="w-full bg-white/5 border border-white/20 rounded-xl px-4 py-3 text-white placeholder-slate-500 focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 outline-none transition"
            />

            {/* Role Selection */}
            <div>
              <label
                className="block text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400 mb-3"
                style={{ fontFamily: "Orbitron, sans-serif" }}
              >
                I am a
              </label>
              <div className="grid grid-cols-2 gap-3">
                {ROLES.map((r) => (
                  <motion.button
                    key={r.value}
                    type="button"
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => setRole(r.value)}
                    className={`relative p-4 rounded-xl border text-left transition-all ${
                      role === r.value
                        ? "bg-cyan-500/10 border-cyan-500/50 shadow-lg shadow-cyan-500/10"
                        : "bg-white/5 border-white/10 hover:border-white/20"
                    }`}
                  >
                    {role === r.value && (
                      <div className="absolute top-2 right-2 w-5 h-5 rounded-full bg-cyan-500 flex items-center justify-center text-white text-xs">
                        ✓
                      </div>
                    )}
                    <span className="text-2xl block mb-1">{r.icon}</span>
                    <span className="text-sm font-semibold text-white block">{r.label}</span>
                    <span className="text-[11px] text-slate-400 leading-tight">{r.desc}</span>
                  </motion.button>
                ))}
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-semibold hover:scale-[1.02] transition-transform shadow-lg shadow-cyan-500/25 disabled:opacity-50 disabled:hover:scale-100 flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <LoadingSpinner size="sm" />
                  Creating Account...
                </>
              ) : (
                "Create Account"
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
              onClick={() => navigate("/login")}
            >
              Already have an account?{" "}
              <span className="text-cyan-400 font-medium">Login</span>
            </p>
          </div>
        </div>
      </motion.div>
    </div>
  );
}