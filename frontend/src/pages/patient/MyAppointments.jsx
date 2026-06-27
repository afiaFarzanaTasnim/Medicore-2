import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import Navbar from "../../components/Navbar";
import { apiRequest, getErrorMessage } from "../../api/client";
import { ENDPOINTS } from "../../api/endpoints";

function formatDate(value) {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleDateString();
}

export default function MyAppointments() {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [fetchedOnce, setFetchedOnce] = useState(false);

  async function loadAppointments() {
    setLoading(true);
    setError("");
    try {
      const res = await apiRequest(ENDPOINTS.appointments, { auth: true });
      if (res?.success) {
        setAppointments(Array.isArray(res.data) ? res.data : []);
      } else {
        setError(res?.message ?? "Failed to load appointments.");
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
      setFetchedOnce(true);
    }
  }

  useEffect(() => {
    loadAppointments();
  }, []);

  return (
    <>
      <Navbar />
      <div className="container">
        <div className="page-header">
          <p className="section-eyebrow role-patient">Patient Portal</p>
          <h1 className="page-title">My Appointments</h1>
          <div className="accent-line"></div>
          <p className="page-subtitle">All appointments you have booked, newest first.</p>
        </div>

        <div style={{ display: "flex", gap: 12, marginBottom: 16 }}>
          <Link to="/patient/book" className="btn btn-primary">
            + Book New Appointment
          </Link>
          <button
            type="button"
            className="btn btn-outline btn-sm"
            onClick={loadAppointments}
            disabled={loading}
          >
            {loading ? "Refreshing…" : "Refresh"}
          </button>
        </div>

        {error && (
          <div
            className="alert alert-error"
            style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12 }}
          >
            <span>{error}</span>
            <button type="button" className="btn btn-outline btn-sm" onClick={loadAppointments}>
              Retry
            </button>
          </div>
        )}

        {loading && <p className="page-subtitle">Loading appointments…</p>}

        {fetchedOnce && !loading && !error && appointments.length === 0 && (
          <div className="card empty-state">
            <p className="empty-state__icon">📅</p>
            <p>You have no appointments yet.</p>
            <Link to="/patient/book" className="btn btn-primary" style={{ marginTop: 12 }}>
              Book your first appointment
            </Link>
          </div>
        )}

        {fetchedOnce && !loading && !error && appointments.length > 0 && (
          <>
            <p className="search-meta">
              {appointments.length} appointment{appointments.length !== 1 ? "s" : ""} found
            </p>
            <div className="mc-table-wrap">
              <table className="mc-table">
                <thead>
                  <tr>
                    <th>Doctor</th>
                    <th>Specialization</th>
                    <th>Date</th>
                    <th>Serial</th>
                    <th>Location</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {appointments.map((appt) => (
                    <tr key={appt.appointmentId ?? appt.prescriptionID ?? `${appt.doctor_id}-${appt.date}`}>
                      <td>{appt.doctor_info?.name ?? appt.doctor_name ?? "—"}</td>
                      <td>
                        <span className="badge badge-accent">
                          {appt.doctor_info?.specialization ?? appt.specialization ?? "—"}
                        </span>
                      </td>
                      <td>{formatDate(appt.date)}</td>
                      <td>{appt.serial_no ?? "—"}</td>
                      <td>{appt.location ?? "—"}</td>
                      <td>
                        <span className="badge role-patient">{appt.status ?? "Booked"}</span>
                      </td>
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