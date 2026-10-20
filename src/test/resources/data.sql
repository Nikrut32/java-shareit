INSERT INTO users (name, email) VALUES ('Иван Петров', 'ivan.petrov@mail.ru');
INSERT INTO users (name, email) VALUES ('Мария Сидорова', 'maria.sidorova@gmail.com');
INSERT INTO users (name, email) VALUES ('Алексей Кузнецов', 'alexey.kuznetsov@yandex.ru');

INSERT INTO items (name, description, available, owner_id, request_id, count_rental)
VALUES ('Перфоратор Makita', 'Мощный перфоратор, 800 Вт', TRUE, 1, 0, 0);
INSERT INTO items (name, description, available, owner_id, request_id, count_rental)
VALUES ('Палатка 4-местная', 'Кемпинговая палатка', FALSE, 2, 0, 0);
INSERT INTO items (name, description, available, owner_id, request_id, count_rental)
VALUES ('Проектор Epson', 'Full HD проектор, 3000 люмен', TRUE, 3, 0, 0);