-- Create the test database
CREATE DATABASE testDB;

-- Use the test database
\c testDB;

-- Create and populate our products using a single insert with many rows
CREATE TABLE products (
  id SERIAL PRIMARY KEY,  -- �������� �� SERIAL ��� �������������� � PostgreSQL
  name VARCHAR(255) NOT NULL,
  description VARCHAR(512),
  weight FLOAT
);

INSERT INTO products(name, description, weight) VALUES 
  ('scooter', 'Small 2-wheel scooter', 3.14),
  ('car battery', '12V car battery', 8.1),
  ('12-pack drill bits', '12-pack of drill bits with sizes ranging from #40 to #3', 0.8),
  ('hammer', '12oz carpenter''s hammer', 0.75),
  ('hammer', '14oz carpenter''s hammer', 0.875),
  ('hammer', '16oz carpenter''s hammer', 1.0),
  ('rocks', 'box of assorted rocks', 5.3),
  ('jacket', 'water resistant black wind breaker', 0.1),
  ('spare tire', '24 inch spare tire', 22.2);

-- Create and populate the products_on_hand using multiple inserts
CREATE TABLE products_on_hand (
  product_id INTEGER NOT NULL PRIMARY KEY,
  quantity INTEGER NOT NULL,
  FOREIGN KEY (product_id) REFERENCES products(id)
);

INSERT INTO products_on_hand VALUES 
  (1, 3),    -- �������� �� ��������������� id �� ������� products
  (2, 8),
  (3, 18),
  (4, 4),
  (5, 5),
  (6, 0),
  (7, 44),
  (8, 2),
  (9, 5);

-- Create some customers ...
CREATE TABLE customers (
  id SERIAL PRIMARY KEY,  -- �������� �� SERIAL ��� �������������� � PostgreSQL
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO customers(first_name, last_name, email) VALUES 
  ('Sally', 'Thomas', 'sally.thomas@acme.com'),
  ('George', 'Bailey', 'gbailey@foobar.com'),
  ('Edward', 'Walker', 'ed@walker.com'),
  ('Anne', 'Kretchmar', 'annek@noanswer.org');

-- Create some very simple orders
CREATE TABLE orders (
  id SERIAL PRIMARY KEY,  -- �������� �� SERIAL ��� �������������� � PostgreSQL
  order_date DATE NOT NULL,
  purchaser INTEGER NOT NULL,
  quantity INTEGER NOT NULL,
  product_id INTEGER NOT NULL,
  FOREIGN KEY (purchaser) REFERENCES customers(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);

INSERT INTO orders(order_date, purchaser, quantity, product_id) VALUES 
  ('2016-01-16', 1, 1, 2),  -- �������� 1001 �� 1, ������� ��������� � customers
  ('2016-01-17', 2, 2, 5),  -- �������� 1002 �� 2
  ('2016-02-19', 2, 2, 6),  -- �������� 1002 �� 2
  ('2016-02-21', 3, 1, 7);  -- �������� 1003 �� 3
