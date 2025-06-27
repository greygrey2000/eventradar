import React, { useState } from "react";
import api from "../api/apiClient";

const EventForm: React.FC<{ onCreated?: () => void }> = ({ onCreated }) => {
  const [form, setForm] = useState({
    name: "",
    description: "",
    location: "",
    dateTime: "",
    tags: "",
    url: "",
    imageUrl: ""
  });
  const [error, setError] = useState("");

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    try {
      await api.post("/events", {
        ...form,
        tags: form.tags.split(",").map(t => t.trim()),
        dateTime: form.dateTime,
        importedAt: new Date().toISOString()
      });
      setForm({ name: "", description: "", location: "", dateTime: "", tags: "", url: "", imageUrl: "" });
      if (onCreated) onCreated();
    } catch {
      setError("Event konnte nicht erstellt werden");
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <h2>Event anlegen</h2>
      <input name="name" placeholder="Name" value={form.name} onChange={handleChange} required />
      <textarea name="description" placeholder="Beschreibung" value={form.description} onChange={handleChange} required />
      <input name="location" placeholder="Ort" value={form.location} onChange={handleChange} required />
      <input name="dateTime" type="datetime-local" value={form.dateTime} onChange={handleChange} required />
      <input name="tags" placeholder="Tags (Komma getrennt)" value={form.tags} onChange={handleChange} />
      <input name="url" placeholder="Event-URL" value={form.url} onChange={handleChange} />
      <input name="imageUrl" placeholder="Bild-URL" value={form.imageUrl} onChange={handleChange} />
      <button type="submit">Erstellen</button>
      {error && <div style={{color: "red"}}>{error}</div>}
    </form>
  );
};

export default EventForm;
