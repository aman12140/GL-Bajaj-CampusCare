export default function Pagination({ page, totalPages, totalElements, onChange }) {
  if (!totalPages || totalPages <= 1) {
    return totalElements ? <div className="pagination"><span className="muted">{totalElements} result{totalElements === 1 ? '' : 's'}</span></div> : null;
  }
  return (
    <div className="pagination">
      <span className="muted">{totalElements} results - page {page + 1} of {totalPages}</span>
      <div>
        <button type="button" className="btn btn-secondary" disabled={page === 0} onClick={() => onChange(page - 1)}>Previous</button>
        <button type="button" className="btn btn-secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Next</button>
      </div>
    </div>
  );
}
