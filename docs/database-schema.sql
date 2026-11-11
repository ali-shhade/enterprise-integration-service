CREATE DATABASE IF NOT EXISTS integration_db;

USE integration_db;

CREATE TABLE IF NOT EXISTS integration_orders (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_system VARCHAR(50) NOT NULL,
    external_order_id VARCHAR(100) NOT NULL,
    customer VARCHAR(150) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uq_source_order
        (source_system, external_order_id),

    KEY idx_orders_status_created
        (status, created_at),

    CONSTRAINT chk_order_status CHECK (
        status IN (
            'RECEIVED',
            'VALIDATED',
            'PROCESSING',
            'SUCCESS',
            'REJECTED',
            'FAILED'
        )
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_status_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id BIGINT UNSIGNED NOT NULL,
    previous_status VARCHAR(20) DEFAULT NULL,
    new_status VARCHAR(20) NOT NULL,
    details VARCHAR(500) DEFAULT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    KEY idx_history_order_time (order_id, changed_at),

    CONSTRAINT fk_history_order
        FOREIGN KEY (order_id)
        REFERENCES integration_orders(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;
