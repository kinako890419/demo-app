import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { User } from '../types'

export default function DashboardPage() {
  const [user, setUser] = useState<User | null>(null)
  const [traceId, setTraceId] = useState<string | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    const stored = localStorage.getItem('user')
    if (stored) setUser(JSON.parse(stored) as User)
    // The login trace ID spans both the frontend (user.login span) and the backend (HTTP + DB spans)
    setTraceId(localStorage.getItem('loginTraceId'))
  }, [])

  const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('loginTraceId')
    navigate('/login')
  }

  const formatDate = (d: string | undefined) =>
    d ? new Date(d).toLocaleDateString(undefined, { dateStyle: 'medium' }) : '—'

  return (
    <div className="dashboard-container">
      <div className="dashboard-card">
        <div className="dashboard-header">
          <h1>Dashboard</h1>
          <button onClick={handleLogout} className="btn-secondary">
            Logout
          </button>
        </div>

        {user && (
          <section className="section">
            <h2 className="section-title">User Profile</h2>
            <div className="profile-grid">
              <div className="profile-item">
                <span className="label">Name</span>
                <span className="value">{user.user_name}</span>
              </div>
              <div className="profile-item">
                <span className="label">Email</span>
                <span className="value">{user.user_email}</span>
              </div>
              <div className="profile-item">
                <span className="label">User ID</span>
                <span className="value">#{user.user_id}</span>
              </div>
              <div className="profile-item">
                <span className="label">Joined</span>
                <span className="value">{formatDate(user.created_at)}</span>
              </div>
            </div>
          </section>
        )}

        <section className="section">
          <h2 className="title">OpenTelemetry Trace</h2>

            {traceId && (
              <div className="trace-row trace-id-row">
                <span className="label">Login Trace ID</span>
                <code className="trace-id">{traceId}</code>
              </div>
            )}

          <div className="observability-links">
            <p className="label" style={{ marginBottom: '0.5rem' }}>
              View traces in:
            </p>
            <div className="link-row">
              <a
                href="http://localhost:3000"
                target="_blank"
                rel="noreferrer"
                className="obs-link"
              >
                Grafana
              </a>
              <a
                href="http://localhost:5601"
                target="_blank"
                rel="noreferrer"
                className="obs-link"
              >
                OpenSearch Dashboards
              </a>
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}
