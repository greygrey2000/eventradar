import React from "react";
import api from "../api/apiClient";
import EventList, { Event } from "../components/EventList";
import VenueAutocomplete, { VenueOption } from "../components/VenueAutocomplete";
import useAuth from "../auth/useAuth";
import { useEventSearch } from "../hooks/useEventSearch";
import { useGeolocation } from "../hooks/useGeolocation";
import FilterBar from "../components/FilterBar";
import Pagination from "../components/Pagination";
import Spinner from "../components/Spinner";

const sortOptions = [
  { value: "date,asc", label: "Datum (aufsteigend)" },
  { value: "date,desc", label: "Datum (absteigend)" },
  { value: "name,asc", label: "Name (A-Z)" },
  { value: "name,desc", label: "Name (Z-A)" },
  { value: "venue,asc", label: "Ort (A-Z)" },
  { value: "venue,desc", label: "Ort (Z-A)" },
];

const Events: React.FC = () => {
  const {
    events,
    loading,
    error,
    page,
    totalPages,
    total,
    filters,
    setFilters,
    setPage,
    fetchEvents,
    resetFilters,
    setError,
  } = useEventSearch();
  const userLatLong = useGeolocation();

  // Fallback für userLatLong-Fehlschläge
  React.useEffect(() => {
    if (userLatLong === null) {
      // Optional: Logging oder Fallback
      // console.log("Geodaten nicht verfügbar, ortsbezogene Suche eingeschränkt.");
    }
  }, [userLatLong]);

  // Filter-Formular-Handler
  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (filters.dateFrom && filters.dateTo && filters.dateFrom > filters.dateTo) {
      setError("Das Startdatum darf nicht nach dem Enddatum liegen.");
      return;
    }
    setError("");
    setPage(0);
    fetchEvents(0);
  };

  // Handler als useCallback für bessere Performance
  const handleCityChange = React.useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setFilters(f => ({...f, city: e.target.value}));
  }, [setFilters]);
  const handleGenreChange = React.useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setFilters(f => ({...f, genre: e.target.value}));
  }, [setFilters]);
  const handleDateFromChange = React.useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setFilters(f => ({...f, dateFrom: e.target.value}));
  }, [setFilters]);
  const handleDateToChange = React.useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setFilters(f => ({...f, dateTo: e.target.value}));
  }, [setFilters]);
  const handleReload = React.useCallback(() => {
    fetchEvents(page);
  }, [fetchEvents, page]);

  React.useEffect(() => {
    fetchEvents(page);
  }, []);

  return (
    <div className="page-container">
      <div className="page-header" style={{flexDirection: "column", alignItems: "stretch", gap: 16}}>
        <h1>Alle Events</h1>
        <FilterBar
          filters={filters}
          setFilters={setFilters}
          sortOptions={sortOptions}
          onSubmit={handleFilterSubmit}
          onReset={resetFilters}
          loading={loading}
        >
          <input type="text" placeholder="Stadt" value={filters.city || ""} onChange={handleCityChange} className="filter-input" style={{minWidth:120, borderRadius: 8, border: '1px solid #ccc', padding: '8px 12px'}} />
          <input type="text" placeholder="Genre/Kategorie" value={filters.genre || ""} onChange={handleGenreChange} className="filter-input" style={{minWidth:120, borderRadius: 8, border: '1px solid #ccc', padding: '8px 12px'}} />
          <VenueAutocomplete value={filters.venueId || null} onChange={venue => setFilters(f => ({...f, venueId: venue}))} latlong={userLatLong} />
          {/* DatePicker-Felder werden in FilterBar direkt verwendet, daher hier entfernt */}
        </FilterBar>
        <button className="btn-secondary" onClick={handleReload} disabled={loading} style={{alignSelf: "flex-end"}}>
          {loading ? "Lädt..." : "Neu laden"}
        </button>
      </div>
      {error && <div className="form-error">{error}</div>}
      {loading ? <Spinner /> : events.length === 0 ? <div>Keine Events gefunden.</div> : <EventList events={events} />}
      <Pagination
        page={page}
        totalPages={totalPages}
        total={total}
        loading={loading}
        onPageChange={setPage}
      />
    </div>
  );
};

export default Events;