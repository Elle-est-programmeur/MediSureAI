import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { patientAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";

export default function PatientProfile() {
  const { user } = useAuth();
  const { addToast } = useToast();

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ name: "", age: "", gender: "" });

  useEffect(() => {
    fetchProfile();
  }, []);

  async function fetchProfile() {
    try {
      const data = await patientAPI.getProfile();
      setProfile(data);
      setForm({
        name: data.name || data.username || "",
        age: data.age || "",
        gender: data.gender || "",
      });
    } catch {
      // Profile might not exist yet — use auth context
      setForm({
        name: user?.username || "",
        age: "",
        gender: "",
      });
    } finally {
      setLoading(false);
    }
  }

  async function handleSave() {
    if (!form.name.trim()) {
      addToast("Name is required", "warning");
      return;
    }
    if (form.age && (isNaN(form.age) || parseInt(form.age) <= 0)) {
      addToast("Enter a valid age", "warning");
      return;
    }
    setSaving(true);
    try {
      const data = await patientAPI.updateProfile({
        name: form.name,
        age: form.age ? parseInt(form.age) : null,
        gender: form.gender || null,
      });
      setProfile(data);
      setEditing(false);
      addToast("Profile updated successfully!", "success");
    } catch {
      addToast("Failed to update profile", "error");
    } finally {
      setSaving(false);
    }
  }

  function getInitials(name) {
    if (!name) return "?";
    return name.split(" ").map((w) => w[0]).join("").toUpperCase().slice(0, 2);
  }

  const inputClass = "w-full bg-white/5 border border-white/20 rounded-xl px-4 py-3 text-white placeholder-slate-500 focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 outline-none transition";
  const readOnlyClass = "w-full bg-white/[0.02] border border-white/10 rounded-xl px-4 py-3 text-slate-300 cursor-default";

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
      <div className="max-w-3xl mx-auto">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-white" style={{ fontFamily: "Orbitron, sans-serif" }}>
            My Profile
          </h1>
          <p className="text-slate-400 text-sm mt-1">Manage your personal information</p>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl overflow-hidden"
        >
          <div className="grid grid-cols-1 sm:grid-cols-[280px_1fr]">
            {/* ── Left: Avatar card ── */}
            <div className="p-8 flex flex-col items-center text-center border-b sm:border-b-0 sm:border-r border-white/10 bg-gradient-to-b from-cyan-500/5 to-transparent">
              {/* Large initials avatar */}
              <div className="w-24 h-24 rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center text-white text-3xl font-bold shadow-lg shadow-cyan-500/20 mb-4" style={{ fontFamily: "Orbitron, sans-serif" }}>
                {getInitials(form.name || user?.username)}
              </div>

              <h2 className="text-lg font-bold text-white" style={{ fontFamily: "Space Grotesk, sans-serif" }}>
                {form.name || user?.username || "User"}
              </h2>

              {/* Role badge */}
              <span className="mt-2 text-[10px] px-3 py-1 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 font-semibold uppercase tracking-wider">
                {user?.role || "PATIENT"}
              </span>

              {/* Medical Record Number */}
              {profile?.medicalRecordNumber && (
                <div className="mt-3 w-full px-3 py-2 rounded-lg bg-slate-800/60 border border-cyan-500/20">
                  <div className="text-[9px] uppercase tracking-[0.2em] text-slate-400 mb-0.5">Medical Record No.</div>
                  <div className="text-sm font-mono tracking-wider text-cyan-400 font-semibold">
                    {profile.medicalRecordNumber}
                  </div>
                </div>
              )}

              {/* Email */}
              <p className="mt-3 text-sm text-slate-400">
                {user?.email || profile?.email || "—"}
              </p>

              {/* Member since */}
              <p className="mt-1 text-xs text-slate-500">
                Member since {profile?.createdAt || new Date().getFullYear()}
              </p>
            </div>

            {/* ── Right: Editable form ── */}
            <div className="p-8">
              <div className="flex items-center justify-between mb-6">
                <h3 className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
                  Personal Information
                </h3>
                {!editing ? (
                  <button
                    onClick={() => setEditing(true)}
                    className="text-sm px-4 py-1.5 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 hover:bg-cyan-500/20 transition-colors"
                  >
                    Edit
                  </button>
                ) : (
                  <div className="flex gap-2">
                    <button
                      onClick={() => {
                        setEditing(false);
                        setForm({
                          name: profile?.name || user?.username || "",
                          age: profile?.age || "",
                          gender: profile?.gender || "",
                        });
                      }}
                      className="text-sm px-3 py-1.5 rounded-lg bg-white/10 border border-white/20 text-white hover:bg-white/20 transition"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={handleSave}
                      disabled={saving}
                      className="text-sm px-4 py-1.5 rounded-lg bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-semibold hover:scale-105 transition-transform shadow-lg shadow-cyan-500/25 disabled:opacity-50 flex items-center gap-2"
                    >
                      {saving ? <><LoadingSpinner size="sm" /> Saving...</> : "Save"}
                    </button>
                  </div>
                )}
              </div>

              <div className="space-y-5">
                {/* Name */}
                <div>
                  <label className="block text-xs text-slate-400 mb-1.5 uppercase tracking-wide">Full Name</label>
                  {editing ? (
                    <motion.input
                      initial={{ borderColor: "rgba(255,255,255,0.1)" }}
                      animate={{ borderColor: "rgba(6,182,212,0.5)" }}
                      type="text"
                      value={form.name}
                      onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
                      className={inputClass}
                      placeholder="Enter your name"
                    />
                  ) : (
                    <div className={readOnlyClass}>{form.name || "—"}</div>
                  )}
                </div>

                {/* Age */}
                <div>
                  <label className="block text-xs text-slate-400 mb-1.5 uppercase tracking-wide">Age</label>
                  {editing ? (
                    <motion.input
                      initial={{ borderColor: "rgba(255,255,255,0.1)" }}
                      animate={{ borderColor: "rgba(6,182,212,0.5)" }}
                      type="number"
                      min="1"
                      max="200"
                      value={form.age}
                      onChange={(e) => setForm((p) => ({ ...p, age: e.target.value }))}
                      className={inputClass}
                      placeholder="Enter your age"
                    />
                  ) : (
                    <div className={readOnlyClass}>{form.age || "—"}</div>
                  )}
                </div>

                {/* Gender */}
                <div>
                  <label className="block text-xs text-slate-400 mb-1.5 uppercase tracking-wide">Gender</label>
                  {editing ? (
                    <motion.select
                      initial={{ borderColor: "rgba(255,255,255,0.1)" }}
                      animate={{ borderColor: "rgba(6,182,212,0.5)" }}
                      value={form.gender}
                      onChange={(e) => setForm((p) => ({ ...p, gender: e.target.value }))}
                      className={inputClass + " cursor-pointer"}
                    >
                      <option value="" className="bg-slate-900">Select gender</option>
                      <option value="MALE" className="bg-slate-900">Male</option>
                      <option value="FEMALE" className="bg-slate-900">Female</option>
                      <option value="OTHER" className="bg-slate-900">Other</option>
                    </motion.select>
                  ) : (
                    <div className={readOnlyClass}>{form.gender || "—"}</div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
