INSERT INTO products (id, name, category, price, stock)
SELECT seed.id, seed.name, seed.category, seed.price, seed.stock
FROM (VALUES
    (1, 'A5 Notebook', 'Stationery', 120.00, 12),
    (2, 'USB-C Cable', 'Electronics', 350.00, 8),
    (3, 'Scientific Calculator', 'Electronics', 899.00, 1),
    (4, 'Steel Water Bottle', 'Campus essentials', 499.00, 6),
    (5, 'Gel Pen Set', 'Stationery', 90.00, 15),
    (6, 'Canvas Tote', 'Campus essentials', 249.00, 5)
) AS seed(id, name, category, price, stock)
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.id = seed.id);
INSERT INTO customers(name, email)
SELECT 'Demo Student', 'student@example.com'
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'student@example.com');

