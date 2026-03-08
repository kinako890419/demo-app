import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { tracer } from '../otel/tracing.js'
import { SpanStatusCode } from '@opentelemetry/api'

const API_BASE = 'http://localhost:8181'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleLogin = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    // Manual instrumentation
    await tracer.startActiveSpan(
      'user.login',
      {
        attributes: {
          'user.email': email,
          'auth.method': 'password',
          'app.page': 'login',
        },
      },
      async (span) => {
        try {
          const response = await fetch(`${API_BASE}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ user_mail: email, user_password: password }),
          })

          const data = await response.json()

          if (!response.ok) {
            throw new Error(data.message || `Login failed (${response.status})`)
          }

          span.setAttributes({
            'user.id': String(data.user?.user_id ?? ''),
            'auth.success': true,
          })
          span.setStatus({ code: SpanStatusCode.OK })

          localStorage.setItem('token', data.token)
          localStorage.setItem('user', JSON.stringify(data.user))
          localStorage.setItem('loginTraceId', span.spanContext().traceId)
          navigate('/dashboard')
        } catch (err) {
          // Record the error into the span so it appears in the trace viewer
          span.setStatus({ code: SpanStatusCode.ERROR, message: err.message })
          span.recordException(err)
          setError(err.message)
        } finally {
          span.end()   // must always be called or the span is never exported
          setLoading(false)
        }
      }
    )
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h1>Sign In</h1>
        <form onSubmit={handleLogin}>
          <div className="form-group">
            <label>Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          {error && <p className="error">{error}</p>}
          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>
        <p className="auth-link">
          <Link to="/register">Register</Link>
        </p>
      </div>
    </div>
  )
}
