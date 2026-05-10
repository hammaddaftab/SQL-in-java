import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'

export default function AdminLoginPage() {
  const [password, setPassword] = useState('')
  const [error, setError]       = useState('')
  const [busy, setBusy]         = useState(false)
  const navigate = useNavigate()

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await api.adminLogin(password)
      navigate('/admin')
    } catch {
      setError('Incorrect password.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: 'var(--coffee-900)' }}>
      <div className="card" style={{ width: 360 }}>
        <div style={{ textAlign: 'center', marginBottom: 28 }}>
          <div style={{ fontFamily: 'var(--font-display)', fontSize: 26, color: 'var(--amber-500)', fontWeight: 600, marginBottom: 4 }}>
            Grind Admin
          </div>
          <p className="text-sm text-muted">Staff access only</p>
        </div>
        <form onSubmit={handleLogin} className="stack stack-sm">
          <div>
            <label className="field-label">Password</label>
            <input
              type="password" className="field"
              value={password} onChange={e => setPassword(e.target.value)}
              required autoFocus
            />
          </div>
          {error && <p className="inline-error">{error}</p>}
          <button className="btn btn-primary btn-full" type="submit" disabled={busy} style={{ marginTop: 4 }}>
            {busy ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </div>
    </div>
  )
}
