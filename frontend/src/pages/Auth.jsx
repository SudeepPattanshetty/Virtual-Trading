import { useState } from 'react';
import { api, token } from '../api';

export default function Auth({ onAuth }) {
  const [mode, setMode] = useState('login');
  const [f, setF] = useState({ username: '', password: '' });
  const [err, setErr] = useState('');
  async function submit(e) {
    e.preventDefault(); setErr('');
    try {
      const r = await api('/auth/' + mode, { method: 'POST', body: f });
      token.set(r.token); onAuth();
    } catch (x) { setErr(x.message); }
  }
  return (
    <div className="auth">
      <form className="card" onSubmit={submit}>
        <h1>VTrade</h1>
        <p className="muted">Practice trading with virtual money. No real funds involved.</p>
        <input placeholder="Username" value={f.username} onChange={(e) => setF({ ...f, username: e.target.value })} autoComplete="username" />
        <input placeholder="Password" type="password" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} />
        {err && <div className="msg bad">{err}</div>}
        <button className="primary">{mode === 'login' ? 'Log in' : 'Create account (get ₹10,00,000 virtual)'}</button>
        <button type="button" className="link" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
          {mode === 'login' ? 'New here? Register' : 'Have an account? Log in'}
        </button>
      </form>
    </div>
  );
}
