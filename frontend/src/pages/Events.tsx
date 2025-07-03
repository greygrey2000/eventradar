import React, { useEffect, useState } from "react";
import api from "../api/apiClient";
import EventList, { Event } from "../components/EventList";
import VenueAutocomplete, { VenueOption } from "../components/VenueAutocomplete";
import useAuth from "../auth/useAuth";

const sortOptions = [
  { value: "date,asc", label: "Datum (aufsteigend)" },
  { value: "date,desc", label: "Datum (absteigend)" },
  { value: "name,asc", label: "Name (A-Z)" },
  { value: "name,desc", label: "Name (Z-A)" },
  { value: "venue,asc", label: "Ort (A-Z)" },
  { value: "venue,desc", label: "Ort (Z-A)" },
];

const Events: React.FC = () => {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  // Filter & Sortier-States
  const [keyword, setKeyword] = useState("");
  const [city, setCity] = useState("");
  const [genre, setGenre] = useState("");
  const [venue, setVenue] = useState("");
  const [venueId, setVenueId] = useState<VenueOption | null>(null);
  const [userLatLong, setUserLatLong] = useState<string | null>(null);
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [sort, setSort] = useState("date,asc");
  const { user } = useAuth();

  // Hilfsfunktion für Reset
  const resetFilters = () => {
    setKeyword("");
    setCity("");
    setGenre("");
    setVenue("");
    setVenueId(null);
    setDateFrom("");
    setDateTo("");
    setSort("date,asc");
    setPage(0);
    setError("");
    // Nach dem State-Reset Events neu laden
    setTimeout(() => fetchEvents(0), 0);
  };

  // useEffect für Filter-Reset: Wenn einer der Filter auf Standard zurückgesetzt wird, Events neu laden
  useEffect(() => {
    if (
      keyword === "" &&
      city === "" &&
      genre === "" &&
      venue === "" &&
      dateFrom === "" &&
      dateTo === "" &&
      sort === "date,asc" &&
      page === 0
    ) {
      fetchEvents(0);
    }
    // eslint-disable-next-line
  }, [keyword, city, genre, venue, dateFrom, dateTo, sort, page]);

  const fetchEvents = async (pageParam = page) => {
    setLoading(true);
    setError("");
    // ISO-8601 für Ticketmaster: yyyy-MM-dd'T'HH:mm:ssZ
    const isoDateFrom = dateFrom ? `${dateFrom}T00:00:00Z` : undefined;
    const isoDateTo = dateTo ? `${dateTo}T23:59:59Z` : undefined;
    try {
      const res = await api.get("/events", {
        params: {
          page: pageParam,
          size,
          keyword: keyword || undefined,
          city: city || undefined,
          genre: genre || undefined,
          venueId: venueId?.id || undefined,
          startDateTime: isoDateFrom,
          endDateTime: isoDateTo,
          sort: sort || undefined,
        },
      });
      setEvents(res.data.events);
      setTotalPages(res.data.totalPages);
      setTotal(res.data.total);
      setPage(res.data.page);
    } catch {
      setError("Events konnten nicht geladen werden.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents(page);
    // eslint-disable-next-line
  }, [page]);

  // Filter-Formular-Handler
  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // Validierung: Startdatum darf nicht nach dem Enddatum liegen
    if (dateFrom && dateTo && dateFrom > dateTo) {
      setError("Das Startdatum darf nicht nach dem Enddatum liegen.");
      return;
    }
    setError("");
    setPage(0);
    fetchEvents(0);
  };

  const handlePrev = () => setPage((p) => Math.max(0, p - 1));
  const handleNext = () => setPage((p) => Math.min(totalPages - 1, p + 1));

  // Hole Nutzer-Position für ortsbezogene Venue-Suche
  useEffect(() => {
    // Wenn User eingeloggt und Profil-Location vorhanden, nutze diese für Venue-Suche
    if (user && user.location) {
      // Geocoding-API aufrufen, um latlong zu bekommen
      api.get("/places/geocode", { params: { location: user.location } })
        .then(res => {
          if (res.data && res.data.lat && res.data.lng) {
            setUserLatLong(`${res.data.lat},${res.data.lng}`);
          } else {
            setUserLatLong(null);
          }
        })
        .catch(() => setUserLatLong(null));
    } else if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setUserLatLong(`${pos.coords.latitude},${pos.coords.longitude}`);
        },
        () => setUserLatLong(null),
        { enableHighAccuracy: false, timeout: 5000 }
      );
    }
  }, [user]);

  return (
    <div className="page-container">
      <div className="page-header" style={{flexDirection: "column", alignItems: "stretch", gap: 16}}>
        <h1>Alle Events</h1>
        <form onSubmit={handleFilterSubmit} style={{display: "flex", flexWrap: "wrap", gap: 12, alignItems: "center", marginBottom: 8}}>
          <div className="filter-bar" style={{
    display: "flex",
    flexWrap: "wrap",
    gap: 16,
    alignItems: "center",
    background: "#f8f9fa",
    borderRadius: 12,
    padding: 16,
    boxShadow: "0 2px 8px rgba(0,0,0,0.04)",
    marginBottom: 16,
    border: "1px solid #e0e0e0"
  }}>
            <input type="text" placeholder="Stichwort" value={keyword} onChange={e => setKeyword(e.target.value)} className="filter-input" style={{minWidth:140, borderRadius: 8, border: '1px solid #ccc', padding: '8px 12px'}} />
            <input type="text" placeholder="Stadt" value={city} onChange={e => setCity(e.target.value)} className="filter-input" style={{minWidth:120, borderRadius: 8, border: '1px solid #ccc', padding: '8px 12px'}} />
            <input type="text" placeholder="Genre/Kategorie" value={genre} onChange={e => setGenre(e.target.value)} className="filter-input" style={{minWidth:120, borderRadius: 8, border: '1px solid #ccc', padding: '8px 12px'}} />
            <VenueAutocomplete value={venueId} onChange={setVenueId} latlong={userLatLong} />
            <input type="date" value={dateFrom} onChange={e => setDateFrom(e.target.value)} title="Von" className="filter-input" style={{borderRadius: 8, border: '1px solid #ccc', padding: '8px 12px'}} />
            <input type="date" value={dateTo} onChange={e => setDateTo(e.target.value)} title="Bis" className="filter-input" style={{borderRadius: 8, border: '1px solid #ccc', padding: '8px 12px'}} />
            <select value={sort} onChange={e => setSort(e.target.value)} className="filter-input" style={{borderRadius: 8, border: '1px solid #ccc', padding: '8px 12px'}}>
              {sortOptions.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
            </select>
            <button className="btn-secondary" type="submit" disabled={loading} style={{borderRadius: 8, padding: '8px 18px', fontWeight: 500}}>Filtern</button>
            <button
              className="btn-secondary"
              type="button"
              onClick={resetFilters}
              disabled={loading}
              style={{ borderRadius: 8, padding: '8px 18px' }}
            >
              Zurücksetzen
            </button>
          </div>
        </form>
        <button className="btn-secondary" onClick={() => fetchEvents(page)} disabled={loading} style={{alignSelf: "flex-end"}}>
          {loading ? "Lädt..." : "Neu laden"}
        </button>
      </div>
      {error && <div className="form-error">{error}</div>}
      <EventList events={events} />
      <div style={{ display: "flex", justifyContent: "center", alignItems: "center", marginTop: 24, gap: 16 }}>
        <button className="btn-secondary" onClick={handlePrev} disabled={page === 0 || loading}>&laquo; Vorherige</button>
        <span style={{ minWidth: 80, textAlign: "center" }}>
          Seite {page + 1} von {totalPages} ({total} Events)
        </span>
        <button className="btn-secondary" onClick={handleNext} disabled={page >= totalPages - 1 || loading}>Nächste &raquo;</button>
      </div>
    </div>
  );
};

export default Events;