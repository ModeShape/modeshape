GRANT SELECT ON TABLE purchaseOrders TO maria,harry;

GRANT UPDATE, USAGE ON TABLE billedOrders TO anita,zhi;

GRANT SELECT ON TABLE orders.bills to PUBLIC;

GRANT INSERT(a, b, c) ON TABLE purchaseOrders TO purchases_reader_role;
