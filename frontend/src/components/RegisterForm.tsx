import React, { useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/apiClient";

const RegisterForm: React.FC = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [location, setLocation] = useState("");
  const [locationSuggestions, setLocationSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [interests, setInterests] = useState<string>("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const locationInputRef = useRef<HTMLInputElement>(null);

  // Autocomplete für Location
  const handleLocationChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setLocation(value);
    setShowSuggestions(true);
    if (value.length < 2) {
      setLocationSuggestions([]);
      return;
    }
    try {
      // Passe den API-Aufruf an den neuen Suggest-Endpoint an
      const res = await api.get(`/places/suggest/places?q=${encodeURIComponent(value)}`);
      setLocationSuggestions(res.data);
    } catch {
      setLocationSuggestions([]);
    }
  };

  const handleSuggestionClick = (suggestion: string) => {
    setLocation(suggestion);
    setShowSuggestions(false);
    setLocationSuggestions([]);
    locationInputRef.current?.blur();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    setSuccess(false);
    try {
      await api.post("/auth/register", {
        email,
        password,
        name,
        location,
        interests: interests.split(",").map(i => i.trim()).filter(Boolean),
      });
      setSuccess(true);
      setTimeout(() => navigate("/login"), 1200);
    } catch (err: any) {
      setError(err?.response?.data || "Registrierung fehlgeschlagen");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="form-card">
      <h2 className="form-title">Registrieren</h2>
      <div className="form-group">
        <label htmlFor="name">Name</label>
        <input
          id="name"
          type="text"
          placeholder="Name"
          value={name}
          onChange={e => setName(e.target.value)}
          required
        />
      </div>
      <div className="form-group">
        <label htmlFor="email">E-Mail</label>
        <input
          id="email"
          type="email"
          placeholder="E-Mail"
          value={email}
          onChange={e => setEmail(e.target.value)}
          required
        />
      </div>
      <div className="form-group">
        <label htmlFor="password">Passwort</label>
        <input
          id="password"
          type="password"
          placeholder="Passwort"
          value={password}
          onChange={e => setPassword(e.target.value)}
          required
        />
      </div>
      <div className="form-group" style={{position: "relative"}}>
        <label htmlFor="location">Ort</label>
        <input
          id="location"
          type="text"
          placeholder="Ort"
          value={location}
          onChange={handleLocationChange}
          onFocus={() => setShowSuggestions(true)}
          autoComplete="off"
          ref={locationInputRef}
          required
        />
        {showSuggestions && locationSuggestions.length > 0 && (
          <ul className="autocomplete-dropdown" style={{position: "absolute", zIndex: 10, background: "white", border: "1px solid #ccc", width: "100%", maxHeight: 150, overflowY: "auto", margin: 0, padding: 0, listStyle: "none"}}>
            {locationSuggestions.map((s, i) => (
              <li key={i} style={{padding: 8, cursor: "pointer"}} onMouseDown={() => handleSuggestionClick(s)}>{s}</li>
            ))}
          </ul>
        )}
      </div>
      <div className="form-group">
        <label htmlFor="interests">Interessen (Komma-getrennt)</label>
        <input
          id="interests"
          type="text"
          placeholder="Interessen (Komma-getrennt)"
          value={interests}
          onChange={e => setInterests(e.target.value)}
        />
      </div>
      <button className="btn-primary" type="submit" disabled={loading}>
        {loading ? "Registriere..." : "Registrieren"}
      </button>
      {error && <div className="form-error">{error}</div>}
      {success && <div className="form-success">Erfolgreich registriert! Weiterleitung...</div>}
    </form>
  );
};

export default RegisterForm;
