CREATE DATABASE IF NOT EXISTS skywalking_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE skywalking_demo;

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  slow_ms BIGINT NOT NULL DEFAULT 0,
  fail_rate DOUBLE NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  KEY idx_order_no (order_no),
  KEY idx_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS lab_actions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  action VARCHAR(64) NOT NULL,
  detail VARCHAR(255) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL,
  KEY idx_action (action),
  KEY idx_created_at (created_at)
);
