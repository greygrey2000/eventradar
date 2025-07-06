import React, { useState, useEffect, useRef } from "react";
import Autocomplete from "@mui/material/Autocomplete";
import TextField from "@mui/material/TextField";
import api from "../api/apiClient";
import useAuth from "../auth/useAuth";

export interface VenueOption {
  label: string;
  id: string;
  city?: string;
  country?: string;
}

export interface VenueAutocompleteProps {
  value: VenueOption | null;
  onChange: (value: VenueOption | null) => void;
  latlong?: string | null;
}

const VenueAutocomplete: React.FC<VenueAutocompleteProps> = ({ value, onChange, latlong }) => {
  const [inputValue, setInputValue] = useState("");
  const [options, setOptions] = useState<VenueOption[]>([]);
  const [loading, setLoading] = useState(false);
  const { user } = useAuth();
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (!inputValue) {
      setOptions([]);
      return;
    }
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      setLoading(true);
      try {
        const params: any = { keyword: inputValue };
        if (latlong) params.latlong = latlong;
        const res = await api.get("/places/suggest/venues", { params });
        const venues =
          res.data?._embedded?.venues?.map((v: any) => ({
            label: v.name + (v.city ? `, ${v.city.name}` : ""),
            id: v.id,
            city: v.city?.name,
            country: v.country?.name,
          })) || [];
        setOptions(venues);
      } catch {
        setOptions([]);
      } finally {
        setLoading(false);
      }
    }, 400); // Debounce auf 400ms
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [inputValue, latlong]);

  return (
    <Autocomplete
      options={options}
      loading={loading}
      value={value}
      onChange={(_, newValue) => onChange(newValue)}
      inputValue={inputValue}
      onInputChange={(_, newInputValue) => setInputValue(newInputValue)}
      renderInput={(params) => (
        <TextField {...params} label="Venue suchen" variant="outlined" size="small" />
      )}
      isOptionEqualToValue={(opt, val) => opt.id === val.id}
    />
  );
};

export default VenueAutocomplete;
