import { useEffect, useState } from 'react';
import { NavLink, Route, Routes } from 'react-router-dom';
import { api, token, inr } from './api';
import Auth from './pages/Auth.jsx';
import Trade from './pages/Trade.jsx';
import Portfolio from './pages/Portfolio.jsx';
import Learn from './pages/Learn.jsx';
import Community from './pages/Community.jsx';
import Leaderboard from './pages/Leaderboard.jsx';

export default function App() {
  const [authed, setAuthed] = useState(!!token.get());
  useEffect(() => {
    const f = () => setAuthed(false);
    window.addEventListener('logout', f);
    return () => window.removeEventListener('logout', f);
  }, []);
  if (!authed) return <Auth onAuth={() => setAuthed(true)} />;
  return <Shell onLogout={() => { token.clear(); setAuthed(false); }} />;
}

function Shell({ onLogout }) {
  const [me, setMe] = useState(null);
  useEffect(() => {
    const load = () => api('/me').then(setMe).catch(() => {});
    load();
    const t = setInterval(load, 10000);
    window.addEventListener('wallet', load);
    return () => { clearInterval(t); window.removeEventListener('wallet', load); };
  }, []);
  return (
    <div className="shell">
      <header>
        <div className="brand">VTrade <small>practice only</small></div>
        <nav>
          <NavLink to="/" end>Trade</NavLink>
          <NavLink to="/portfolio">Portfolio</NavLink>
          <NavLink to="/learn">Learn</NavLink>
          <NavLink to="/community">Community</NavLink>
          <NavLink to="/leaderboard">Leaderboard</NavLink>
        </nav>
        <div className="who">
          <span className="cash">{me ? inr(me.CASH ?? me.cash) : '...'}</span>
          <button className="ghost" onClick={onLogout}>Logout{me ? ` (${me.USERNAME ?? me.username})` : ''}</button>
        </div>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Trade />} />
          <Route path="/portfolio" element={<Portfolio />} />
          <Route path="/learn" element={<Learn />} />
          <Route path="/community" element={<Community me={me?.USERNAME ?? me?.username} />} />
          <Route path="/leaderboard" element={<Leaderboard />} />
        </Routes>
      </main>
    </div>
  );
}
