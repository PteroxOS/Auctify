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
    end_time INTEGER NOT NULL,
    bin_only INTEGER NOT NULL DEFAULT 0,
    tax_exempt INTEGER NOT NULL DEFAULT 0
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

-- Pending money refunds for failed economy deposits
CREATE TABLE IF NOT EXISTS auctify_pending_refunds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    amount REAL NOT NULL,
    reason TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

-- Player ratings (reputation system)
CREATE TABLE IF NOT EXISTS auctify_ratings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    seller_uuid TEXT NOT NULL,
    rater_uuid TEXT NOT NULL,
    rating INTEGER NOT NULL,
    created_at INTEGER NOT NULL
);

-- Blacklisted players (admin moderation)
CREATE TABLE IF NOT EXISTS auctify_blacklist (
    player_uuid TEXT PRIMARY KEY,
    reason TEXT,
    blacklisted_by TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

-- Buy orders (WTB - Want to Buy)
CREATE TABLE IF NOT EXISTS auctify_buy_orders (
    id TEXT PRIMARY KEY,
    buyer_uuid TEXT NOT NULL,
    buyer_name TEXT NOT NULL,
    item_type TEXT NOT NULL,
    amount INTEGER NOT NULL,
    price_per_unit REAL NOT NULL,
    created_at INTEGER NOT NULL,
    expiry_time INTEGER NOT NULL,
    active INTEGER NOT NULL DEFAULT 1
);

-- Player watchlists
CREATE TABLE IF NOT EXISTS auctify_watchlist (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    listing_id TEXT NOT NULL,
    added_at INTEGER NOT NULL,
    UNIQUE(player_uuid, listing_id)
);

-- Pending buy order deliveries (for offline buyers)
CREATE TABLE IF NOT EXISTS auctify_pending_buy_deliveries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    item_data TEXT NOT NULL,
    order_id TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
