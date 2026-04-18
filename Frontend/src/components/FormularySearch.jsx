import React, { useState } from 'react';
import { searchFormulary } from '../services/api';
import './FormularySearch.css';

const FormularySearch = () => {
    const [query, setQuery] = useState('');
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(false);

    const handleSearch = async (e) => {
        e.preventDefault();
        if (!query.trim()) return;

        setLoading(true);
        try {
            const data = await searchFormulary(query);
            setResult(data);
        } catch (err) {
            console.error("Formulary search failed", err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="formulary-search">
            <form onSubmit={handleSearch} className="search-box">
                <input 
                    type="text" 
                    placeholder="Search medication coverage..." 
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    className="search-input"
                />
                <button type="submit" className="search-button" disabled={loading}>
                    {loading ? '...' : '🔍'}
                </button>
            </form>

            {result && (
                <div className="search-result-card animate-in">
                    <div className="result-header">
                        <span className="pill">AI Policy Lookup</span>
                        <button className="close-btn" onClick={() => setResult(null)}>×</button>
                    </div>
                    <div className="result-body">
                        <p className="result-text">{result.finalAnswer}</p>
                    </div>
                </div>
            )}
        </div>
    );
};

export default FormularySearch;
