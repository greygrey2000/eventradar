import { useState, useCallback } from "react";
import api from "../api/apiClient";
import { Event } from "../components/EventList";
import { VenueOption } from "../components/VenueAutocomplete";

export interface EventFilters {
  keyword?: string;
  city?: string;
  genre?: string;
  venueId?: VenueOption | null;
  dateFrom?: string;
  dateTo?: string;
  sort?: string;
}

export const useEventSearch = (initialFilters: EventFilters = {}) => {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  const [filters, setFilters] = useState<EventFilters>(initialFilters);

  const fetchEvents = useCallback(async (pageParam = 0, customFilters?: EventFilters) => {
    setLoading(true);
    setError("");
    const f = customFilters || filters;
    const isoDateFrom = f.dateFrom ? `${f.dateFrom}T00:00:00Z` : undefined;
    const isoDateTo = f.dateTo ? `${f.dateTo}T23:59:59Z` : undefined;
    try {
      const res = await api.get("/events", {
        params: {
          page: pageParam,
          size,
          keyword: f.keyword || undefined,
          city: f.city || undefined,
          genre: f.genre || undefined,
          venueId: f.venueId?.id || undefined,
          startDateTime: isoDateFrom,
          endDateTime: isoDateTo,
          sort: f.sort || undefined,
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
  }, [filters, size]);

  const resetFilters = useCallback(() => {
    setFilters({ sort: "date,asc" });
    setPage(0);
    setError("");
    fetchEvents(0, { sort: "date,asc" });
  }, [fetchEvents]);

  return {
    events,
    loading,
    error,
    page,
    size,
    totalPages,
    total,
    filters,
    setFilters,
    setPage,
    fetchEvents,
    resetFilters,
    setError,
  };
};
