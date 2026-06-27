import { useState, useEffect, useMemo } from "react";
import { apiRequest } from "../../api/client";
import { ENDPOINTS } from "../../api/endpoints";
import Navbar from "../../components/Navbar";

export default function DoctorDirectory() {
  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [specialization, setSpecialization] = useState("");
  const [location, setLocation] = useState("");

  async function loadDoctors() {
    setLoading(true);
    setError("");
    try {
      const res = await apiRequest(ENDPOINTS.doctors, { auth: true });
      setDoctors(res.data ?? []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadDoctors();
  }, []);

  const filtered = useMemo(() => {
    const spec = specialization.trim().toLowerCase();
    const loc = location.trim().toLowerCase();
    return doctors.filter((doc) => {
      const specMatch = !spec || (doc.specialization || "").toLowerCase().includes(spec);
      const locMatch = !loc || (doc.location || "").toLowerCase().includes(loc);
      return specMatch && locMatch;
    });
  }, [doctors, specialization, location]);

  const hasFilters = specialization || location;

  return (
    <>
      <Navbar />
      <div className="container">
        <div className="page-header">
          <div>
            <p className="section-eyebrow role-patient">Patient Portal</p>
            <h1 className="page-title">Available Doctors</h1>
          </div>
        </div>

        <form className="form-row" onSubmit={(e) => e.preventDefault()}>
          <div className="form-group">
            <label className="form-label" htmlFor="specialization">Specialization</label>
            <input
              id="specialization"
              className="form-control"
              placeholder="e.g. Cardiology"
              value={specialization}
              onChange={(e) => setSpecialization(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label className="form-label" htmlFor="location">Location</label>
            <input
              id="location"
              className="form-control"
              placeholder="e.g. Building A"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
            />
          </div>
          {hasFilters && (
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => { setSpecialization(""); setLocation(""); }}
            >
              Clear
            </button>
          )}
        </form>

        {error && (
          <div className="alert alert-error" style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12 }}>
            <span>{error}</span>
            <button className="btn btn-outline btn-sm" onClick={loadDoctors}>Retry</button>
          </div>
        )}

        {loading && <p className="page-subtitle">Loading doctors…</p>}

        {!loading && !error && filtered.length === 0 && (
          <div className="card empty-state">
            <p className="empty-state__icon">🩺</p>
            <p>{hasFilters ? "No doctors match your search." : "No approved doctors yet."}</p>
          </div>
        )}

        {!loading && !error && filtered.length > 0 && (
          <>
            {hasFilters && (
              <p className="search-meta">
                {filtered.length} doctor{filtered.length !== 1 ? "s" : ""} found
              </p>
            )}
            <div className="mc-table-wrap">
              <table className="mc-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Specialization</th>
                    <th>Rating</th>
                    <th>Fee</th>
                    <th>Location</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((doc) => (
                    <tr key={doc.doctorId}>
                      <td>{doc.name}</td>
                      <td><span className="badge badge-accent">{doc.specialization}</span></td>
                      <td>{doc.rating ?? "—"}</td>
                      <td>${doc.visiting_fee}</td>
                      <td>{doc.location}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>
    </>
  );
}