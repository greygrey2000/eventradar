import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/apiClient";

const RegisterForm: React.FC = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [location, setLocation] = useState("");
  const [interests, setInterests] = useState<string>("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

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
      <div className="form-group">
        <label htmlFor="location">Ort</label>
        <input
          id="location"
          type="text"
          placeholder="Ort"
          value={location}
          onChange={e => setLocation(e.target.value)}
          required
        />
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
