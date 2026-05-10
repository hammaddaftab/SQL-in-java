import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';

export default function AdminLoginPage() {
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.adminLogin(password);
      navigate('/admin');
    } catch (err: any) {
      setError(err.message || 'Login failed');
    }
  };

  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: 'var(--coffee-900)' }}>
      <div className="card" style={{ width: 360, textAlign: 'center' }}>
        <div style={{ fontFamily: 'var(--font-display)', fontSize: 28, color: 'var(--amber-600)', marginBottom: 32, fontWeight: 600 }}>
          Grind Admin
        </div>
        <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <input 
            type="password" 
            className="input-field" 
            placeholder="Admin Password" 
            value={password} 
            onChange={e => setPassword(e.target.value)} 
            required 
            autoFocus
          />
          {error && <div style={{ color: 'var(--danger)', fontSize: 14 }}>{error}</div>}
          <button className="btn-primary" type="submit" style={{ width: '100%' }}>Login</button>
        </form>
      </div>
    </div>
  );
}
