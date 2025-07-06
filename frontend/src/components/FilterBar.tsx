import React from "react";
import { VenueOption } from "./VenueAutocomplete";
import { useTheme } from "../theme/ThemeContext";
import { FiSearch, FiCalendar } from "react-icons/fi";
import DatePicker, { registerLocale } from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { de } from "date-fns/locale/de";
import { Locale } from "date-fns";
registerLocale("de", de as unknown as Locale);

interface SortOption {
  value: string;
  label: string;
}

export interface FilterState {
  keyword?: string;
  city?: string;
  genre?: string;
  venueId?: VenueOption | null;
  dateFrom?: string;
  dateTo?: string;
  sort?: string;
}

interface FilterBarProps {
  filters: FilterState;
  setFilters: React.Dispatch<React.SetStateAction<FilterState>>;
  sortOptions: SortOption[];
  onSubmit: (e: React.FormEvent) => void;
  onReset: () => void;
  loading: boolean;
  children?: React.ReactNode;
}

const FilterBar: React.FC<FilterBarProps> = ({
  filters,
  setFilters,
  sortOptions,
  onSubmit,
  onReset,
  loading,
  children,
}) => {
  const { theme, toggleTheme } = useTheme();

  const inputStyle: React.CSSProperties = {
    width: "100%",
    height: 40,
    padding: "8px 12px",
    border: "1px solid var(--input-border)",
    borderRadius: 8,
    fontSize: "1rem",
    background: "var(--input-bg)",
    color: "var(--input-color)",
    boxSizing: "border-box",
  };

  return (
    <form
      onSubmit={onSubmit}
      className="filter-bar"
      style={{
        display: "grid",
        gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
        gap: 16,
        background: "var(--filter-bg)",
        padding: 20,
        borderRadius: 12,
        boxShadow: "0 2px 10px var(--filter-shadow)",
        alignItems: "center",
        marginBottom: 16,
        border: "1px solid var(--filter-border)",
      }}
    >
      <div style={{ position: "relative" }}>
        <FiSearch style={{ position: "absolute", top: 10, left: 10, color: "#888" }} />
        <input
          type="text"
          placeholder="Stichwort..."
          value={filters.keyword || ""}
          onChange={(e) => setFilters((f) => ({ ...f, keyword: e.target.value }))}
          style={{ ...inputStyle, paddingLeft: 32 }}
        />
      </div>

      {children}

      <DatePicker
        selected={filters.dateFrom ? new Date(filters.dateFrom) : null}
        onChange={(date) =>
          setFilters((f) => ({ ...f, dateFrom: date ? date.toISOString().slice(0, 10) : "" }))
        }
        placeholderText="Von..."
        className="filter-input"
        dateFormat="dd.MM.yyyy"
        locale="de"
        wrapperClassName="date-picker-wrapper"
      />

      <DatePicker
        selected={filters.dateTo ? new Date(filters.dateTo) : null}
        onChange={(date) =>
          setFilters((f) => ({ ...f, dateTo: date ? date.toISOString().slice(0, 10) : "" }))
        }
        placeholderText="Bis..."
        className="filter-input"
        dateFormat="dd.MM.yyyy"
        locale="de"
        wrapperClassName="date-picker-wrapper"
      />

      <select
        value={filters.sort || "date,asc"}
        onChange={(e) => setFilters((f) => ({ ...f, sort: e.target.value }))}
        style={inputStyle}
      >
        {sortOptions.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>

      <div
        style={{
          display: "flex",
          gap: 8,
          gridColumn: "1 / -1",
          justifyContent: "flex-end",
          flexWrap: "wrap",
        }}
      >
        <button className="btn-primary" type="submit" disabled={loading}>
          Filtern
        </button>
        <button className="btn-secondary" type="button" onClick={onReset} disabled={loading}>
          Zurücksetzen
        </button>
        <button
          type="button"
          className="btn-secondary"
          aria-label="Theme wechseln"
          onClick={toggleTheme}
          style={{ marginLeft: 8 }}
        >
          {theme === "dark" ? "🌙" : "☀️"}
        </button>
      </div>
    </form>
  );
};

export default FilterBar;
