-- FinSight Database Schema
-- Based on the original ExpenseManagementSystem schema

CREATE DATABASE IF NOT EXISTS expense_db;
USE expense_db;

-- 1. Users table
CREATE TABLE IF NOT EXISTS user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

-- 2. Transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    category VARCHAR(100),
    amount DECIMAL(10,2) NOT NULL,
    description VARCHAR(255),
    transaction_date DATE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);
