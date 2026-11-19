package edu.cu.campuscart;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class ShopController {
    private final ShopRepository repository;
    private final SpringJdbcCheckout springCheckout;
    private final TraditionalJdbcCheckout traditionalCheckout;

    ShopController(ShopRepository repository, SpringJdbcCheckout springCheckout,
                   TraditionalJdbcCheckout traditionalCheckout) {
        this.repository = repository;
        this.springCheckout = springCheckout;
        this.traditionalCheckout = traditionalCheckout;
    }

    @GetMapping("/products")
    List<Product> products(@RequestParam(defaultValue = "") String q) {
        return repository.search(q);
    }

    @GetMapping("/customers")
    List<Customer> customers() {
        return repository.customers();
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    Customer register(@RequestBody Registration registration) {
        return repository.register(registration);
    }

    @GetMapping("/orders")
    List<OrderSummary> orders(@RequestParam long customerId) {
        return repository.orders(customerId);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    Receipt checkout(@RequestParam(defaultValue = "spring") String mode,
                     @RequestBody CheckoutRequest request) throws SQLException {
        return switch (mode) {
            case "spring" -> springCheckout.place(request);
            case "jdbc" -> traditionalCheckout.place(request);
            default -> throw new ShopException(400, "Choose spring or jdbc as the checkout mode.");
        };
    }

    @ExceptionHandler(ShopException.class)
    ResponseEntity<Map<String, String>> shopError(ShopException failure) {
        return ResponseEntity.status(failure.status()).body(Map.of("message", failure.getMessage()));
    }
}

