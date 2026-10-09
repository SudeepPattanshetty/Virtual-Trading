import { useEffect, useState } from 'react';
import { api, inr, pct } from '../api';

export default function Leaderboard() {
  const [rows, setRows] = useState([]);
  useEffect(() => { api('/leaderboard').then(setRows); }, []);
  return (
    <section className="card">
      <h2>Leaderboard</h2>
      <p className="muted">Ranked by return on virtual money added.</p>
      <div className="scroll"><table>
        <thead><tr><th>#</th><th>Trader</th><th>Net worth</th><th>Return</th></tr></thead>
        <tbody>{rows.map((r, i) => (
          <tr key={r.username}><td>{i + 1}</td><td><b>{r.username}</b></td><td>{inr(r.netWorth)}</td>
            <td className={r.returnPct >= 0 ? 'good-t' : 'bad-t'}>{pct(r.returnPct)}</td></tr>))}
        </tbody></table></div>
    </section>
  );
}
