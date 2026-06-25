// src/pages/patient/PatientDashboard.jsx
import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import Navbar from "../../components/Navbar";

const CARDS = [
  {
    to: "/patient/doctors",
    icon: "🩺",
    title: "Find a Doctor",
    desc: "Browse and filter approved doctors by specialization or location.",
  },
  {
    to: "/patient/book",
    icon: "📅",
    title: "Book Appointment",
    desc: "Schedule a visit and get your serial number instantly.",
  },
  {
    to: "/patient/prescriptions",
    icon: "📄",
    title: "My Prescriptions",
    desc: "View all prescriptions issued to you, with medicine details.",
  },
  {
    to: "/patient/chat",
    icon: "💬",
    title: "Chat with Doctor",
    desc: "Send messages directly to your doctor.",
  },
  {
    to: "/patient/donors",
    icon: "🩸",
    title: "Blood Donors",
    desc: "Find compatible blood donors near you.",
  },
];

export default function PatientDashboard() {
  const { user } = useAuth();

  return (
    <>
      <Navbar />
      <div className="container" style={{ paddingTop: 40, paddingBottom: 60 }}>

        {/* Header */}
        <div style={{ marginBottom: 40 }}>
          <p style={{ fontSize: 13, color: "var(--text-muted)", marginBottom: 4 }}>Patient Portal</p>
          <h1 style={{ fontSize: 28, fontWeight: 600, color: "var(--text)", marginBottom: 8 }}>
            Welcome, {user?.name?.split(" ")[0] ?? "there"}
          </h1>
          <div className="accent-line" />
        </div>

        {/* Quick access cards */}
        <div style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))",
          gap: 20,
        }}>
          {CARDS.map((card) => (
            <Link
              key={card.to}
              to={card.to}
              style={{ textDecoration: "none" }}
            >
              <div className="card" style={{
                padding: "24px 20px",
                cursor: "pointer",
                transition: "border-color 0.15s, box-shadow 0.15s",
                borderColor: "var(--border)",
              }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.borderColor = "var(--accent)";
                  e.currentTarget.style.boxShadow = "0 2px 12px rgba(0,0,0,0.06)";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.borderColor = "var(--border)";
                  e.currentTarget.style.boxShadow = "none";
                }}
              >
                <div style={{ fontSize: 28, marginBottom: 12 }}>{card.icon}</div>
                <h3 style={{ fontSize: 15, fontWeight: 600, color: "var(--text)", marginBottom: 6 }}>
                  {card.title}
                </h3>
                <p style={{ fontSize: 13, color: "var(--text-muted)", lineHeight: 1.6 }}>
                  {card.desc}
                </p>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </>
  );
}