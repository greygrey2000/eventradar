import { useState, useCallback } from "react";
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

const initialFilters: EventFilters = {
  keyword: "",
  city: "",
  genre: "",
  venueId: null,
  dateFrom: "",
  dateTo: "",
  sort: "date,asc",
};

export const useEventFilters = () => {
  const [filters, setFilters] = useState<EventFilters>(initialFilters);

  const resetFilters = useCallback(() => {
    setFilters(initialFilters);
  }, []);

  return { filters, setFilters, resetFilters };
};
