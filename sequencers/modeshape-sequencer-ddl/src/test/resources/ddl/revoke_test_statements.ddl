REVOKE SELECT ON TABLE purchaseOrders FROM maria,harry;

REVOKE UPDATE, USAGE ON TABLE orderDetails FROM anita,zhi CASCADE;

REVOKE SELECT ON TABLE orders.bills FROM PUBLIC RESTRICT;

REVOKE INSERT(a, b, c) ON TABLE orderSummaries FROM purchases_reader_role;
