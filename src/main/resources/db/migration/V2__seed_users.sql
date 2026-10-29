-- All seed users share the password "password123" (BCrypt).
INSERT INTO users (id, name, date_of_birth, password_hash) VALUES
    (1, 'Alice Johnson', '1990-05-14', '$2a$10$wqMbX8ZwNzdjiD3b6h/uyee5xsuTbcWPfb5jH/JzwTPvmX8cQoSg6'),
    (2, 'Bob Smith',     '1985-11-02', '$2a$10$wqMbX8ZwNzdjiD3b6h/uyee5xsuTbcWPfb5jH/JzwTPvmX8cQoSg6'),
    (3, 'Alina Petrova', '1998-03-27', '$2a$10$wqMbX8ZwNzdjiD3b6h/uyee5xsuTbcWPfb5jH/JzwTPvmX8cQoSg6');
SELECT setval('users_id_seq', 3);

INSERT INTO accounts (user_id, balance, initial_deposit) VALUES
    (1, 1000.00, 1000.00),
    (2, 500.00, 500.00),
    (3, 250.00, 250.00);

INSERT INTO emails (user_id, value) VALUES
    (1, 'alice@example.com'),
    (1, 'alice.j@example.com'),
    (2, 'bob@example.com'),
    (3, 'alina@example.com');

INSERT INTO phones (user_id, value) VALUES
    (1, '79990000001'),
    (2, '79990000002'),
    (2, '79990000012'),
    (3, '79990000003');
