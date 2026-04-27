import { useState, useEffect, useRef, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useToast } from "../../context/ToastContext";
import { documentAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";
import EmptyState from "../../components/ui/EmptyState";
import StatusBadge from "../../components/ui/StatusBadge";
import ConfirmDialog from "../../components/ui/ConfirmDialog";

const DOC_TYPES = [
  "INSURANCE_POLICY",
  "MEDICAL_REPORT",
  "PRESCRIPTION",
  "LAB_RESULT",
  "BILLING_STATEMENT",
  "CLAIM_FORM",
  "OTHER",
];

const TYPE_COLORS = {
  INSURANCE_POLICY: "bg-violet-500/15 text-violet-400 border-violet-500/30",
  MEDICAL_REPORT: "bg-cyan-500/15 text-cyan-400 border-cyan-500/30",
  PRESCRIPTION: "bg-emerald-500/15 text-emerald-400 border-emerald-500/30",
  LAB_RESULT: "bg-amber-500/15 text-amber-400 border-amber-500/30",
  BILLING_STATEMENT: "bg-blue-500/15 text-blue-400 border-blue-500/30",
  CLAIM_FORM: "bg-pink-500/15 text-pink-400 border-pink-500/30",
  OTHER: "bg-slate-500/15 text-slate-400 border-slate-500/30",
};

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.07 } } };
const item = { hidden: { opacity: 0, y: 20 }, show: { opacity: 1, y: 0 } };

export default function PatientDocuments() {
  const { addToast } = useToast();
  const fileInputRef = useRef(null);

  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState({});
  const [documentType, setDocumentType] = useState("MEDICAL_REPORT");
  const [dragOver, setDragOver] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [pollInterval, setPollInterval] = useState(null);

  useEffect(() => {
    fetchDocuments();
    return () => { if (pollInterval) clearInterval(pollInterval); };
  }, []);

  async function fetchDocuments() {
    try {
      const data = await documentAPI.list();
      setDocuments(Array.isArray(data) ? data : []);

      // Start polling if any docs are processing
      const pending = (Array.isArray(data) ? data : []).filter(
        (d) => d.status === "UPLOADED" || d.status === "PROCESSING"
      );
      if (pending.length > 0 && !pollInterval) {
        const id = setInterval(async () => {
          try {
            const fresh = await documentAPI.list();
            setDocuments(Array.isArray(fresh) ? fresh : []);
            const stillPending = (Array.isArray(fresh) ? fresh : []).filter(
              (d) => d.status === "UPLOADED" || d.status === "PROCESSING"
            );
            if (stillPending.length === 0) {
              clearInterval(id);
              setPollInterval(null);
            }
          } catch { /* silent */ }
        }, 3000);
        setPollInterval(id);
      }
    } catch {
      addToast("Failed to load documents", "error");
    } finally {
      setLoading(false);
    }
  }

  async function handleUpload(files) {
    if (!files || files.length === 0) return;
    setUploading(true);

    // Track progress per file
    const progressMap = {};
    Array.from(files).forEach((f, i) => { progressMap[i] = 0; });
    setUploadProgress(progressMap);

    // Simulate progress
    const progressTimer = setInterval(() => {
      setUploadProgress((prev) => {
        const next = { ...prev };
        Object.keys(next).forEach((k) => {
          if (next[k] < 90) next[k] += Math.random() * 15;
        });
        return next;
      });
    }, 300);

    try {
      await documentAPI.upload(files, documentType);
      clearInterval(progressTimer);
      setUploadProgress({});
      addToast(`${files.length} document(s) uploaded successfully!`, "success");
      fetchDocuments();
    } catch (err) {
      addToast("Upload failed", "error");
      clearInterval(progressTimer);
      setUploadProgress({});
    } finally {
      setUploading(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    try {
      await documentAPI.delete(deleteTarget.id);
      setDocuments((prev) => prev.filter((d) => d.id !== deleteTarget.id));
      addToast("Document deleted", "info");
    } catch {
      addToast("Delete failed", "error");
    } finally {
      setDeleteTarget(null);
    }
  }

  function handleDrop(e) {
    e.preventDefault();
    setDragOver(false);
    handleUpload(e.dataTransfer.files);
  }

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
      <div className="max-w-4xl mx-auto">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-white" style={{ fontFamily: "Orbitron, sans-serif" }}>
            My Documents
          </h1>
          <p className="text-slate-400 text-sm mt-1">{documents.length} documents uploaded</p>
        </div>

        {/* Upload zone */}
        <div className="mb-8">
          {/* Type selector */}
          <div className="mb-3 flex items-center gap-3">
            <label className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-400" style={{ fontFamily: "Orbitron, sans-serif" }}>
              Type:
            </label>
            <select
              value={documentType}
              onChange={(e) => setDocumentType(e.target.value)}
              className="bg-white/5 border border-white/20 rounded-xl px-3 py-2 text-white text-sm focus:border-cyan-500 outline-none cursor-pointer"
            >
              {DOC_TYPES.map((t) => (
                <option key={t} value={t} className="bg-slate-900 text-white">
                  {t.replace(/_/g, " ")}
                </option>
              ))}
            </select>
          </div>

          {/* Drop zone */}
          <div
            onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
            onDragLeave={() => setDragOver(false)}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            className={`relative border-2 border-dashed rounded-2xl p-8 sm:p-12 text-center cursor-pointer transition-all ${
              dragOver
                ? "border-cyan-400 bg-cyan-500/5"
                : "border-white/20 hover:border-white/30 bg-white/[0.02]"
            }`}
          >
            {uploading ? (
              <div className="space-y-3">
                <LoadingSpinner size="md" />
                <p className="text-white text-sm">Uploading...</p>
                {Object.entries(uploadProgress).map(([key, pct]) => (
                  <div key={key} className="max-w-xs mx-auto">
                    <div className="w-full h-1.5 bg-white/10 rounded-full overflow-hidden">
                      <motion.div
                        className="h-full bg-gradient-to-r from-cyan-500 to-blue-600 rounded-full"
                        initial={{ width: 0 }}
                        animate={{ width: `${Math.min(pct, 100)}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <>
                <motion.div
                  animate={{ y: [0, -4, 0] }}
                  transition={{ duration: 2, repeat: Infinity }}
                  className="text-4xl mb-3"
                >
                  📤
                </motion.div>
                <p className="text-white font-medium text-sm">
                  Drop files here or click to browse
                </p>
                <p className="text-slate-400 text-xs mt-1">Supports multiple files</p>
              </>
            )}

            <input
              ref={fileInputRef}
              type="file"
              multiple
              onChange={(e) => handleUpload(e.target.files)}
              className="hidden"
            />
          </div>
        </div>

        {/* Documents list */}
        {documents.length === 0 ? (
          <EmptyState
            icon="📄"
            title="No documents yet"
            subtitle="Upload your insurance policy to unlock AI-powered coverage analysis"
            actionLabel="Upload Document"
            onAction={() => fileInputRef.current?.click()}
          />
        ) : (
          <motion.div variants={container} initial="hidden" animate="show" className="space-y-3">
            {documents.map((doc, i) => (
              <motion.div
                key={doc.id || i}
                variants={item}
                className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-xl p-4 flex items-center gap-4 hover:border-white/20 transition-colors"
              >
                <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center text-xl shrink-0">
                  📄
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-white text-sm font-medium truncate">{doc.filename || doc.name || "Document"}</p>
                  <div className="flex items-center gap-2 mt-1 flex-wrap">
                    <span className={`text-[10px] px-2 py-0.5 rounded-full border font-semibold ${TYPE_COLORS[doc.documentType] || TYPE_COLORS.OTHER}`}>
                      {(doc.documentType || "OTHER").replace(/_/g, " ")}
                    </span>
                    <StatusBadge status={doc.status || "UPLOADED"} />
                    {doc.size && <span className="text-slate-500 text-[10px]">{(doc.size / 1024).toFixed(1)} KB</span>}
                  </div>
                </div>
                <button
                  onClick={() => setDeleteTarget(doc)}
                  className="shrink-0 text-slate-400 hover:text-red-400 transition-colors p-1"
                  title="Delete"
                >
                  🗑️
                </button>
              </motion.div>
            ))}
          </motion.div>
        )}

        {/* Delete confirmation */}
        <ConfirmDialog
          open={!!deleteTarget}
          title="Delete Document"
          message={`Are you sure you want to delete "${deleteTarget?.filename || deleteTarget?.name || "this document"}"?`}
          confirmLabel="Delete"
          danger
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      </div>
    </div>
  );
}
