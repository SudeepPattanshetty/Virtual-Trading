import { useEffect, useRef, useState } from 'react';
import { api } from '../api';

export default function Community({ me }) {
  const [msgs, setMsgs] = useState([]);
  const [text, setText] = useState('');
  const [err, setErr] = useState('');
  const box = useRef(null);
  const load = () => api('/chat').then(setMsgs).catch(() => {});
  useEffect(() => { load(); const t = setInterval(load, 3000); return () => clearInterval(t); }, []);
  useEffect(() => { box.current?.scrollTo(0, box.current.scrollHeight); }, [msgs.length]);

  async function send(e) {
    e.preventDefault(); setErr('');
    if (!text.trim()) return;
    try { await api('/chat', { method: 'POST', body: { message: text } }); setText(''); load(); }
    catch (x) { setErr(x.message); }
  }
  return (
    <section className="card chat">
      <h2>Community chat</h2>
      <p className="muted">Share ideas and learn from other traders. Be kind. This is not financial advice.</p>
      <div className="msgs" ref={box}>
        {msgs.map((m) => (
          <div key={m.ID} className={'bubble ' + (m.USERNAME === me ? 'mine' : '')}>
            <b>{m.USERNAME}</b> <small>{new Date(m.CREATED_AT).toLocaleTimeString()}</small>
            <div>{m.MESSAGE}</div>
          </div>))}
      </div>
      {err && <div className="msg bad">{err}</div>}
      <form className="row" onSubmit={send}>
        <input value={text} maxLength={500} placeholder="Write a message..." onChange={(e) => setText(e.target.value)} />
        <button className="primary">Send</button>
      </form>
    </section>
  );
}
