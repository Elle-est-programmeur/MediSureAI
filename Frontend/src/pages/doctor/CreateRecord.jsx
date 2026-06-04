import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { useToast } from "../../context/ToastContext";
import { doctorAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";

export default function CreateRecord() {
  const navigate = useNavigate();
  const { addToast } = useToast();

  const [patients, setPatients] = useState([]);
  const [patientsLoading, setPatientsLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [patientSearch, setPatientSearch] = useState("");
  const [showDropdown, setShowDropdown] = useState(false);
  const [mrnInput, setMrnInput] = useState("");
  const [mrnLooking, setMrnLooking] = useState(false);
  const [history, setHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  const [form, setForm] = useState({
    patientUserId: "",
    patientName: "",
    patientMRN: "",
    diagnosis: "",
    treatmentPlan: "",
    drugs: [{ name: "", dosage: "", purpose: "" }],
  });

  useEffect(() => {
    loadPatients();
  }, []);

  async function loadPatients() {
    try {
      const data = await doctorAPI.getPatients();
      setPatients(Array.isArray(data) ? data : []);
    } catch {
      addToast("Failed to load patients", "error");
    } finally {
      setPatientsLoading(false);
    }
  }

  function selectPatient(patient) {
    const userId = patient.userId || patient.id;
    setForm((prev) => ({
      ...prev,
      patientUserId: userId,
      patientName: patient.name || patient.username || "",
      patientMRN: patient.medicalRecordNumber || "",
    }));
    setPatientSearch(patient.name || patient.username || "");
    setShowDropdown(false);
    loadHistory(userId);
  }

  async function loadHistory(userId) {
    if (!userId) { setHistory([]); return; }
    setHistoryLoading(true);
    try {
      const data = await doctorAPI.getPatientHistory(userId);
      setHistory(Array.isArray(data) ? data : []);
    } catch (err) {
      addToast(err.response?.data?.message || "Failed to load patient history", "error");
      setHistory([]);
    } finally {
      setHistoryLoading(false);
    }
  }

  async function lookupByMRN() {
    const mrn = mrnInput.trim();
    if (!mrn) {
      addToast("Enter an MRN to look up", "warning");
      return;
    }
    setMrnLooking(true);
    try {
      const patient = await doctorAPI.lookupByMRN(mrn);
      selectPatient(patient);
      addToast(`Found: ${patient.name || patient.username}`, "success");
    } catch (err) {
      addToast(err.response?.data?.message || "No patient found with that MRN", "error");
    } finally {
      setMrnLooking(false);
    }
  }

  function updateDrug(index, field, value) {
    setForm((prev) => {
      const drugs = [...prev.drugs];
      drugs[index] = { ...drugs[index], [field]: value };
      return { ...prev, drugs };
    });
  }

  function addDrug() {
    setForm((prev) => ({
      ...prev,
      drugs: [...prev.drugs, { name: "", dosage: "", purpose: "" }],
    }));
  }

  function removeDrug(index) {
    setForm((prev) => ({
      ...prev,
      drugs: prev.drugs.filter((_, i) => i !== index),
    }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!form.patientUserId) { addToast("Please select a patient", "warning"); return; }
    if (!form.diagnosis.trim()) { addToast("Diagnosis is required", "warning"); return; }

    setSubmitting(true);
    try {
      await doctorAPI.createRecord({
        patientUserId: form.patientUserId,
        diagnosis: form.diagnosis,
        treatmentPlan: form.treatmentPlan,
        drugs: form.drugs.filter((d) => d.name.trim()),
      });
      addToast("Medical record created successfully!", "success");
      navigate("/doctor/dashboard");
    } catch (err) {
      addToast(err.response?.data?.message || "Failed to create record", "error");
    } finally {
      setSubmitting(false);
    }
  }

  const filteredPatients = patients.filter((p) =>
    (p.name || p.username || "").toLowerCase().includes(patientSearch.toLowerCase())
  );

  const inputClass = "w-full bg-white/5 border border-white/20 rounded-xl px-4 py-3 text-white placeholder-slate-500 focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 outline-none transition";

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-white" style={{ fontFamily: "Orbitron, sans-serif" }}>
            Create Medical Record
          </h1>
          <p className="text-slate-400 text-sm mt-1">Fill in the details for a new medical record</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* ── Left: Form ── */}
          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Patient — by MRN (primary) or search existing */}
            <div className="space-y-3">
              <label className="block text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
                Patient (by MRN)
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={mrnInput}
                  onChange={(e) => setMrnInput(e.target.value.toUpperCase())}
                  onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); lookupByMRN(); } }}
                  placeholder="MRN-XXXXXXXX"
                  className={inputClass + " font-mono tracking-wider"}
                />
                <button
                  type="button"
                  onClick={lookupByMRN}
                  disabled={mrnLooking}
                  className="shrink-0 px-4 rounded-xl bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 text-sm font-medium hover:bg-cyan-500/20 transition-colors disabled:opacity-50"
                >
                  {mrnLooking ? <LoadingSpinner size="sm" /> : "Look up"}
                </button>
              </div>

              {form.patientUserId && (
                <div className="p-3 rounded-xl bg-cyan-500/5 border border-cyan-500/20 flex items-center gap-3">
                  <div className="w-8 h-8 rounded-full bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center text-xs text-white font-bold">
                    {(form.patientName || "?")[0].toUpperCase()}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm text-white font-medium truncate">{form.patientName}</div>
                    <div className="text-[10px] text-cyan-400 font-mono tracking-wider">{form.patientMRN}</div>
                  </div>
                  <button
                    type="button"
                    onClick={() => { setForm((p) => ({ ...p, patientUserId: "", patientName: "", patientMRN: "" })); setMrnInput(""); setPatientSearch(""); setHistory([]); }}
                    className="text-slate-400 hover:text-white text-lg"
                  >×</button>
                </div>
              )}

              {/* Existing-patients search (fallback for repeat visits) */}
              {!patientsLoading && patients.length > 0 && (
                <details className="group">
                  <summary className="text-xs text-slate-400 cursor-pointer hover:text-cyan-400 transition-colors select-none">
                    Or search existing patients ({patients.length})
                  </summary>
                  <div className="relative mt-2">
                    <input
                      type="text"
                      value={patientSearch}
                      onChange={(e) => { setPatientSearch(e.target.value); setShowDropdown(true); }}
                      onFocus={() => setShowDropdown(true)}
                      placeholder="Search by name..."
                      className={inputClass}
                    />
                    <AnimatePresence>
                      {showDropdown && filteredPatients.length > 0 && (
                        <motion.div
                          initial={{ opacity: 0, y: -5 }}
                          animate={{ opacity: 1, y: 0 }}
                          exit={{ opacity: 0, y: -5 }}
                          className="absolute z-20 top-full left-0 right-0 mt-1 bg-slate-800 border border-white/10 rounded-xl shadow-2xl max-h-48 overflow-y-auto"
                        >
                          {filteredPatients.map((p, i) => (
                            <div
                              key={p.id || i}
                              onClick={() => selectPatient(p)}
                              className="px-4 py-3 hover:bg-white/5 cursor-pointer text-sm text-white flex items-center gap-2 border-b border-white/5 last:border-0"
                            >
                              <div className="w-6 h-6 rounded-full bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center text-[10px] text-white font-bold">
                                {(p.name || p.username || "?")[0].toUpperCase()}
                              </div>
                              <div className="flex-1 min-w-0">
                                <div className="truncate">{p.name || p.username}</div>
                                {p.medicalRecordNumber && (
                                  <div className="text-[10px] text-cyan-400 font-mono">{p.medicalRecordNumber}</div>
                                )}
                              </div>
                              {p.age && <span className="text-slate-400 text-xs ml-2">{p.age} yrs</span>}
                            </div>
                          ))}
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                </details>
              )}
            </div>

            {/* Diagnosis */}
            <div>
              <label className="block text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400 mb-2" style={{ fontFamily: "Orbitron, sans-serif" }}>
                Diagnosis
              </label>
              <textarea
                value={form.diagnosis}
                onChange={(e) => setForm((prev) => ({ ...prev, diagnosis: e.target.value }))}
                placeholder="Enter diagnosis..."
                rows={3}
                className={inputClass + " resize-none"}
                required
              />
              <span className="text-xs text-slate-500 mt-1 block">{form.diagnosis.length} characters</span>
            </div>

            {/* Treatment Plan */}
            <div>
              <label className="block text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400 mb-2" style={{ fontFamily: "Orbitron, sans-serif" }}>
                Treatment Plan
              </label>
              <textarea
                value={form.treatmentPlan}
                onChange={(e) => setForm((prev) => ({ ...prev, treatmentPlan: e.target.value }))}
                placeholder="Describe treatment plan..."
                rows={4}
                className={inputClass + " resize-none"}
              />
            </div>

            {/* Drugs */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
                  Prescribed Drugs
                </label>
                <button type="button" onClick={addDrug} className="text-xs px-3 py-1 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 hover:bg-cyan-500/20 transition-colors">
                  + Add Drug
                </button>
              </div>

              <AnimatePresence>
                {form.drugs.map((drug, index) => (
                  <motion.div
                    key={index}
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: "auto" }}
                    exit={{ opacity: 0, height: 0 }}
                    layout
                    className="mb-3"
                  >
                    <div className="grid grid-cols-3 gap-2 bg-white/5 border border-white/10 rounded-xl p-3">
                      <input
                        value={drug.name}
                        onChange={(e) => updateDrug(index, "name", e.target.value)}
                        placeholder="Drug name"
                        className="bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-white text-sm placeholder-slate-500 focus:border-cyan-500 outline-none transition"
                      />
                      <input
                        value={drug.dosage}
                        onChange={(e) => updateDrug(index, "dosage", e.target.value)}
                        placeholder="Dosage"
                        className="bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-white text-sm placeholder-slate-500 focus:border-cyan-500 outline-none transition"
                      />
                      <div className="flex gap-2">
                        <input
                          value={drug.purpose}
                          onChange={(e) => updateDrug(index, "purpose", e.target.value)}
                          placeholder="Purpose"
                          className="flex-1 bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-white text-sm placeholder-slate-500 focus:border-cyan-500 outline-none transition"
                        />
                        {form.drugs.length > 1 && (
                          <button
                            type="button"
                            onClick={() => removeDrug(index)}
                            className="text-red-400 hover:text-red-300 text-lg px-1 transition-colors"
                          >
                            ×
                          </button>
                        )}
                      </div>
                    </div>
                  </motion.div>
                ))}
              </AnimatePresence>
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={submitting}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-semibold hover:scale-[1.02] transition-transform shadow-lg shadow-cyan-500/25 disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {submitting ? <><LoadingSpinner size="sm" /> Creating...</> : "Create Medical Record"}
            </button>
          </form>

          {/* ── Right: History + Live Preview ── */}
          <div className="hidden lg:block">
            <div className="sticky top-8 space-y-6">
              {/* Patient History */}
              {form.patientUserId && (
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
                      Patient History
                    </h3>
                    <span className="text-[10px] text-slate-500">
                      {historyLoading ? "Loading…" : `${history.length} prior record${history.length === 1 ? "" : "s"}`}
                    </span>
                  </div>
                  <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-4 max-h-72 overflow-y-auto">
                    {historyLoading ? (
                      <div className="flex justify-center py-4"><LoadingSpinner size="sm" /></div>
                    ) : history.length === 0 ? (
                      <p className="text-slate-400 text-xs text-center py-4">
                        No prior medical records on file for this patient.
                      </p>
                    ) : (
                      <ul className="space-y-3">
                        {history.map((r) => (
                          <li key={r.id} className="border-l-2 border-cyan-500/40 pl-3">
                            <div className="flex items-center justify-between gap-2">
                              <span className="text-white text-sm font-medium truncate">
                                {r.diagnosis || "Untitled"}
                              </span>
                              <span className="text-[10px] text-slate-500 shrink-0">
                                {r.createdAt ? String(r.createdAt).slice(0, 10) : ""}
                              </span>
                            </div>
                            <div className="text-[10px] text-slate-500 mt-0.5">
                              {r.doctorName ? `Dr. ${r.doctorName}` : "—"}
                              {r.doctorSpecialization ? ` • ${r.doctorSpecialization}` : ""}
                            </div>
                            {r.treatmentPlan && (
                              <p className="text-slate-300 text-xs mt-1 line-clamp-2">{r.treatmentPlan}</p>
                            )}
                            {Array.isArray(r.drugs) && r.drugs.length > 0 && (
                              <div className="mt-1.5 flex flex-wrap gap-1">
                                {r.drugs.slice(0, 5).map((d, i) => (
                                  <span key={i} className="text-[10px] px-1.5 py-0.5 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-300">
                                    💊 {d.name}{d.dosage ? ` ${d.dosage}` : ""}
                                  </span>
                                ))}
                                {r.drugs.length > 5 && (
                                  <span className="text-[10px] text-slate-500">+{r.drugs.length - 5} more</span>
                                )}
                              </div>
                            )}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </div>
              )}

              <h3 className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400 mb-3" style={{ fontFamily: "Orbitron, sans-serif" }}>
                Live Preview
              </h3>
              <motion.div
                layout
                className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6 shadow-lg shadow-cyan-500/5"
              >
                {/* Patient */}
                <div className="mb-4 pb-4 border-b border-white/10">
                  <span className="text-[10px] uppercase tracking-wider text-slate-500">Patient</span>
                  <p className="text-white font-medium">{form.patientName || "Select a patient..."}</p>
                </div>

                {/* Diagnosis */}
                <div className="mb-4 pb-4 border-b border-white/10">
                  <span className="text-[10px] uppercase tracking-wider text-slate-500">Diagnosis</span>
                  <p className="text-white text-lg font-bold mt-1" style={{ fontFamily: "Orbitron, sans-serif" }}>
                    {form.diagnosis || "Enter diagnosis..."}
                  </p>
                </div>

                {/* Treatment */}
                {form.treatmentPlan && (
                  <div className="mb-4 pb-4 border-b border-white/10">
                    <span className="text-[10px] uppercase tracking-wider text-slate-500">Treatment Plan</span>
                    <p className="text-slate-300 text-sm mt-1 whitespace-pre-wrap">{form.treatmentPlan}</p>
                  </div>
                )}

                {/* Drugs */}
                <div>
                  <span className="text-[10px] uppercase tracking-wider text-slate-500">Prescribed Drugs</span>
                  <div className="mt-2 flex flex-wrap gap-2">
                    {form.drugs.filter((d) => d.name.trim()).length === 0 ? (
                      <span className="text-slate-500 text-sm">No drugs added yet</span>
                    ) : (
                      form.drugs.filter((d) => d.name.trim()).map((d, i) => (
                        <span
                          key={i}
                          className="inline-flex items-center gap-1 px-3 py-1.5 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 text-xs font-medium"
                        >
                          💊 {d.name}
                          {d.dosage && <span className="text-slate-400">• {d.dosage}</span>}
                        </span>
                      ))
                    )}
                  </div>
                </div>
              </motion.div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
