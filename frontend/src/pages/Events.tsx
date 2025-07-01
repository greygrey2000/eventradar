import React, { useEffect, useState } from "react";
import api from "../api/apiClient";
import EventList, { Event } from "../components/EventList";

const Events: React.FC = () => {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const fetchEvents = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await api.get("/events");
      setEvents(res.data);
    } catch {
      setError("Events konnten nicht geladen werden.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents();
  }, []);

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>Alle Events</h1>
        <button className="btn-secondary" onClick={fetchEvents} disabled={loading}>
          {loading ? "Lädt..." : "Neu laden"}
        </button>
      </div>
      {error && <div className="form-error">{error}</div>}
      <EventList events={events} />
    </div>
  );
};

export default Events;