package edu.cu.campuscart;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
class ShopRepository {
    static final RowMapper<Product> PRODUCT_MAPPER = (row, index) -> new Product(
            row.getInt("id"), row.getString("name"), row.getString("category"),
            row.getBigDecimal("price"), row.getInt("stock"));

    private final JdbcTemplate jdbc;

    ShopRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<Product> search(String term) {
        String query = term.strip().toLowerCase(Locale.ROOT);
        return jdbc.query("""
                SELECT id, name, category, price, stock FROM products
                WHERE LOCATE(?, LOWER(name)) > 0 OR LOCATE(?, LOWER(category)) > 0
                ORDER BY id
                """, PRODUCT_MAPPER, query, query);
    }

    List<Customer> customers() {
        return jdbc.query("SELECT id, name, email FROM customers ORDER BY id",
                (row, index) -> new Customer(row.getLong("id"), row.getString("name"),
                        row.getString("email")));
    }

    Customer register(Registration registration) {
        String name = registration.name() == null ? "" : registration.name().strip();
        String email = registration.email() == null ? ""
                : registration.email().strip().toLowerCase(Locale.ROOT);
        if (name.isEmpty() || name.length() > 100 || email.length() > 180
                || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new ShopException(400, "Enter a name and a valid email address.");
        }
        try {
            jdbc.update("INSERT INTO customers(name, email) VALUES (?, ?)", name, email);
        } catch (DuplicateKeyException failure) {
            throw new ShopException(409, "That email is already registered.");
        }
        Long id = jdbc.queryForObject("SELECT id FROM customers WHERE email = ?", Long.class, email);
        return new Customer(id, name, email);
    }

    List<OrderSummary> orders(long customerId) {
        return jdbc.query("""
                SELECT id, placed_at, subtotal, discount, total FROM shop_orders
                WHERE customer_id = ? ORDER BY placed_at DESC, id
                """, (row, index) -> new OrderSummary(row.getString("id"),
                row.getObject("placed_at", OffsetDateTime.class), row.getBigDecimal("subtotal"),
                row.getBigDecimal("discount"), row.getBigDecimal("total")), customerId);
    }
}

