import { useEffect, useRef } from 'react';

// Embeds a free TradingView widget. type: 'advanced-chart' | 'technical-analysis' | 'market-overview' ...
export default function TVWidget({ type, config, height = 500 }) {
  const ref = useRef(null);
  const key = JSON.stringify(config);
  useEffect(() => {
    const el = ref.current;
    el.innerHTML = '';
    const inner = document.createElement('div');
    inner.className = 'tradingview-widget-container__widget';
    inner.style.height = '100%';
    const s = document.createElement('script');
    s.src = `https://s3.tradingview.com/external-embedding/embed-widget-${type}.js`;
    s.async = true;
    s.innerHTML = JSON.stringify({ autosize: true, ...config });
    el.appendChild(inner);
    el.appendChild(s);
    return () => { el.innerHTML = ''; };
  }, [type, key]);
  return <div className="tradingview-widget-container" ref={ref} style={{ height }} />;
}
