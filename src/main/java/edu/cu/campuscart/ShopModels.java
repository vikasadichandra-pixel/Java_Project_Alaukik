package edu.cu.campuscart;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

record Product(int id, String name, String category, BigDecimal price, int stock) {}
record Customer(long id, String name, String email) {}
record Registration(String name, String email) {}
record CartItem(int productId, int quantity) {}
record CheckoutRequest(long customerId, List<CartItem> items, String coupon) {}
record OrderItem(int productId, String name, int quantity, BigDecimal unitPrice) {}
record Receipt(String id, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
               List<OrderItem> items) {}
record OrderSummary(String id, OffsetDateTime placedAt, BigDecimal subtotal,
                    BigDecimal discount, BigDecimal total) {}

class ShopException extends RuntimeException {
    private final int status;

    ShopException(int status, String message) {
        super(message);
        this.status = status;
    }

    int status() {
        return status;
    }
}

