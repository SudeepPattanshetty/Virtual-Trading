package com.vtrade;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Price source for virtual orders. Tries Yahoo Finance (unofficial, no key) and falls back
 * to a simulated random walk if it is unreachable. Swap fetchLive() for any paid API later.
 * The TradingView widgets show their own live data; this service prices YOUR virtual trades.
 */
@Service
public class MarketService {

    public record Instrument(String symbol, String name, String market, String currency,
                             String yahoo, String tv, double base) {}
    public record Quote(String symbol, String name, String market, double price, String currency,
                        double priceInr, String source) {}
    private record Cached(double price, String source, long ts) {}

    public static final List<Instrument> CATALOG = List.of(
        new Instrument("RELIANCE", "Reliance Industries", "India", "INR", "RELIANCE.NS", "NSE:RELIANCE", 1400),
        new Instrument("TCS", "Tata Consultancy Services", "India", "INR", "TCS.NS", "NSE:TCS", 3300),
        new Instrument("INFY", "Infosys", "India", "INR", "INFY.NS", "NSE:INFY", 1500),
        new Instrument("HDFCBANK", "HDFC Bank", "India", "INR", "HDFCBANK.NS", "NSE:HDFCBANK", 1000),
        new Instrument("ICICIBANK", "ICICI Bank", "India", "INR", "ICICIBANK.NS", "NSE:ICICIBANK", 1300),
        new Instrument("SBIN", "State Bank of India", "India", "INR", "SBIN.NS", "NSE:SBIN", 800),
        new Instrument("ITC", "ITC Ltd", "India", "INR", "ITC.NS", "NSE:ITC", 420),
        new Instrument("BHARTIARTL", "Bharti Airtel", "India", "INR", "BHARTIARTL.NS", "NSE:BHARTIARTL", 1900),
        new Instrument("AAPL", "Apple", "US", "USD", "AAPL", "NASDAQ:AAPL", 230),
        new Instrument("MSFT", "Microsoft", "US", "USD", "MSFT", "NASDAQ:MSFT", 450),
        new Instrument("GOOGL", "Alphabet", "US", "USD", "GOOGL", "NASDAQ:GOOGL", 200),
        new Instrument("AMZN", "Amazon", "US", "USD", "AMZN", "NASDAQ:AMZN", 210),
        new Instrument("TSLA", "Tesla", "US", "USD", "TSLA", "NASDAQ:TSLA", 300),
        new Instrument("NVDA", "NVIDIA", "US", "USD", "NVDA", "NASDAQ:NVDA", 180),
        new Instrument("EURUSD", "Euro / US Dollar", "Forex", "USD", "EURUSD=X", "FX:EURUSD", 1.08),
        new Instrument("GBPUSD", "British Pound / US Dollar", "Forex", "USD", "GBPUSD=X", "FX:GBPUSD", 1.30),
        new Instrument("USDINR", "US Dollar / Indian Rupee", "Forex", "INR", "USDINR=X", "FX_IDC:USDINR", 85),
        new Instrument("EURINR", "Euro / Indian Rupee", "Forex", "INR", "EURINR=X", "FX_IDC:EURINR", 92)
    );

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final Map<String, Double> simState = new ConcurrentHashMap<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public Instrument find(String symbol) {
        return CATALOG.stream().filter(i -> i.symbol().equalsIgnoreCase(symbol)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown symbol: " + symbol));
    }

    public Quote quote(String symbol) {
        Instrument i = find(symbol);
        Cached c = raw(i);
        double fx = i.currency().equals("USD") ? raw(find("USDINR")).price() : 1.0;
        return new Quote(i.symbol(), i.name(), i.market(), c.price(), i.currency(), c.price() * fx, c.source());
    }

    private Cached raw(Instrument i) {
        long now = System.currentTimeMillis();
        Cached c = cache.get(i.symbol());
        if (c != null && now - c.ts() < 15_000) return c;
        Double live = fetchLive(i);
        Cached n;
        if (live != null) {
            simState.put(i.symbol(), live);
            n = new Cached(live, "LIVE", now);
        } else {
            double last = simState.getOrDefault(i.symbol(), i.base());
            double next = last * (1 + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.004);
            simState.put(i.symbol(), next);
            n = new Cached(next, "SIMULATED", now);
        }
        cache.put(i.symbol(), n);
        return n;
    }

    private Double fetchLive(Instrument i) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(
                    "https://query1.finance.yahoo.com/v8/finance/chart/" + i.yahoo() + "?interval=1m&range=1d"))
                    .timeout(Duration.ofSeconds(4)).header("User-Agent", "Mozilla/5.0").build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return null;
            double p = mapper.readTree(res.body()).at("/chart/result/0/meta/regularMarketPrice").asDouble(0);
            return p > 0 ? p : null;
        } catch (Exception e) {
            return null;
        }
    }
}
