package com.vtrade;

import com.vtrade.MarketService.Quote;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class TradeService {
    private final JdbcTemplate jdbc;
    private final MarketService market;

    public TradeService(JdbcTemplate jdbc, MarketService market) {
        this.jdbc = jdbc;
        this.market = market;
    }

    private static ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }

    @Transactional
    public Map<String, Object> trade(String username, String symbol, String side, BigDecimal qty) {
        if (qty == null) throw bad("Quantity is required");
        qty = qty.setScale(4, RoundingMode.DOWN);
        if (qty.signum() <= 0) throw bad("Quantity must be greater than zero");
        if (!"BUY".equals(side) && !"SELL".equals(side)) throw bad("Side must be BUY or SELL");

        Quote q = market.quote(symbol);
        BigDecimal price = BigDecimal.valueOf(q.priceInr()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal total = price.multiply(qty).setScale(2, RoundingMode.HALF_UP);
        Long uid = jdbc.queryForObject("SELECT id FROM users WHERE username=?", Long.class, username);
        String sym = q.symbol();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT qty, avg_price FROM holdings WHERE user_id=? AND symbol=?", uid, sym);

        if (side.equals("BUY")) {
            int n = jdbc.update("UPDATE users SET cash=cash-? WHERE id=? AND cash>=?", total, uid, total);
            if (n == 0) throw bad("Not enough virtual cash. Add practice money from the Portfolio page.");
            if (rows.isEmpty()) {
                jdbc.update("INSERT INTO holdings(user_id,symbol,qty,avg_price) VALUES(?,?,?,?)", uid, sym, qty, price);
            } else {
                BigDecimal oq = (BigDecimal) rows.get(0).get("QTY");
                BigDecimal oa = (BigDecimal) rows.get(0).get("AVG_PRICE");
                BigDecimal nq = oq.add(qty);
                BigDecimal na = oq.multiply(oa).add(qty.multiply(price)).divide(nq, 4, RoundingMode.HALF_UP);
                jdbc.update("UPDATE holdings SET qty=?, avg_price=? WHERE user_id=? AND symbol=?", nq, na, uid, sym);
            }
        } else {
            if (rows.isEmpty() || ((BigDecimal) rows.get(0).get("QTY")).compareTo(qty) < 0)
                throw bad("You don't own enough " + sym + " to sell");
            BigDecimal left = ((BigDecimal) rows.get(0).get("QTY")).subtract(qty);
            jdbc.update("UPDATE users SET cash=cash+? WHERE id=?", total, uid);
            if (left.signum() == 0) jdbc.update("DELETE FROM holdings WHERE user_id=? AND symbol=?", uid, sym);
            else jdbc.update("UPDATE holdings SET qty=? WHERE user_id=? AND symbol=?", left, uid, sym);
        }
        jdbc.update("INSERT INTO trades(user_id,symbol,side,qty,price) VALUES(?,?,?,?,?)", uid, sym, side, qty, price);
        return Map.of("symbol", sym, "side", side, "qty", qty, "price", price, "total", total);
    }
}
