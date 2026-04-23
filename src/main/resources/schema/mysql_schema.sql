-- Auctify MySQL Schema
-- Active auction listings persisted across server restarts
CREATE TABLE IF NOT EXISTS auctify_listings (
    id VARCHAR(8) PRIMARY KEY,
    seller_uuid VARCHAR(36) NOT NULL,
    seller_name VARCHAR(16) NOT NULL,
    item_data LONGTEXT NOT NULL,
    start_price DOUBLE NOT NULL,
    buyout_price DOUBLE NOT NULL,
    current_bid DOUBLE NOT NULL,
    top_bidder_uuid VARCHAR(36),
    top_bidder_name VARCHAR(16),
    created_at BIGINT NOT NULL,
    end_time BIGINT NOT NULL,
    INDEX idx_seller (seller_uuid),
    INDEX idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Completed auction history records
CREATE TABLE IF NOT EXISTS auctify_history (
    id VARCHAR(8) PRIMARY KEY,
    seller_uuid VARCHAR(36) NOT NULL,
    seller_name VARCHAR(16) NOT NULL,
    winner_uuid VARCHAR(36),
    winner_name VARCHAR(16),
    item_data LONGTEXT NOT NULL,
    start_price DOUBLE NOT NULL,
    final_price DOUBLE NOT NULL,
    tax_amount DOUBLE NOT NULL,
    resolved_at BIGINT NOT NULL,
    reason VARCHAR(32) NOT NULL,
    INDEX idx_seller_history (seller_uuid),
    INDEX idx_winner_history (winner_uuid),
    INDEX idx_resolved (resolved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Items pending delivery to offline players
CREATE TABLE IF NOT EXISTS auctify_pending_deliveries (
    player_uuid VARCHAR(36) NOT NULL,
    item_data LONGTEXT NOT NULL,
    created_at BIGINT NOT NULL,
    INDEX idx_player (player_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
