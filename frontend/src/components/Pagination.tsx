import React from "react";

interface PaginationProps {
  page: number;
  totalPages: number;
  total: number;
  loading: boolean;
  onPageChange: (page: number) => void;
}

const Pagination: React.FC<PaginationProps> = ({ page, totalPages, total, loading, onPageChange }) => (
  <div style={{ display: "flex", justifyContent: "center", alignItems: "center", marginTop: 24, gap: 16 }}>
    <button className="btn-secondary" onClick={() => onPageChange(Math.max(0, page - 1))} disabled={page === 0 || loading}>&laquo; Vorherige</button>
    <span style={{ minWidth: 80, textAlign: "center" }}>
      Seite {page + 1} von {totalPages} ({total} Events)
    </span>
    <button className="btn-secondary" onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))} disabled={page >= totalPages - 1 || loading}>Nächste &raquo;</button>
  </div>
);

export default Pagination;
