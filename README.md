# VTrade - Virtual Trading Practice App

Spring Boot (Java) backend + React (Vite) frontend. TradingView free widgets for charts/indicators.
Virtual money only. Markets: Indian stocks (NSE), US stocks, Forex.

## Run (two terminals)
Backend (http://localhost:8080):
    cd backend
    mvn spring-boot:run

Frontend (http://localhost:5173):
    cd frontend
    npm install
    npm run dev

Open http://localhost:5173, register (you get Rs 10,00,000 virtual cash), and trade.

## Notes
- DB: H2 file at backend/data/vtrade (no Docker needed). Postgres can be swapped in later.
- Prices for your virtual orders: Yahoo Finance (unofficial) with automatic simulated fallback.
  The UI badge shows "Live price" or "Simulated price".
- Set JWT_SECRET env var (32+ chars) before deploying anywhere.
- Java is compiled to release 21 and runs on JDK 26.
