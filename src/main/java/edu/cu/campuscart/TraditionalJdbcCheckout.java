package edu.cu.campuscart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.springframework.stereotype.Service;

@Service
class TraditionalJdbcCheckout {
    private final Checkout checkout;
    private final DataSource dataSource;

    TraditionalJdbcCheckout(Checkout checkout, DataSource dataSource) {
        this.checkout = checkout;
        this.dataSource = dataSource;
    }

    Receipt place(CheckoutRequest request) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Receipt receipt = checkout.place(request, new TraditionalStore(connection));
                connection.commit();
                return receipt;
            } catch (SQLException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        }
    }

    private record TraditionalStore(Connection connection) implements CheckoutStore {
        public boolean customerExists(long id) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(CUSTOMER_EXISTS)) {
                statement.setLong(1, id);
                try (var results = statement.executeQuery()) {
                    results.next();
                    return results.getInt(1) > 0;
                }
            }
        }

        public Product findProduct(int id) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(FIND_PRODUCT)) {
                statement.setInt(1, id);
                try (var results = statement.executeQuery()) {
                    return results.next() ? new Product(results.getInt("id"), results.getString("name"),
                            results.getString("category"), results.getBigDecimal("price"),
                            results.getInt("stock")) : null;
                }
            }
        }

        public boolean decreaseStock(int id, int quantity) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(DECREASE_STOCK)) {
                statement.setInt(1, quantity);
                statement.setInt(2, id);
                statement.setInt(3, quantity);
                return statement.executeUpdate() == 1;
            }
        }

        public void save(long customerId, Receipt receipt) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_ORDER)) {
                statement.setString(1, receipt.id());
                statement.setLong(2, customerId);
                statement.setBigDecimal(3, receipt.subtotal());
                statement.setBigDecimal(4, receipt.discount());
                statement.setBigDecimal(5, receipt.total());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(INSERT_ITEM)) {
                for (OrderItem item : receipt.items()) {
                    statement.setString(1, receipt.id());
                    statement.setInt(2, item.productId());
                    statement.setString(3, item.name());
                    statement.setInt(4, item.quantity());
                    statement.setBigDecimal(5, item.unitPrice());
                    statement.executeUpdate();
                }
            }
        }
    }
}

