import { useEffect, useState } from 'react';
import { api, inr, pct } from '../api';

export default function Portfolio() {
  const [p, setP] = useState(null);
  const [tx, setTx] = useState([]);
  const [amt, setAmt] = useState('100000');
  const [msg, setMsg] = useState(null);
  const load = () => { api('/portfolio').then(setP); api('/trades').then(setTx); };
  useEffect(() => { load(); }, []);

  async function add() {
    setMsg(null);
    try { await api('/wallet/add', { method: 'POST', body: { amount: Number(amt) } });
      setMsg({ ok: true, t: `Added ${inr(Number(amt))} of practice money` });
      window.dispatchEvent(new Event('wallet')); load();
    } catch (e) { setMsg({ ok: false, t: e.message }); }
  }
  if (!p) return <p className="muted">Loading...</p>;
  const cls = (n) => (n >= 0 ? 'good-t' : 'bad-t');
  return (
    <>
      <div className="stats">
        <div className="card"><span className="muted">Net worth</span><b>{inr(p.netWorth)}</b></div>
        <div className="card"><span className="muted">Cash</span><b>{inr(p.cash)}</b></div>
        <div className="card"><span className="muted">Invested value</span><b>{inr(p.holdingsValue)}</b></div>
        <div className="card"><span className="muted">Total P&amp;L</span>
          <b className={cls(p.totalPnl)}>{inr(p.totalPnl)} ({pct(p.deposited ? (p.totalPnl / p.deposited) * 100 : 0)})</b></div>
      </div>

      <section className="card">
        <h3>Add practice money</h3>
        <div className="row">
          {[10000, 50000, 100000, 500000].map((v) => <button key={v} className="ghost" onClick={() => setAmt(String(v))}>{inr(v)}</button>)}
          <input type="number" value={amt} onChange={(e) => setAmt(e.target.value)} />
          <button className="primary" onClick={add}>Add virtual money</button>
        </div>
        {msg && <div className={'msg ' + (msg.ok ? 'good' : 'bad')}>{msg.t}</div>}
        <p className="muted">This is not real money. P&amp;L is measured against all virtual money you've added.</p>
      </section>

      <section className="card">
        <h3>Holdings</h3>
        <div className="scroll"><table>
          <thead><tr><th>Symbol</th><th>Qty</th><th>Avg price</th><th>LTP</th><th>Value</th><th>P&amp;L</th></tr></thead>
          <tbody>{p.holdings.length === 0 && <tr><td colSpan="6" className="muted">No holdings yet. Place a trade!</td></tr>}
            {p.holdings.map((h) => (
              <tr key={h.symbol}><td><b>{h.symbol}</b></td><td>{h.qty}</td><td>{inr(h.avgPrice)}</td><td>{inr(h.price)}</td>
                <td>{inr(h.value)}</td><td className={cls(h.pnl)}>{inr(h.pnl)}</td></tr>))}
          </tbody></table></div>
      </section>

      <section className="card">
        <h3>Trade history</h3>
        <div className="scroll"><table>
          <thead><tr><th>Time</th><th>Symbol</th><th>Side</th><th>Qty</th><th>Price</th></tr></thead>
          <tbody>{tx.map((t) => (
            <tr key={t.ID}><td>{new Date(t.CREATED_AT).toLocaleString()}</td><td>{t.SYMBOL}</td>
              <td className={t.SIDE === 'BUY' ? 'good-t' : 'bad-t'}>{t.SIDE}</td><td>{t.QTY}</td><td>{inr(t.PRICE)}</td></tr>))}
          </tbody></table></div>
      </section>
    </>
  );
}
