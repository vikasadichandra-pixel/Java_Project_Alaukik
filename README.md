# Campus Cart

Campus Cart is a Java 17 case study on product search, shopping carts, discounts,
and transactional order processing. It is a local classroom demo with sample
products and a sample customer.

## Run the packaged application

1. Extract the entire ZIP into a writable folder.
2. Open a terminal in the extracted `CampusCart` folder.
3. Run `java -version` to check that Java 17 or newer is installed.
4. Run `java -jar campus-cart.jar` and keep that terminal open.
5. Open [http://127.0.0.1:8080](http://127.0.0.1:8080) in a browser.
6. Press Ctrl+C in the terminal to stop the application.

The JAR and its adjacent `lib` folder are needed together. This launcher uses the
bundled dependencies and requires no Maven installation or database server.
The local H2 database is created in `data` beside the JAR. It retains orders and
stock changes when the app stops. For a clean demo, use a newly extracted copy or
stop the app and rename the existing `data` directory to keep it as a backup.
If port 8080 is in use, start with
`java -jar campus-cart.jar --server.port=8081` and open port 8081.

## Viva demonstration

1. Use the default customer or register a new one under **Order history**.
2. Add two A5 Notebooks and one USB-C Cable. Enter `CAMPUS10` and click
   **Place order**. The subtotal is INR 590.00, the discount is INR 59.00, and
   the final total is INR 531.00. Confirm that stock decreased and an order was
   recorded.
3. Add one A5 Notebook and two Scientific Calculators. A fresh catalog has only
   one calculator. Place the order and confirm the insufficient-stock message.
   The cart stays available, no order appears, and notebook stock stays the same.
4. Remove the calculator, then place the corrected order.
5. Search for `usb` to demonstrate case-insensitive product search.

The storefront uses Spring JDBC. On a fresh demo, this PowerShell command
reproduces the stock conflict using Traditional JDBC through the checkout API.

```powershell
Invoke-RestMethod 'http://127.0.0.1:8080/api/checkout?mode=jdbc' `
    -Method Post -ContentType 'application/json' `
    -Body '{"customerId":1,"items":[{"productId":1,"quantity":1},{"productId":3,"quantity":2}]}'
```

## Source build

Open `pom.xml` as a Maven project in your IDE or run
`mvn spring-boot:run` from this directory with Java 17 and Maven 3.6.3 or newer.
The first Maven build needs access to Maven Central. To build a separate
executable JAR, run `mvn clean package` and start
`java -jar target/campus-cart-1.0.0.jar`.

The Maven project pins Spring Boot 3.5.10. The included offline demo was compiled
and validated against the locally available Spring Boot 3.5.10 snapshot,
Spring Framework 6.2.15, and H2 2.3.232. See `validation-results.txt`
for the checkout and rollback results.

## Source map

| File | Role |
| --- | --- |
| ShopController.java | HTTP endpoints and checkout mode selection |
| Checkout.java | Shared rules, pricing, and checkout persistence interface |
| SpringJdbcCheckout.java | JdbcTemplate and declarative transactions |
| TraditionalJdbcCheckout.java | Connection, PreparedStatement, ResultSet, explicit commit and rollback |
| ShopRepository.java | Product search, customer registration, and order history |
| schema.sql and data.sql | H2 schema and repeatable sample-data initialization |
| static/ | Browser interface |

The catalog is public within the local demo. Customer selection is for
demonstration and does not implement sign-in. Use sample data only.

