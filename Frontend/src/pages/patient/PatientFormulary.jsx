import { useState } from "react";
import { motion } from "framer-motion";
import { useToast } from "../../context/ToastContext";
import { patientAPI } from "../../services/api";
import LoadingSpinner from "../../components/ui/LoadingSpinner";

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.07 } } };
const item = { hidden: { opacity: 0, y: 20 }, show: { opacity: 1, y: 0 } };

export default function PatientFormulary() {
  const { addToast } = useToast();
  const [query, setQuery] = useState("");
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  async function handleSearch(e) {
    e.preventDefault();
    if (!query.trim()) return;
    setLoading(true);
    setSearched(true);
    try {
      const data = await patientAPI.formularySearch(query);
      setResults(data);
    } catch {
      addToast("Search failed", "error");
      setResults(null);
    } finally {
      setLoading(false);
    }
  }

  // Normalize results — could be array or object with nested data
  const drugList = Array.isArray(results)
    ? results
    : results?.drugs || results?.results || results?.data
    ? Array.isArray(results?.drugs || results?.results || results?.data)
      ? results?.drugs || results?.results || results?.data
      : [results]
    : results
    ? [results]
    : [];

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
      <div className="max-w-3xl mx-auto">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-white" style={{ fontFamily: "Orbitron, sans-serif" }}>
            Drug Formulary Search
          </h1>
          <p className="text-slate-400 text-sm mt-1">Search for drug information, coverage, and alternatives</p>
        </div>

        {/* Search bar */}
        <form onSubmit={handleSearch} className="mb-8">
          <div className="flex gap-2">
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search for a drug (e.g., Metformin, Amoxicillin...)"
              className="flex-1 bg-white/5 border border-white/20 rounded-xl px-4 py-3 text-white placeholder-slate-500 focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 outline-none transition"
            />
            <button
              type="submit"
              disabled={loading || !query.trim()}
              className="px-6 py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-semibold hover:scale-105 transition-transform shadow-lg shadow-cyan-500/25 disabled:opacity-50 disabled:hover:scale-100"
            >
              {loading ? <LoadingSpinner size="sm" /> : "Search"}
            </button>
          </div>
        </form>

        {/* Results */}
        {loading ? (
          <div className="flex justify-center py-12">
            <LoadingSpinner size="lg" />
          </div>
        ) : searched && drugList.length === 0 ? (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-center py-12"
          >
            <span className="text-4xl block mb-3">🔍</span>
            <p className="text-white font-medium">No results found for "{query}"</p>
            <p className="text-slate-400 text-sm mt-1">Try a different drug name or check spelling</p>
          </motion.div>
        ) : drugList.length > 0 ? (
          <motion.div variants={container} initial="hidden" animate="show" className="space-y-4">
            {drugList.map((drug, i) => (
              <motion.div
                key={i}
                variants={item}
                whileHover={{ scale: 1.01, y: -2 }}
                className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6 hover:border-cyan-500/20 transition-all"
              >
                <div className="flex items-start justify-between gap-4 mb-3">
                  <div>
                    <h3 className="text-lg font-bold text-white" style={{ fontFamily: "Orbitron, sans-serif" }}>
                      {drug.name || drug.drugName || drug.brandName || query}
                    </h3>
                    {drug.genericName && (
                      <p className="text-slate-400 text-sm">{drug.genericName}</p>
                    )}
                  </div>
                  {drug.covered !== undefined && (
                    <span className={`shrink-0 text-xs px-3 py-1 rounded-full font-semibold border ${
                      drug.covered
                        ? "bg-emerald-500/15 text-emerald-400 border-emerald-500/30"
                        : "bg-red-500/15 text-red-400 border-red-500/30"
                    }`}>
                      {drug.covered ? "Covered" : "Not Covered"}
                    </span>
                  )}
                </div>

                {/* Details grid */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {drug.category && (
                    <div>
                      <span className="text-[10px] uppercase tracking-wider text-slate-500">Category</span>
                      <p className="text-white text-sm">{drug.category}</p>
                    </div>
                  )}
                  {drug.tier && (
                    <div>
                      <span className="text-[10px] uppercase tracking-wider text-slate-500">Tier</span>
                      <p className="text-white text-sm">{drug.tier}</p>
                    </div>
                  )}
                  {drug.dosageForm && (
                    <div>
                      <span className="text-[10px] uppercase tracking-wider text-slate-500">Form</span>
                      <p className="text-white text-sm">{drug.dosageForm}</p>
                    </div>
                  )}
                  {drug.copay !== undefined && (
                    <div>
                      <span className="text-[10px] uppercase tracking-wider text-slate-500">Copay</span>
                      <p className="text-white text-sm font-bold" style={{ fontFamily: "Orbitron, sans-serif" }}>
                        ₹{drug.copay}
                      </p>
                    </div>
                  )}
                </div>

                {/* Description / notes */}
                {(drug.description || drug.notes || drug.response) && (
                  <div className="mt-3 pt-3 border-t border-white/10">
                    <p className="text-slate-300 text-sm whitespace-pre-wrap">
                      {drug.description || drug.notes || drug.response}
                    </p>
                  </div>
                )}

                {/* Alternatives */}
                {drug.alternatives && drug.alternatives.length > 0 && (
                  <div className="mt-3 pt-3 border-t border-white/10">
                    <span className="text-[10px] uppercase tracking-wider text-slate-500 block mb-2">Alternatives</span>
                    <div className="flex flex-wrap gap-2">
                      {drug.alternatives.map((alt, j) => (
                        <span
                          key={j}
                          onClick={() => { setQuery(typeof alt === "string" ? alt : alt.name); }}
                          className="text-xs px-3 py-1 rounded-full bg-white/5 border border-white/10 text-slate-300 hover:text-cyan-400 hover:border-cyan-500/20 cursor-pointer transition-colors"
                        >
                          {typeof alt === "string" ? alt : alt.name}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </motion.div>
            ))}
          </motion.div>
        ) : !searched ? (
          <div className="text-center py-16">
            <motion.div
              animate={{ y: [0, -6, 0] }}
              transition={{ duration: 3, repeat: Infinity }}
              className="text-5xl mb-4 block"
            >
              💊
            </motion.div>
            <p className="text-slate-400 text-sm">Enter a drug name to search the formulary</p>
          </div>
        ) : null}
      </div>
    </div>
  );
}
