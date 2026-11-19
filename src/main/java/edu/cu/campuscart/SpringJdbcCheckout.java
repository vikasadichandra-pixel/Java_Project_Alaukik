package edu.cu.campuscart;

import java.sql.SQLException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SpringJdbcCheckout {
    private final Checkout checkout;
    private final CheckoutStore store;

    SpringJdbcCheckout(Checkout checkout, JdbcTemplate jdbc) {
        this.checkout = checkout;
        this.store = new SpringCheckoutStore(jdbc);
    }

    @Transactional(rollbackFor = SQLException.class)
    public Receipt place(CheckoutRequest request) throws SQLException {
        return checkout.place(request, store);
    }

    private record SpringCheckoutStore(JdbcTemplate jdbc) implements CheckoutStore {
        public boolean customerExists(long id) {
            return jdbc.queryForObject(CUSTOMER_EXISTS, Integer.class, id) > 0;
        }

        public Product findProduct(int id) {
            return jdbc.query(FIND_PRODUCT, ShopRepository.PRODUCT_MAPPER, id)
                    .stream().findFirst().orElse(null);
        }

        public boolean decreaseStock(int id, int quantity) {
            return jdbc.update(DECREASE_STOCK, quantity, id, quantity) == 1;
        }

        public void save(long customerId, Receipt receipt) {
            jdbc.update(INSERT_ORDER, receipt.id(), customerId,
                    receipt.subtotal(), receipt.discount(), receipt.total());
            for (OrderItem item : receipt.items()) {
                jdbc.update(INSERT_ITEM, receipt.id(), item.productId(),
                        item.name(), item.quantity(), item.unitPrice());
            }
        }
    }
}

