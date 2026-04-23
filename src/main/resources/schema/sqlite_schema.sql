-- Auctify SQLite Schema
-- Active auction listings persisted across server restarts
CREATE TABLE IF NOT EXISTS auctify_listings (
    id TEXT PRIMARY KEY,
    seller_uuid TEXT NOT NULL,
    seller_name TEXT NOT NULL,
    item_data TEXT NOT NULL,
    start_price REAL NOT NULL,
    buyout_price REAL NOT NULL,
    current_bid REAL NOT NULL,
    top_bidder_uuid TEXT,
    top_bidder_name TEXT,
    created_at INTEGER NOT NULL,
    end_time INTEGER NOT NULL
);

-- Completed auction history records
CREATE TABLE IF NOT EXISTS auctify_history (
    id TEXT PRIMARY KEY,
    seller_uuid TEXT NOT NULL,
    seller_name TEXT NOT NULL,
    winner_uuid TEXT,
    winner_name TEXT,
    item_data TEXT NOT NULL,
    start_price REAL NOT NULL,
    final_price REAL NOT NULL,
    tax_amount REAL NOT NULL,
    resolved_at INTEGER NOT NULL,
    reason TEXT NOT NULL
);

-- Items pending delivery to offline players
CREATE TABLE IF NOT EXISTS auctify_pending_deliveries (
    player_uuid TEXT NOT NULL,
    item_data TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
