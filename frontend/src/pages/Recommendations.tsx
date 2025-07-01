import React, { useEffect, useState } from "react";
import api from "../api/apiClient";

interface Recommendation {
  id: number;
  title: string;
  description: string;
  date: string;
  location: string;
}

const Recommendations: React.FC = () => {
  const [recs, setRecs] = useState<Recommendation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/recommendations")
      .then(res => setRecs(res.data))
      .catch(() => setError("Empfehlungen konnten nicht geladen werden."))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="page-container">
      <h1 style={{ color: "#1976d2", textAlign: "center", marginBottom: 24 }}>Empfohlene Events</h1>
      {loading && <div className="centered">Lädt Empfehlungen...</div>}
      {error && <div className="form-error">{error}</div>}
      <div className="recommendations-list">
        {recs.map(rec => (
          <div key={rec.id} className="event-card">
            <h3 className="event-title">{rec.title}</h3>
            <p className="event-desc">{rec.description}</p>
            <div className="event-meta">
              <span role="img" aria-label="Datum" className="event-meta-icon">📅</span>
              <span>{new Date(rec.date).toLocaleDateString()}</span>
              <span role="img" aria-label="Ort" className="event-meta-icon" style={{marginLeft:8}}>📍</span>
              <span>{rec.location}</span>
            </div>
          </div>
        ))}
        {!loading && recs.length === 0 && <div className="form-error">Keine Empfehlungen gefunden.</div>}
      </div>
    </div>
  );
};

export default Recommendations;