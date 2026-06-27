// src/pages/admin/AdminDonors.jsx
//
// Admin: oversee the blood-donor registry with blood-group filter and per-row
// deletion. Mirrors the patient-facing Blooddonor page but with admin powers.
//
// Backend endpoints (paths registered in ENDPOINTS):
//   GET    /bloodbank/admin/donors?bloodGroup=A+   -> all donors, optional filter
//   DELETE /bloodbank/admin/donors/{bloodBankId}
//
// Response shape per row:
//   { bloodBankId, name, contactNo, donorId, lastdate, bloodgroup }

import { useEffect, useMemo, useState } from "react";
import Navbar from "../../components/Navbar";
import { apiRequest, getErrorMessage } from "../../api/client";
import { ENDPOINTS } from "../../api/endpoints";

const BLOOD_GROUPS = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

export default function AdminDonors() {
  const [donors, setDonors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [bloodGroup, setBloodGroup] = useState("All");
  const [search, setSearch] = useState("");
  const [busy, setBusy] = useState({}); // bloodBankId -> "delete"
  const [confirmDelete, setConfirmDelete] = useState(null);

  async function loadDonors(group = bloodGroup) {
    setLoading(true);
    setError("");
    try {
      const res = await apiRequest(ENDPOINTS.adminDonors(group), { auth: true });
      setDonors(res?.data ?? res ?? []);
    } catch (err) {
      setError(getErrorMessage(err));
      setDonors([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadDonors(bloodGroup);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bloodGroup]);

  // Client-side text search on top of the server-side blood-group filter
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return donors;
    return donors.filter((d) => {
      const haystack = [d.name, d.contactNo, d.donorId, d.bloodgroup]
        .filter(Boolean).join(" ").toLowerCase();
      return haystack.includes(q);
    });
  }, [donors, search]);

  async function handleDelete(donor) {
    const id = donor.bloodBankId;
    setBusy((b) => ({ ...b, [id]: "delete" }));
    try {
      await apiRequest(ENDPOINTS.adminDeleteDonor(id), { method: "DELETE", auth: true });
      setDonors((prev) => prev.filter((row) => row.bloodBankId !== id));
      setConfirmDelete(null);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusy((b) => { const n = { ...b }; delete n[id]; return n; });
    }
  }

  function formatDate(s) {
    if (!s) return "—";
    const d = new Date(s);
    return Number.isNaN(d.getTime()) ? s : d.toLocaleDateString();
  }

  return (
    <>
      <Navbar />
      <div className="container">
        <div className="page-header">
          <div>
            <p className="section-eyebrow role-admin">Admin Portal</p>
            <h1 className="page-title">Blood Donors</h1>
            <p className="page-subtitle">
              Oversee the donor registry, filter by blood group, remove outdated entries.
            </p>
          </div>
        </div>

        {error && (
          <div className="alert alert-error" style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12 }}>
            <span>{error}</span>
            <button className="btn btn-outline btn-sm" onClick={() => loadDonors(bloodGroup)}>Retry</button>
          </div>
        )}

        <div className="form-row">
          <div className="form-group">
            <label className="form-label" htmlFor="donor-bg">Blood Group</label>
            <select
              id="donor-bg"
              className="form-control"
              value={bloodGroup}
              onChange={(e) => setBloodGroup(e.target.value)}
            >
              <option value="All">All groups</option>
              {BLOOD_GROUPS.map((bg) => (
                <option key={bg} value={bg}>{bg}</option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label className="form-label" htmlFor="donor-search">Search</label>
            <input
              id="donor-search"
              className="form-control"
              placeholder="Name, contact, or donor ID…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>

        {loading ? (
          <p className="page-subtitle">Loading donors…</p>
        ) : filtered.length === 0 ? (
          <div className="card empty-state">
            <p className="empty-state__icon">🩸</p>
            <p>
              {search
                ? "No donors match your search."
                : bloodGroup === "All"
                ? "No donors registered yet."
                : `No donors for blood group ${bloodGroup}.`}
            </p>
          </div>
        ) : (
          <p className="search-meta">
            {filtered.length} donor{filtered.length !== 1 ? "s" : ""}
            {bloodGroup !== "All" ? ` (${bloodGroup})` : ""}
          </p>
        )}

        {!loading && filtered.length > 0 && (
          <div className="mc-table-wrap">
            <table className="mc-table">
              <thead>
                <tr>
                  <th>Blood Bank ID</th>
                  <th>Name</th>
                  <th>Blood Group</th>
                  <th>Contact</th>
                  <th>Donor ID</th>
                  <th>Last Donated</th>
                  <th style={{ textAlign: "right" }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((d) => (
                  <tr key={d.bloodBankId}>
                    <td style={{ fontFamily: "monospace", fontSize: 13 }}>{d.bloodBankId}</td>
                    <td>{d.name ?? "—"}</td>
                    <td>
                      <span className="badge role-patient">
                        {d.bloodgroup ?? "—"}
                      </span>
                    </td>
                    <td>{d.contactNo ?? "—"}</td>
                    <td style={{ fontFamily: "monospace", fontSize: 13 }}>{d.donorId ?? "—"}</td>
                    <td>{formatDate(d.lastdate)}</td>
                    <td style={{ textAlign: "right" }}>
                      <button
                        className="btn btn-danger btn-sm"
                        disabled={!!busy[d.bloodBankId]}
                        onClick={() => setConfirmDelete(d)}
                      >
                        {busy[d.bloodBankId] === "delete" ? "Deleting…" : "Remove"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* ─── Delete confirmation modal ──────────────────────────────── */}
        {confirmDelete && (
          <div
            style={{
              position: "fixed", inset: 0, background: "rgba(17,24,39,0.5)",
              display: "flex", alignItems: "center", justifyContent: "center",
              zIndex: 1000, padding: 20,
            }}
            onClick={() => setConfirmDelete(null)}
          >
            <div
              className="card"
              style={{ maxWidth: 420, width: "100%" }}
              onClick={(e) => e.stopPropagation()}
            >
              <p className="section-eyebrow" style={{ color: "#DC2626" }}>Confirm Removal</p>
              <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 8 }}>Remove donor?</h2>
              <p style={{ fontSize: 14, color: "var(--text-muted)", marginBottom: 20, lineHeight: 1.55 }}>
                You're about to remove{" "}
                <strong style={{ color: "var(--text)" }}>{confirmDelete.name ?? confirmDelete.donorId ?? confirmDelete.bloodBankId}</strong>{" "}
                ({confirmDelete.bloodgroup ?? "—"}) from the donor registry. This cannot be undone.
              </p>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}>
                <button
                  className="btn btn-ghost"
                  onClick={() => setConfirmDelete(null)}
                  disabled={busy[confirmDelete.bloodBankId] === "delete"}
                >
                  Cancel
                </button>
                <button
                  className="btn btn-danger"
                  onClick={() => handleDelete(confirmDelete)}
                  disabled={busy[confirmDelete.bloodBankId] === "delete"}
                >
                  {busy[confirmDelete.bloodBankId] === "delete" ? "Removing…" : "Remove Donor"}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </>
  );
}
