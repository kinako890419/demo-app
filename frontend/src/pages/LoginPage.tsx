import { SubmitEvent, useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { tracer } from '../otel/tracing.ts'
import {Span, SpanStatusCode} from '@opentelemetry/api'
import apiClient from '../api/client'
import type { LoginResponse } from '../types'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleLogin = async (e: SubmitEvent) => {
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
      async (span: Span) => {
        try {
          const { data } = await apiClient.post<LoginResponse>('/api/auth/login', {
            user_mail: email,
            user_password: password,
          })

          span.setAttributes({
            'user.id': String(data.user?.user_id ?? ''),
          })
          span.setStatus({ code: SpanStatusCode.OK })

          localStorage.setItem('token', data.token)
          localStorage.setItem('user', JSON.stringify(data.user))
          localStorage.setItem('loginTraceId', span.spanContext().traceId)
          navigate('/dashboard')
        } catch (err) {
          // Record the error into the span so it appears in the trace viewer
          const message = err instanceof Error ? err.message : String(err)
          span.setStatus({ code: SpanStatusCode.ERROR, message })
          span.recordException(err instanceof Error ? err : new Error(message))
          setError(message)
        } finally {
          span.end() // end span
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
