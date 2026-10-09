import { useEffect, useState } from 'react';
import { api, inr } from '../api';
import TVWidget from '../components/TVWidget.jsx';

export default function Trade() {
  const [list, setList] = useState([]);
  const [sym, setSym] = useState('RELIANCE');
  const [q, setQ] = useState(null);
  const [qty, setQty] = useState('1');
  const [msg, setMsg] = useState(null);

  useEffect(() => { api('/instruments').then(setList); }, []);
  useEffect(() => {
    let on = true;
    const load = () => api('/quote/' + sym).then((d) => on && setQ(d)).catch(() => {});
    setQ(null); load();
    const t = setInterval(load, 10000);
    return () => { on = false; clearInterval(t); };
  }, [sym]);

  const inst = list.find((i) => i.symbol === sym);
  const est = q ? q.priceInr * Number(qty || 0) : 0;
  const markets = [...new Set(list.map((i) => i.market))];

  async function order(side) {
    setMsg(null);
    try {
      const r = await api('/trade', { method: 'POST', body: { symbol: sym, side, qty: Number(qty) } });
      setMsg({ ok: true, t: `${side} ${r.qty} ${sym} @ ${inr(r.price)} = ${inr(r.total)}` });
      window.dispatchEvent(new Event('wallet'));
    } catch (e) { setMsg({ ok: false, t: e.message }); }
  }

  return (
    <div className="trade-grid">
      <section className="card chart">
        <div className="row">
          <select value={sym} onChange={(e) => setSym(e.target.value)}>
            {markets.map((m) => (
              <optgroup key={m} label={m}>
                {list.filter((i) => i.market === m).map((i) => <option key={i.symbol} value={i.symbol}>{i.symbol} - {i.name}</option>)}
              </optgroup>
            ))}
          </select>
          <span className="muted">Use the chart's “Indicators” button to add RSI, MACD, Bollinger Bands and more.</span>
        </div>
        {inst && (
          <TVWidget type="advanced-chart" height={560}
            config={{ symbol: inst.tv, interval: 'D', theme: 'dark', style: '1', locale: 'en', timezone: 'Asia/Kolkata',
              allow_symbol_change: false, withdateranges: true, studies: ['RSI@tv-basicstudies'], support_host: 'https://www.tradingview.com' }} />
        )}
      </section>

      <aside className="side">
        <section className="card">
          <h3>{q?.name ?? sym}</h3>
          <div className="price">{q ? inr(q.priceInr) : '...'}</div>
          {q && q.currency === 'USD' && <div className="muted">Quote: {q.price.toFixed(4)} USD (converted to INR)</div>}
          {q && <div className={'tag ' + (q.source === 'LIVE' ? 'ok' : 'warn')}>{q.source === 'LIVE' ? 'Live price' : 'Simulated price'}</div>}
          <label>Quantity</label>
          <input type="number" min="0" step="any" value={qty} onChange={(e) => setQty(e.target.value)} />
          <div className="muted">Estimated: {inr(est)}</div>
          <div className="row">
            <button className="buy" onClick={() => order('BUY')}>Buy</button>
            <button className="sell" onClick={() => order('SELL')}>Sell</button>
          </div>
          {msg && <div className={'msg ' + (msg.ok ? 'good' : 'bad')}>{msg.t}</div>}
        </section>
        <section className="card">
          <h3>Technical summary</h3>
          {inst && <TVWidget type="technical-analysis" height={380}
            config={{ symbol: inst.tv, interval: '1D', colorTheme: 'dark', isTransparent: true, locale: 'en' }} />}
        </section>
      </aside>
    </div>
  );
}
