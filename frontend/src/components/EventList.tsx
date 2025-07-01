import React from "react";

// Angepasstes Event-DTO für Ticketmaster
export type Event = {
  id: string; // Ticketmaster-Event-ID
  name: string;
  description?: string;
  date?: string;
  location?: string;
  imageUrl?: string;
  url?: string;
};

interface EventListProps {
  events: Event[];
}

const EventList: React.FC<EventListProps> = ({ events }) => {
  if (events.length === 0) {
    return <div className="form-error">Keine Events gefunden.</div>;
  }
  return (
    <div className="event-list">
      {events.map((event) => (
        <div key={event.id} className="event-card">
          <h3 className="event-title">{event.name}</h3>
          {event.imageUrl && <img src={event.imageUrl} alt={event.name} style={{maxWidth:200}} />}
          <p className="event-desc">{event.description}</p>
          <div className="event-meta">
            {event.date && <><span role="img" aria-label="Datum" className="event-meta-icon">📅</span>
            <span>{new Date(event.date).toLocaleDateString()}</span></>}
            {event.location && <><span role="img" aria-label="Ort" className="event-meta-icon" style={{marginLeft:8}}>📍</span>
            <span>{event.location}</span></>}
          </div>
          {event.url && <a href={event.url} target="_blank" rel="noopener noreferrer">Zum Event</a>}
        </div>
      ))}
    </div>
  );
};

export default EventList;
