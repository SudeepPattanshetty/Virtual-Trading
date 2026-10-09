import { useState } from 'react';
import TVWidget from '../components/TVWidget.jsx';

const TOPICS = [
  { n: 'Moving Averages (SMA / EMA)', d: 'Smooth price to show trend direction. EMA reacts faster than SMA.', u: 'Price above a rising 50/200 MA = uptrend. A fast MA crossing above a slow MA (golden cross) is a bullish signal; the reverse (death cross) is bearish.' },
  { n: 'RSI (Relative Strength Index)', d: 'Momentum oscillator from 0 to 100 measuring speed of price moves.', u: 'Above 70 = possibly overbought, below 30 = possibly oversold. In strong trends RSI can stay extreme; look for divergence with price.' },
  { n: 'MACD', d: 'Difference between two EMAs (12 and 26) with a 9-period signal line and histogram.', u: 'MACD crossing above the signal line is bullish, below is bearish. Histogram shrinking warns that momentum is fading.' },
  { n: 'Bollinger Bands', d: 'A moving average with bands two standard deviations above and below.', u: 'Narrow bands (squeeze) often precede big moves. Price riding the upper band shows strength; a close back inside can signal a pause.' },
  { n: 'Stochastic Oscillator', d: 'Compares the close to the recent high-low range.', u: 'Above 80 overbought, below 20 oversold. %K crossing %D gives entry hints, best used with a trend filter.' },
  { n: 'ATR (Average True Range)', d: 'Measures volatility, not direction.', u: 'Use it to size stop-losses, e.g. 2 x ATR below your entry, so normal noise does not stop you out.' },
  { n: 'VWAP', d: 'Volume-weighted average price for the session.', u: 'Intraday traders treat price above VWAP as bullish and below as bearish. Institutions use it as a fair-price benchmark.' },
  { n: 'Volume', d: 'Number of shares or contracts traded.', u: 'Breakouts on high volume are more reliable. Rising price on falling volume suggests weak conviction.' },
  { n: 'Fibonacci Retracement', d: 'Horizontal levels (38.2%, 50%, 61.8%) drawn between a swing high and low.', u: 'Pullbacks in a trend often pause near these levels, which traders use as potential entry zones.' },
  { n: 'Supertrend', d: 'ATR-based trailing line that flips above or below price.', u: 'Green line below price = uptrend, red above = downtrend. Popular for simple trend-following in Indian markets.' },
  { n: 'Support & Resistance', d: 'Price zones where buyers (support) or sellers (resistance) have repeatedly stepped in.', u: 'A break and retest of a level often becomes the new support/resistance. Always confirm with volume.' },
  { n: 'Candlestick Patterns', d: 'Each candle shows open, high, low and close for a period.', u: 'Learn doji, hammer, engulfing and morning/evening star. Patterns matter most at support/resistance levels.' },
];

export default function Learn() {
  const [open, setOpen] = useState(0);
  return (
    <>
      <section className="card">
        <h2>Learn technical analysis</h2>
        <p className="muted">Read the idea, then add the indicator on the Trade page chart (“Indicators” button) and practise on virtual money. Past patterns never guarantee future results.</p>
        <div className="topics">
          {TOPICS.map((t, i) => (
            <div key={t.n} className={'topic ' + (open === i ? 'open' : '')} onClick={() => setOpen(open === i ? -1 : i)}>
              <h4>{t.n}</h4>
              <p>{t.d}</p>
              {open === i && <p className="use"><b>How to use:</b> {t.u}</p>}
            </div>
          ))}
        </div>
      </section>
      <section className="card">
        <h3>Market overview</h3>
        <TVWidget type="market-overview" height={500}
          config={{ colorTheme: 'dark', dateRange: '12M', showChart: true, locale: 'en', isTransparent: true,
            tabs: [
              { title: 'Indices', symbols: [{ s: 'FOREXCOM:SPXUSD', d: 'S&P 500' }, { s: 'NSE:NIFTY', d: 'Nifty 50' }, { s: 'BSE:SENSEX', d: 'Sensex' }] },
              { title: 'Forex', symbols: [{ s: 'FX:EURUSD' }, { s: 'FX:GBPUSD' }, { s: 'FX_IDC:USDINR' }] },
            ] }} />
      </section>
    </>
  );
}
