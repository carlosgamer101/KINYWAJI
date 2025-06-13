CREATE TABLE IF NOT EXISTS branches (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS drinks (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    price DOUBLE NOT NULL
);

CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS stock (
    branch_id INT,
    drink_id INT,
    quantity INT NOT NULL,
    PRIMARY KEY (branch_id, drink_id),
    FOREIGN KEY (branch_id) REFERENCES branches(id),
    FOREIGN KEY (drink_id) REFERENCES drinks(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT,
    branch_id INT,
    drink_id INT,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (branch_id) REFERENCES branches(id),
    FOREIGN KEY (drink_id) REFERENCES drinks(id)
);

MERGE INTO branches (id, name) KEY(id) VALUES
(1, 'Nairobi'),
(2, 'Nakuru'),
(3, 'Mombasa'),
(4, 'Kisumu');

MERGE INTO drinks (id, name, price) KEY(id) VALUES
(1, 'Cola', 50.0),
(2, 'Fanta', 45.0),
(3, 'Sprite', 40.0),
(4, 'Water', 30.0);

MERGE INTO stock (branch_id, drink_id, quantity) KEY(branch_id, drink_id) VALUES
(1, 1, 100), (1, 2, 100), (1, 3, 100), (1, 4, 100),
(2, 1, 100), (2, 2, 100), (2, 3, 100), (2, 4, 100),
(3, 1, 100), (3, 2, 100), (3, 3, 100), (3, 4, 100),
(4, 1, 100), (4, 2, 100), (4, 3, 100), (4, 4, 100);