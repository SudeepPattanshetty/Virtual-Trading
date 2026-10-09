package com.vtrade;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    record Cred(String username, String password) {}
    record Amount(BigDecimal amount) {}
    record Order(String symbol, String side, BigDecimal qty) {}
    record Msg(String message) {}

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final SecurityConfig.JwtService jwt;
    private final MarketService market;
    private final TradeService trades;
    private final BigDecimal startCash;

    public ApiController(JdbcTemplate jdbc, PasswordEncoder encoder, SecurityConfig.JwtService jwt,
                         MarketService market, TradeService trades,
                         @Value("${app.start-cash}") BigDecimal startCash) {
        this.jdbc = jdbc; this.encoder = encoder; this.jwt = jwt;
        this.market = market; this.trades = trades; this.startCash = startCash;
    }

    private static ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }

    // ---------- auth ----------
    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody Cred c) {
        if (c.username() == null || !c.username().matches("[A-Za-z0-9_]{3,20}"))
            throw bad("Username must be 3-20 letters, numbers or underscore");
        if (c.password() == null || c.password().length() < 6) throw bad("Password must be at least 6 characters");
        try {
            jdbc.update("INSERT INTO users(username,password_hash,cash,deposited) VALUES(?,?,?,?)",
                    c.username(), encoder.encode(c.password()), startCash, startCash);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        return Map.of("token", jwt.create(c.username()), "username", c.username());
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Cred c) {
        List<String> h = c.username() == null ? List.of() :
                jdbc.queryForList("SELECT password_hash FROM users WHERE username=?", String.class, c.username());
        if (h.isEmpty() || c.password() == null || !encoder.matches(c.password(), h.get(0)))
            throw bad("Invalid username or password");
        return Map.of("token", jwt.create(c.username()), "username", c.username());
    }

    // ---------- market ----------
    @GetMapping("/instruments")
    public List<MarketService.Instrument> instruments() { return MarketService.CATALOG; }

    @GetMapping("/quote/{symbol}")
    public MarketService.Quote quote(@PathVariable String symbol) { return market.quote(symbol); }

    // ---------- wallet & trading ----------
    @GetMapping("/me")
    public Map<String, Object> me(Authentication a) {
        return jdbc.queryForMap("SELECT username, cash FROM users WHERE username=?", a.getName());
    }

    @PostMapping("/wallet/add")
    public Map<String, Object> addMoney(Authentication a, @RequestBody Amount amt) {
        if (amt.amount() == null || amt.amount().signum() <= 0 || amt.amount().compareTo(new BigDecimal("10000000")) > 0)
            throw bad("Enter an amount between 1 and 1,00,00,000 (virtual money)");
        jdbc.update("UPDATE users SET cash=cash+?, deposited=deposited+? WHERE username=?",
                amt.amount(), amt.amount(), a.getName());
        return me(a);
    }

    @PostMapping("/trade")
    public Map<String, Object> trade(Authentication a, @RequestBody Order o) {
        String side = o.side() == null ? "" : o.side().toUpperCase();
        return trades.trade(a.getName(), o.symbol(), side, o.qty());
    }

    @GetMapping("/portfolio")
    public Map<String, Object> portfolio(Authentication a) {
        Map<String, Object> u = jdbc.queryForMap("SELECT id, cash, deposited FROM users WHERE username=?", a.getName());
        long uid = ((Number) u.get("ID")).longValue();
        List<Map<String, Object>> out = new ArrayList<>();
        double value = 0;
        for (Map<String, Object> h : jdbc.queryForList("SELECT symbol, qty, avg_price FROM holdings WHERE user_id=?", uid)) {
            String sym = (String) h.get("SYMBOL");
            double qty = ((BigDecimal) h.get("QTY")).doubleValue();
            double avg = ((BigDecimal) h.get("AVG_PRICE")).doubleValue();
            double price = market.quote(sym).priceInr();
            value += qty * price;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("symbol", sym); row.put("name", market.find(sym).name());
            row.put("qty", qty); row.put("avgPrice", avg); row.put("price", price);
            row.put("value", qty * price); row.put("pnl", (price - avg) * qty);
            out.add(row);
        }
        double cash = ((BigDecimal) u.get("CASH")).doubleValue();
        double dep = ((BigDecimal) u.get("DEPOSITED")).doubleValue();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("cash", cash); r.put("holdingsValue", value); r.put("netWorth", cash + value);
        r.put("deposited", dep); r.put("totalPnl", cash + value - dep);
        r.put("holdings", out);
        return r;
    }

    @GetMapping("/trades")
    public List<Map<String, Object>> history(Authentication a) {
        return jdbc.queryForList("""
            SELECT t.id, t.symbol, t.side, t.qty, t.price, t.created_at FROM trades t
            JOIN users u ON u.id=t.user_id WHERE u.username=? ORDER BY t.id DESC LIMIT 100""", a.getName());
    }

    // ---------- community ----------
    @GetMapping("/leaderboard")
    public List<Map<String, Object>> leaderboard() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> u : jdbc.queryForList("SELECT id, username, cash, deposited FROM users")) {
            long uid = ((Number) u.get("ID")).longValue();
            double nw = ((BigDecimal) u.get("CASH")).doubleValue();
            for (Map<String, Object> h : jdbc.queryForList("SELECT symbol, qty FROM holdings WHERE user_id=?", uid))
                nw += ((BigDecimal) h.get("QTY")).doubleValue() * market.quote((String) h.get("SYMBOL")).priceInr();
            double dep = ((BigDecimal) u.get("DEPOSITED")).doubleValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("username", u.get("USERNAME")); row.put("netWorth", nw);
            row.put("returnPct", dep == 0 ? 0 : (nw - dep) / dep * 100);
            out.add(row);
        }
        out.sort((x, y) -> Double.compare((double) y.get("returnPct"), (double) x.get("returnPct")));
        return out.size() > 50 ? out.subList(0, 50) : out;
    }

    @GetMapping("/chat")
    public List<Map<String, Object>> chat() {
        return jdbc.queryForList("""
            SELECT * FROM (SELECT id, username, message, created_at FROM chat ORDER BY id DESC LIMIT 60) t ORDER BY id""");
    }

    @PostMapping("/chat")
    public Map<String, Object> say(Authentication a, @RequestBody Msg m) {
        String text = m.message() == null ? "" : m.message().trim();
        if (text.isEmpty() || text.length() > 500) throw bad("Message must be 1-500 characters");
        jdbc.update("INSERT INTO chat(username,message) VALUES(?,?)", a.getName(), text);
        return Map.of("ok", true);
    }
}
