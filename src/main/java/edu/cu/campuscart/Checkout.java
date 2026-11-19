package edu.cu.campuscart;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Component;

interface CheckoutStore {
    String CUSTOMER_EXISTS = "SELECT COUNT(*) FROM customers WHERE id = ?";
    String FIND_PRODUCT = "SELECT id, name, category, price, stock FROM products WHERE id = ?";
    String DECREASE_STOCK = "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?";
    String INSERT_ORDER = """
        INSERT INTO shop_orders(id, customer_id, subtotal, discount, total) VALUES (?, ?, ?, ?, ?)
        """;
    String INSERT_ITEM = """
        INSERT INTO order_items(order_id, product_id, product_name, quantity, unit_price)
        VALUES (?, ?, ?, ?, ?)
        """;

    boolean customerExists(long id) throws SQLException;
    Product findProduct(int id) throws SQLException;
    boolean decreaseStock(int id, int quantity) throws SQLException;
    void save(long customerId, Receipt receipt) throws SQLException;
}

@Component
class Checkout {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");

    Receipt place(CheckoutRequest request, CheckoutStore store) throws SQLException {
        if (request.items() == null || request.items().isEmpty()) {
            throw new ShopException(400, "Add at least one product to the cart.");
        }
        if (request.items().size() > 100) {
            throw new ShopException(400, "A cart can contain at most 100 lines.");
        }
        if (!store.customerExists(request.customerId())) {
            throw new ShopException(404, "Select a registered customer.");
        }
        String coupon = request.coupon() == null ? "" : request.coupon().strip();
        if (!coupon.isEmpty() && !coupon.equalsIgnoreCase("CAMPUS10")) {
            throw new ShopException(400, "Unknown coupon. Use CAMPUS10 for 10% off.");
        }

        // Use the same product order in all checkouts to reduce lock conflicts.
        var quantities = new TreeMap<Integer, Integer>();
        for (CartItem item : request.items()) {
            if (item == null || item.productId() <= 0 || item.quantity() < 1 || item.quantity() > 99) {
                throw new ShopException(400, "Product IDs must be positive and quantities must be 1 to 99.");
            }
            int quantity = quantities.merge(item.productId(), item.quantity(), Integer::sum);
            if (quantity > 99) {
                throw new ShopException(400, "A product can have at most 99 units per order.");
            }
        }

        List<OrderItem> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2);
        for (var entry : quantities.entrySet()) {
            int quantity = entry.getValue();
            Product product = store.findProduct(entry.getKey());
            if (product == null) {
                throw new ShopException(404, "Product " + entry.getKey() + " was not found.");
            }
            if (!store.decreaseStock(product.id(), quantity)) {
                throw new ShopException(409, "Insufficient stock for " + product.name() + ".");
            }
            lines.add(new OrderItem(product.id(), product.name(), quantity, product.price()));
            subtotal = subtotal.add(product.price().multiply(BigDecimal.valueOf(quantity)));
        }

        BigDecimal discount = coupon.isEmpty() ? BigDecimal.ZERO.setScale(2)
                : subtotal.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
        Receipt receipt = new Receipt(UUID.randomUUID().toString(), subtotal, discount,
                subtotal.subtract(discount), List.copyOf(lines));
        store.save(request.customerId(), receipt);
        return receipt;
    }
}

