-- Demo seed data: one order per saga outcome
-- All orders owned by user1 (id: 00000000-0000-0000-0000-000000000001)

-- Order 1: Happy path – Led Zeppelin IV + Pink Floyd Animals (4 days ago)
INSERT INTO orders (id, status, version, user_id, user_name, created_at) VALUES
  ('e3f2a1b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', 'INVENTORY_APPROVED', 0, '00000000-0000-0000-0000-000000000001', 'user1', NOW() - INTERVAL '4 days');
INSERT INTO order_items (id, order_id, product_id, quantity, price) VALUES
  ('10000000-0000-0000-0001-000000000001', 'e3f2a1b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', 'a1b2c3d4-0001-0001-0001-000000000001', 1, 34.99),
  ('10000000-0000-0000-0001-000000000002', 'e3f2a1b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', 'a1b2c3d4-0001-0001-0001-000000000002', 1, 36.99);
INSERT INTO order_status_history (id, order_id, status, occurred_at) VALUES
  (gen_random_uuid(), 'e3f2a1b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', 'CREATED',            NOW() - INTERVAL '4 days'),
  (gen_random_uuid(), 'e3f2a1b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', 'PAID',               NOW() - INTERVAL '4 days' + INTERVAL '3 seconds'),
  (gen_random_uuid(), 'e3f2a1b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', 'INVENTORY_APPROVED', NOW() - INTERVAL '4 days' + INTERVAL '6 seconds');
INSERT INTO order_saga (order_id, step, deadline, correlation_id, updated_at) VALUES
  ('e3f2a1b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', 'COMPLETED', NOW() - INTERVAL '4 days', gen_random_uuid()::text, NOW() - INTERVAL '4 days');

-- Order 2: Inventory rejected + payment rolled back – Black Sabbath Vol. 4 (2 days ago)
INSERT INTO orders (id, status, version, user_id, user_name, created_at) VALUES
  ('9c8d7e6f-5a4b-4c3d-2e1f-0a9b8c7d6e5f', 'INVENTORY_REJECTED', 0, '00000000-0000-0000-0000-000000000001', 'user1', NOW() - INTERVAL '2 days 5 hours');
INSERT INTO order_items (id, order_id, product_id, quantity, price) VALUES
  ('10000000-0000-0000-0001-000000000003', '9c8d7e6f-5a4b-4c3d-2e1f-0a9b8c7d6e5f', 'a1b2c3d4-0001-0001-0001-000000000003', 1, 31.99);
INSERT INTO order_status_history (id, order_id, status, occurred_at) VALUES
  (gen_random_uuid(), '9c8d7e6f-5a4b-4c3d-2e1f-0a9b8c7d6e5f', 'CREATED',             NOW() - INTERVAL '2 days 5 hours'),
  (gen_random_uuid(), '9c8d7e6f-5a4b-4c3d-2e1f-0a9b8c7d6e5f', 'PAID',                NOW() - INTERVAL '2 days 5 hours' + INTERVAL '2 seconds'),
  (gen_random_uuid(), '9c8d7e6f-5a4b-4c3d-2e1f-0a9b8c7d6e5f', 'INVENTORY_REJECTED',  NOW() - INTERVAL '2 days 5 hours' + INTERVAL '5 seconds'),
  (gen_random_uuid(), '9c8d7e6f-5a4b-4c3d-2e1f-0a9b8c7d6e5f', 'PAYMENT_ROLLED_BACK', NOW() - INTERVAL '2 days 5 hours' + INTERVAL '8 seconds');
INSERT INTO order_saga (order_id, step, deadline, correlation_id, updated_at) VALUES
  ('9c8d7e6f-5a4b-4c3d-2e1f-0a9b8c7d6e5f', 'CANCELLED', NOW() - INTERVAL '2 days 5 hours', gen_random_uuid()::text, NOW() - INTERVAL '2 days 5 hours');

-- Order 3: Happy path – Abbey Road x2 + Jailbreak x1 (7 hours ago)
INSERT INTO orders (id, status, version, user_id, user_name, created_at) VALUES
  ('2f4d6b8a-0e2c-4a4e-8f0b-2c4e6a8c0e2a', 'INVENTORY_APPROVED', 0, '00000000-0000-0000-0000-000000000001', 'user1', NOW() - INTERVAL '7 hours');
INSERT INTO order_items (id, order_id, product_id, quantity, price) VALUES
  ('10000000-0000-0000-0001-000000000004', '2f4d6b8a-0e2c-4a4e-8f0b-2c4e6a8c0e2a', 'a1b2c3d4-0001-0001-0001-000000000004', 2, 38.99),
  ('10000000-0000-0000-0001-000000000005', '2f4d6b8a-0e2c-4a4e-8f0b-2c4e6a8c0e2a', 'a1b2c3d4-0001-0001-0001-000000000007', 1, 32.99);
INSERT INTO order_status_history (id, order_id, status, occurred_at) VALUES
  (gen_random_uuid(), '2f4d6b8a-0e2c-4a4e-8f0b-2c4e6a8c0e2a', 'CREATED',            NOW() - INTERVAL '7 hours'),
  (gen_random_uuid(), '2f4d6b8a-0e2c-4a4e-8f0b-2c4e6a8c0e2a', 'PAID',               NOW() - INTERVAL '7 hours' + INTERVAL '2 seconds'),
  (gen_random_uuid(), '2f4d6b8a-0e2c-4a4e-8f0b-2c4e6a8c0e2a', 'INVENTORY_APPROVED', NOW() - INTERVAL '7 hours' + INTERVAL '5 seconds');
INSERT INTO order_saga (order_id, step, deadline, correlation_id, updated_at) VALUES
  ('2f4d6b8a-0e2c-4a4e-8f0b-2c4e6a8c0e2a', 'COMPLETED', NOW() - INTERVAL '7 hours', gen_random_uuid()::text, NOW() - INTERVAL '7 hours');

-- Order 4: Payment failed – King Crimson + AC/DC x2 (14 hours ago)
INSERT INTO orders (id, status, version, user_id, user_name, created_at) VALUES
  ('b7c8d9e0-f1a2-4b3c-4d5e-6f7a8b9c0d1e', 'PAYMENT_FAILED', 0, '00000000-0000-0000-0000-000000000001', 'user1', NOW() - INTERVAL '14 hours');
INSERT INTO order_items (id, order_id, product_id, quantity, price) VALUES
  ('10000000-0000-0000-0001-000000000006', 'b7c8d9e0-f1a2-4b3c-4d5e-6f7a8b9c0d1e', 'a1b2c3d4-0001-0001-0001-000000000005', 1, 39.99),
  ('10000000-0000-0000-0001-000000000007', 'b7c8d9e0-f1a2-4b3c-4d5e-6f7a8b9c0d1e', 'a1b2c3d4-0001-0001-0001-000000000006', 2, 29.99);
INSERT INTO order_status_history (id, order_id, status, occurred_at) VALUES
  (gen_random_uuid(), 'b7c8d9e0-f1a2-4b3c-4d5e-6f7a8b9c0d1e', 'CREATED',        NOW() - INTERVAL '14 hours'),
  (gen_random_uuid(), 'b7c8d9e0-f1a2-4b3c-4d5e-6f7a8b9c0d1e', 'PAYMENT_FAILED', NOW() - INTERVAL '14 hours' + INTERVAL '3 seconds');
INSERT INTO order_saga (order_id, step, deadline, correlation_id, updated_at) VALUES
  ('b7c8d9e0-f1a2-4b3c-4d5e-6f7a8b9c0d1e', 'CANCELLED', NOW() - INTERVAL '14 hours', gen_random_uuid()::text, NOW() - INTERVAL '14 hours');

-- Order 5: Cancelled (timeout) – Dire Straits Making Movies (1 day 8 hours ago)
INSERT INTO orders (id, status, version, user_id, user_name, created_at) VALUES
  ('7a8b9c0d-1e2f-4a3b-5c6d-7e8f9a0b1c2d', 'CANCELLED', 0, '00000000-0000-0000-0000-000000000001', 'user1', NOW() - INTERVAL '1 day 8 hours');
INSERT INTO order_items (id, order_id, product_id, quantity, price) VALUES
  ('10000000-0000-0000-0001-000000000008', '7a8b9c0d-1e2f-4a3b-5c6d-7e8f9a0b1c2d', 'a1b2c3d4-0001-0001-0001-000000000008', 1, 33.99);
INSERT INTO order_status_history (id, order_id, status, occurred_at) VALUES
  (gen_random_uuid(), '7a8b9c0d-1e2f-4a3b-5c6d-7e8f9a0b1c2d', 'CREATED',   NOW() - INTERVAL '1 day 8 hours'),
  (gen_random_uuid(), '7a8b9c0d-1e2f-4a3b-5c6d-7e8f9a0b1c2d', 'CANCELLED', NOW() - INTERVAL '1 day 8 hours' + INTERVAL '30 seconds');
INSERT INTO order_saga (order_id, step, deadline, correlation_id, updated_at) VALUES
  ('7a8b9c0d-1e2f-4a3b-5c6d-7e8f9a0b1c2d', 'CANCELLED', NOW() - INTERVAL '1 day 8 hours', gen_random_uuid()::text, NOW() - INTERVAL '1 day 8 hours');

-- Order 6: Inventory rejected + payment rolled back – Thin Lizzy + King Crimson (2 hours ago)
INSERT INTO orders (id, status, version, user_id, user_name, created_at) VALUES
  ('4d5e6f7a-8b9c-4d0e-1f2a-3b4c5d6e7f8a', 'INVENTORY_REJECTED', 0, '00000000-0000-0000-0000-000000000001', 'user1', NOW() - INTERVAL '2 hours');
INSERT INTO order_items (id, order_id, product_id, quantity, price) VALUES
  ('10000000-0000-0000-0001-000000000009', '4d5e6f7a-8b9c-4d0e-1f2a-3b4c5d6e7f8a', 'a1b2c3d4-0001-0001-0001-000000000007', 1, 32.99),
  ('10000000-0000-0000-0001-000000000010', '4d5e6f7a-8b9c-4d0e-1f2a-3b4c5d6e7f8a', 'a1b2c3d4-0001-0001-0001-000000000005', 1, 39.99);
INSERT INTO order_status_history (id, order_id, status, occurred_at) VALUES
  (gen_random_uuid(), '4d5e6f7a-8b9c-4d0e-1f2a-3b4c5d6e7f8a', 'CREATED',             NOW() - INTERVAL '2 hours'),
  (gen_random_uuid(), '4d5e6f7a-8b9c-4d0e-1f2a-3b4c5d6e7f8a', 'PAID',                NOW() - INTERVAL '2 hours' + INTERVAL '2 seconds'),
  (gen_random_uuid(), '4d5e6f7a-8b9c-4d0e-1f2a-3b4c5d6e7f8a', 'INVENTORY_REJECTED',  NOW() - INTERVAL '2 hours' + INTERVAL '5 seconds'),
  (gen_random_uuid(), '4d5e6f7a-8b9c-4d0e-1f2a-3b4c5d6e7f8a', 'PAYMENT_ROLLED_BACK', NOW() - INTERVAL '2 hours' + INTERVAL '8 seconds');
INSERT INTO order_saga (order_id, step, deadline, correlation_id, updated_at) VALUES
  ('4d5e6f7a-8b9c-4d0e-1f2a-3b4c5d6e7f8a', 'CANCELLED', NOW() - INTERVAL '2 hours', gen_random_uuid()::text, NOW() - INTERVAL '2 hours');
