-- Datos demo solo para perfil H2 (spring.sql.init.mode=always).
INSERT INTO customer (id, first_name, last_name, document_number, email) VALUES
                                                                             (1, 'Ana', 'Gomez', 'DNI1001', 'ana.gomez@mail.com'),
                                                                             (2, 'Luis', 'Perez', 'DNI1002', 'luis.perez@mail.com'),
                                                                             (3, 'Carla', 'Romero', 'DNI1003', 'carla.romero@mail.com');

INSERT INTO branch (id, name, city, address, phone) VALUES
                                                        (1, 'Casa Central', 'Buenos Aires', 'Av. Corrientes 1234', '1122334455'),
                                                        (2, 'Sucursal Norte', 'Cordoba', 'San Martin 456', '3514455667'),
                                                        (3, 'Sucursal Oeste', 'Rosario', 'Belgrano 789', '3415566778');

INSERT INTO bank_account (id, customer_id, branch_id, account_number, account_type, balance, opened_at) VALUES
                                                                                                            (1, 1, 1, '00010001', 'CAJA_AHORRO', 150000.00, DATE '2025-01-10'),
                                                                                                            (2, 2, 1, '00010002', 'CUENTA_CORRIENTE', 800000.00, DATE '2025-02-05'),
                                                                                                            (3, 3, 2, '00020001', 'CAJA_AHORRO', 250000.00, DATE '2025-03-15');

INSERT INTO card (id, bank_account_id, card_number, card_type, expiry_date) VALUES
                                                                                (1, 1, '4111111111111111', 'DEBITO', DATE '2028-12-31'),
                                                                                (2, 2, '4222222222222222', 'CREDITO', DATE '2027-06-30'),
                                                                                (3, 3, '4333333333333333', 'DEBITO', DATE '2029-03-31');

INSERT INTO transaction_type (id, name, description, is_incoming, is_outgoing) VALUES
                                                                                   (1, 'DEPOSITO', 'Ingreso de dinero en cuenta', TRUE, FALSE),
                                                                                   (2, 'EXTRACCION', 'Salida de dinero en cuenta', FALSE, TRUE),
                                                                                   (3, 'TRANSFERENCIA', 'Movimiento entre cuentas', TRUE, TRUE);

INSERT INTO bank_transaction (id, bank_account_id, transaction_type_id, transaction_date, amount, reference) VALUES
                                                                                                                 (1, 1, 1, TIMESTAMP '2026-09-01 10:00:00', 50000.00, 'Sueldo Agosto'),
                                                                                                                 (2, 1, 2, TIMESTAMP '2026-09-02 12:30:00', 15000.00, 'Pago tarjeta'),
                                                                                                                 (3, 2, 3, TIMESTAMP '2026-09-03 09:15:00', 20000.00, 'Transferencia a cuenta 1');

INSERT INTO loan (id, customer_id, branch_id, amount, status) VALUES
                                                                  (1, 1, 1, 500000.00, 'APROBADO'),
                                                                  (2, 2, 2, 1200000.00, 'PENDIENTE'),
                                                                  (3, 3, 3, 300000.00, 'APROBADO');

INSERT INTO loan_payment (id, loan_id, payment_date, amount, payment_method) VALUES
                                                                                 (1, 1, DATE '2026-01-15', 25000.00, 'DEBITO_CUENTA'),
                                                                                 (2, 1, DATE '2026-02-15', 25000.00, 'DEBITO_CUENTA'),
                                                                                 (3, 3, DATE '2026-04-10', 15000.00, 'TRANSFERENCIA');

