package dev.auctify.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.auctify.auction.AuctionHistory;
import dev.auctify.auction.AuctionListing;
import dev.auctify.util.ItemUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL implementation of {@link StorageManager} using HikariCP connection
 * pooling.
 * Recommended for production servers with high traffic.
 */
public class MySQLStorage implements StorageManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private HikariDataSource dataSource;

    /** @param plugin the main plugin instance */
    public MySQLStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        var cfg = plugin.getConfig();
        String host = cfg.getString("storage.mysql.host", "localhost");
        int port = cfg.getInt("storage.mysql.port", 3306);
        String db = cfg.getString("storage.mysql.database", "auctify");
        String user = cfg.getString("storage.mysql.username", "root");
        String pass = cfg.getString("storage.mysql.password", "password");
        int pool = cfg.getInt("storage.mysql.pool-size", 10);
        boolean ssl = cfg.getBoolean("storage.mysql.use-ssl", false);

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=" + ssl + "&autoReconnect=true");
        hc.setUsername(user);
        hc.setPassword(pass);
        hc.setMaximumPoolSize(pool);
        hc.setMinimumIdle(Math.max(1, pool / 2));
        hc.setConnectionTimeout(10000);
        hc.setIdleTimeout(300000); // 5 minutes
        hc.setMaxLifetime(1800000); // 30 minutes
        hc.setPoolName("Auctify-MySQL");
        hc.addDataSourceProperty("cachePrepStmts", "true");
        hc.addDataSourceProperty("prepStmtCacheSize", "250");
        dataSource = new HikariDataSource(hc);

        executeSchema();
        migrateSchema();
        logger.info("MySQL storage initialized: " + host + ":" + port + "/" + db);
    }

    private void executeSchema() {
        try (InputStream is = plugin.getResource("schema/mysql_schema.sql")) {
            if (is == null) {
                logger.severe("mysql_schema.sql not found!");
                return;
            }
            String schema = new String(is.readAllBytes());
            try (Connection conn = dataSource.getConnection()) {
                for (String s : schema.split(";")) {
                    String t = s.trim();
                    if (!t.isEmpty())
                        try (Statement st = conn.createStatement()) {
                            st.execute(t);
                        }
                }
            }
        } catch (IOException | SQLException e) {
            logger.log(Level.SEVERE, "MySQL schema exec failed", e);
        }
    }

    private void migrateSchema() {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            // Add tax_exempt column if missing (schema migration)
            try {
                st.execute("ALTER TABLE auctify_listings ADD COLUMN tax_exempt TINYINT NOT NULL DEFAULT 0");
            } catch (SQLException ignored) {
                // Column already exists or other expected error
            }
            try {
                st.execute("ALTER TABLE auctify_listings ADD COLUMN bin_only TINYINT NOT NULL DEFAULT 0");
            } catch (SQLException ignored) {
            }
            // Create price_history table if not exists
            try {
                st.execute(
                        "CREATE TABLE IF NOT EXISTS auctify_price_history (id INT AUTO_INCREMENT PRIMARY KEY, listing_id VARCHAR(36) NOT NULL, item_material VARCHAR(64) NOT NULL, item_name VARCHAR(255) NOT NULL, final_price DOUBLE NOT NULL, seller_name VARCHAR(36) NOT NULL, winner_name VARCHAR(36) NOT NULL, timestamp BIGINT NOT NULL, INDEX idx_item_material (item_material), INDEX idx_timestamp (timestamp))");
            } catch (SQLException ignored) {
            }
            // Create auto_bid table if not exists
            try {
                st.execute(
                        "CREATE TABLE IF NOT EXISTS auctify_auto_bid (id INT AUTO_INCREMENT PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, player_name VARCHAR(36) NOT NULL, listing_id VARCHAR(36) NOT NULL, max_bid_amount DOUBLE NOT NULL, created_at BIGINT NOT NULL, UNIQUE KEY unique_player_listing (player_uuid, listing_id), INDEX idx_listing (listing_id), INDEX idx_player (player_uuid))");
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "MySQL schema migration failed", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void saveListing(AuctionListing l) {
        String sql = "REPLACE INTO auctify_listings (id,seller_uuid,seller_name,item_data,start_price,buyout_price,current_bid,top_bidder_uuid,top_bidder_name,created_at,end_time,bin_only,tax_exempt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, l.getId());
            ps.setString(2, l.getSellerUUID().toString());
            ps.setString(3, l.getSellerName());
            ps.setString(4, ItemUtil.serializeToBase64(l.getItem()));
            ps.setDouble(5, l.getStartPrice());
            ps.setDouble(6, l.getBuyoutPrice());
            ps.setDouble(7, l.getCurrentBid());
            ps.setString(8, l.getTopBidderUUID() != null ? l.getTopBidderUUID().toString() : null);
            ps.setString(9, l.getTopBidderName());
            ps.setLong(10, l.getCreatedAt());
            ps.setLong(11, l.getEndTime());
            ps.setInt(12, l.isBinOnly() ? 1 : 0);
            ps.setInt(13, l.isTaxExempt() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Save listing failed: " + l.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteListing(String id) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM auctify_listings WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Delete listing failed: " + id, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<AuctionListing> getAllListings() {
        List<AuctionListing> res = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM auctify_listings")) {
            while (rs.next()) {
                ItemStack item = ItemUtil.deserializeFromBase64(rs.getString("item_data"));
                if (item == null)
                    continue;
                String tbuStr = rs.getString("top_bidder_uuid");
                UUID tbu = tbuStr != null ? UUID.fromString(tbuStr) : null;
                boolean binOnly = false;
                try {
                    binOnly = rs.getInt("bin_only") == 1;
                } catch (SQLException ignored) {
                }
                boolean taxExempt = false;
                try {
                    taxExempt = rs.getInt("tax_exempt") == 1;
                } catch (SQLException ignored) {
                }
                AuctionListing listing = new AuctionListing(rs.getString("id"),
                        UUID.fromString(rs.getString("seller_uuid")),
                        rs.getString("seller_name"), item, rs.getDouble("start_price"),
                        rs.getDouble("buyout_price"), rs.getDouble("current_bid"), tbu,
                        rs.getString("top_bidder_name"), rs.getLong("created_at"), rs.getLong("end_time"), binOnly);
                listing.setTaxExempt(taxExempt);
                res.add(listing);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Load listings failed", e);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public void saveHistory(AuctionHistory h) {
        String sql = "REPLACE INTO auctify_history (id,seller_uuid,seller_name,winner_uuid,winner_name,item_data,start_price,final_price,tax_amount,resolved_at,reason) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, h.id());
            ps.setString(2, h.sellerUUID().toString());
            ps.setString(3, h.sellerName());
            ps.setString(4, h.winnerUUID() != null ? h.winnerUUID().toString() : null);
            ps.setString(5, h.winnerName());
            ps.setString(6, h.itemData());
            ps.setDouble(7, h.startPrice());
            ps.setDouble(8, h.finalPrice());
            ps.setDouble(9, h.taxAmount());
            ps.setLong(10, h.resolvedAt());
            ps.setString(11, h.reason());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Save history failed", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<AuctionHistory> getHistory(UUID playerUUID, int limit) {
        List<AuctionHistory> res = new ArrayList<>();
        String sql = "SELECT * FROM auctify_history WHERE seller_uuid=? OR winner_uuid=? ORDER BY resolved_at DESC LIMIT ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String u = playerUUID.toString();
            ps.setString(1, u);
            ps.setString(2, u);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String wu = rs.getString("winner_uuid");
                    res.add(new AuctionHistory(rs.getString("id"), UUID.fromString(rs.getString("seller_uuid")),
                            rs.getString("seller_name"), wu != null ? UUID.fromString(wu) : null,
                            rs.getString("winner_name"), rs.getString("item_data"), rs.getDouble("start_price"),
                            rs.getDouble("final_price"), rs.getDouble("tax_amount"),
                            rs.getLong("resolved_at"), rs.getString("reason")));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Load history failed", e);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public void savePendingDelivery(UUID playerUUID, ItemStack item) {
        String sql = "INSERT INTO auctify_pending_deliveries (player_uuid,item_data,created_at) VALUES (?,?,?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, ItemUtil.serializeToBase64(item));
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Save pending delivery failed", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<ItemStack> getPendingDeliveries(UUID playerUUID) {
        List<ItemStack> res = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c
                        .prepareStatement("SELECT item_data FROM auctify_pending_deliveries WHERE player_uuid=?")) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemStack i = ItemUtil.deserializeFromBase64(rs.getString("item_data"));
                    if (i != null)
                        res.add(i);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Load pending failed", e);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public void clearPendingDeliveries(UUID playerUUID) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c
                        .prepareStatement("DELETE FROM auctify_pending_deliveries WHERE player_uuid=?")) {
            ps.setString(1, playerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Clear pending failed", e);
        }
    }

    // ─── Rating System ───────────────────────────────

    @Override
    public void saveRating(UUID sellerUUID, UUID raterUUID, int rating) {
        String sql = "REPLACE INTO auctify_ratings (seller_uuid,rater_uuid,rating,created_at) VALUES (?,?,?,?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sellerUUID.toString());
            ps.setString(2, raterUUID.toString());
            ps.setInt(3, rating);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Save rating failed", e);
        }
    }

    @Override
    public double getAverageRating(UUID sellerUUID) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT AVG(rating) as avg_rating FROM auctify_ratings WHERE seller_uuid=?")) {
            ps.setString(1, sellerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble("avg_rating");
                    if (rs.wasNull())
                        return -1;
                    return avg;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Get average rating failed", e);
        }
        return -1;
    }

    @Override
    public int getRatingCount(UUID sellerUUID) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c
                        .prepareStatement("SELECT COUNT(*) as cnt FROM auctify_ratings WHERE seller_uuid=?")) {
            ps.setString(1, sellerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Get rating count failed", e);
        }
        return 0;
    }

    @Override
    public boolean hasRated(UUID sellerUUID, UUID raterUUID) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT COUNT(*) as cnt FROM auctify_ratings WHERE seller_uuid=? AND rater_uuid=?")) {
            ps.setString(1, sellerUUID.toString());
            ps.setString(2, raterUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("cnt") > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Check has rated failed", e);
        }
        return false;
    }

    // ─── Blacklist System ────────────────────────────

    @Override
    public void addBlacklist(UUID playerUUID, String reason, String blacklistedBy) {
        String sql = "REPLACE INTO auctify_blacklist (player_uuid,reason,blacklisted_by,created_at) VALUES (?,?,?,?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, reason);
            ps.setString(3, blacklistedBy);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Add blacklist failed", e);
        }
    }

    @Override
    public void removeBlacklist(UUID playerUUID) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM auctify_blacklist WHERE player_uuid=?")) {
            ps.setString(1, playerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Remove blacklist failed", e);
        }
    }

    @Override
    public boolean isBlacklisted(UUID playerUUID) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c
                        .prepareStatement("SELECT COUNT(*) as cnt FROM auctify_blacklist WHERE player_uuid=?")) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("cnt") > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Check blacklist failed", e);
        }
        return false;
    }

    @Override
    public List<String[]> getBlacklist() {
        List<String[]> res = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM auctify_blacklist")) {
            while (rs.next()) {
                res.add(new String[] { rs.getString("player_uuid"), rs.getString("reason"),
                        rs.getString("blacklisted_by"), String.valueOf(rs.getLong("created_at")) });
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Get blacklist failed", e);
        }
        return res;
    }

    @Override
    public boolean listingExists(String id) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement("SELECT 1 FROM auctify_listings WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Listing exists check failed: " + id, e);
        }
        return false;
    }

    @Override
    public void savePendingRefund(PendingRefund refund) {
        String sql = "INSERT INTO auctify_pending_refunds (player_uuid,amount,reason,created_at) VALUES (?,?,?,?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, refund.playerUUID().toString());
            ps.setDouble(2, refund.amount());
            ps.setString(3, refund.reason());
            ps.setLong(4, refund.createdAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Save pending refund failed", e);
        }
    }

    @Override
    public List<PendingRefund> getPendingRefunds(UUID playerUUID) {
        List<PendingRefund> res = new ArrayList<>();
        String sql = "SELECT amount, reason, created_at FROM auctify_pending_refunds WHERE player_uuid=?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new PendingRefund(playerUUID, rs.getDouble("amount"),
                            rs.getString("reason"), rs.getLong("created_at")));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Get pending refunds failed", e);
        }
        return res;
    }

    @Override
    public void clearPendingRefunds(UUID playerUUID) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM auctify_pending_refunds WHERE player_uuid=?")) {
            ps.setString(1, playerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Clear pending refunds failed", e);
        }
    }

    @Override
    public List<PendingRefund> claimAndClearRefunds(UUID playerUUID) {
        List<PendingRefund> res = new ArrayList<>();
        String select = "SELECT amount, reason, created_at FROM auctify_pending_refunds WHERE player_uuid=?";
        String delete = "DELETE FROM auctify_pending_refunds WHERE player_uuid=?";
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(select)) {
                ps.setString(1, playerUUID.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        res.add(new PendingRefund(playerUUID, rs.getDouble("amount"),
                                rs.getString("reason"), rs.getLong("created_at")));
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(delete)) {
                ps.setString(1, playerUUID.toString());
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Atomic claim and clear refunds failed", e);
            return Collections.emptyList();
        }
        return res;
    }

    @Override
    public List<ItemStack> claimAndClearDeliveries(UUID playerUUID) {
        List<ItemStack> res = new ArrayList<>();
        String select = "SELECT item_data FROM auctify_pending_deliveries WHERE player_uuid=?";
        String delete = "DELETE FROM auctify_pending_deliveries WHERE player_uuid=?";
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(select)) {
                ps.setString(1, playerUUID.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ItemStack i = ItemUtil.deserializeFromBase64(rs.getString("item_data"));
                        if (i != null)
                            res.add(i);
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(delete)) {
                ps.setString(1, playerUUID.toString());
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Atomic claim and clear failed", e);
            return Collections.emptyList();
        }
        return res;
    }

    // ─── Bid History Implementation ─────────────────

    @Override
    public void recordBid(String listingId, UUID bidderUUID, String bidderName, double amount) {
        String sql = "INSERT INTO auctify_bid_history (listing_id, bidder_uuid, bidder_name, amount, bid_time) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, listingId);
            ps.setString(2, bidderUUID.toString());
            ps.setString(3, bidderName);
            ps.setDouble(4, amount);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to record bid", e);
        }
    }

    @Override
    public java.util.List<dev.auctify.auction.BidRecord> getBidHistory(String listingId) {
        java.util.List<dev.auctify.auction.BidRecord> bids = new java.util.ArrayList<>();
        String sql = "SELECT bidder_uuid, bidder_name, amount, bid_time FROM auctify_bid_history WHERE listing_id = ? ORDER BY bid_time DESC";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, listingId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                bids.add(new dev.auctify.auction.BidRecord(
                        UUID.fromString(rs.getString("bidder_uuid")),
                        rs.getString("bidder_name"),
                        rs.getDouble("amount"),
                        rs.getLong("bid_time")));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get bid history", e);
        }
        return bids;
    }

    // ─── Pending Notification Implementation ────────

    @Override
    public void addPendingNotification(UUID playerUUID, String type, String itemName, String winnerName, String amount,
            String netAmount) {
        String sql = "INSERT INTO auctify_pending_notifications (player_uuid, type, item_name, winner_name, amount, net_amount, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, type);
            ps.setString(3, itemName);
            ps.setString(4, winnerName);
            ps.setString(5, amount);
            ps.setString(6, netAmount);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to add pending notification", e);
        }
    }

    @Override
    public java.util.List<String[]> getAndClearPendingNotifications(UUID playerUUID) {
        java.util.List<String[]> notifications = new java.util.ArrayList<>();
        String selectSql = "SELECT type, item_name, winner_name, amount, net_amount FROM auctify_pending_notifications WHERE player_uuid = ?";
        String deleteSql = "DELETE FROM auctify_pending_notifications WHERE player_uuid = ?";
        try (Connection c = dataSource.getConnection()) {
            // Select
            try (PreparedStatement ps = c.prepareStatement(selectSql)) {
                ps.setString(1, playerUUID.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    notifications.add(new String[] {
                            rs.getString("type"),
                            rs.getString("item_name"),
                            rs.getString("winner_name"),
                            rs.getString("amount"),
                            rs.getString("net_amount")
                    });
                }
            }
            // Delete
            try (PreparedStatement ps = c.prepareStatement(deleteSql)) {
                ps.setString(1, playerUUID.toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get/clear pending notifications", e);
        }
        return notifications;
    }

    @Override
    public double[] getPriceStats(String itemType) {
        // TODO: Query history table for item type stats
        return null;
    }

    // ─── Buy Order Implementation ───────────────────

    @Override
    public void saveBuyOrder(dev.auctify.auction.BuyOrder order) {
        // TODO: Implement buy order storage
    }

    @Override
    public void deleteBuyOrder(String orderId) {
        // TODO: Implement buy order deletion
    }

    @Override
    public java.util.List<dev.auctify.auction.BuyOrder> getAllBuyOrders() {
        // TODO: Implement buy order retrieval
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<dev.auctify.auction.BuyOrder> getBuyOrdersByPlayer(UUID playerUUID) {
        // TODO: Implement buy order retrieval by player
        return java.util.Collections.emptyList();
    }

    // ─── Watchlist Implementation ───────────────────

    @Override
    public void addToWatchlist(UUID playerUUID, String listingId) {
        // TODO: Implement watchlist storage
    }

    @Override
    public void removeFromWatchlist(UUID playerUUID, String listingId) {
        // TODO: Implement watchlist removal
    }

    @Override
    public boolean isInWatchlist(UUID playerUUID, String listingId) {
        // TODO: Implement watchlist check
        return false;
    }

    @Override
    public java.util.List<String> getWatchlist(UUID playerUUID) {
        // TODO: Implement watchlist retrieval
        return java.util.Collections.emptyList();
    }

    @Override
    public void clearWatchlist(UUID playerUUID) {
        // TODO: Implement watchlist clear
    }

    // ─── Pending Buy Order Deliveries ───────────────

    @Override
    public void addPendingBuyDelivery(UUID playerUUID, org.bukkit.inventory.ItemStack item, String orderId) {
        // TODO: Implement pending buy delivery storage for MySQL
    }

    @Override
    public java.util.List<org.bukkit.inventory.ItemStack> getPendingBuyDeliveries(UUID playerUUID) {
        // TODO: Implement pending buy delivery retrieval for MySQL
        return java.util.Collections.emptyList();
    }

    @Override
    public void clearPendingBuyDeliveries(UUID playerUUID) {
        // TODO: Implement pending buy delivery clear for MySQL
    }

    // ─── Price History System ─────────────────────────

    @Override
    public void savePriceHistory(dev.auctify.auction.PriceHistory priceHistory) {
        String sql = "INSERT INTO auctify_price_history (listing_id, item_material, item_name, final_price, seller_name, winner_name, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, priceHistory.getId());
            ps.setString(2, priceHistory.getItemMaterial());
            ps.setString(3, priceHistory.getItemName());
            ps.setDouble(4, priceHistory.getFinalPrice());
            ps.setString(5, priceHistory.getSellerName());
            ps.setString(6, priceHistory.getWinnerName());
            ps.setLong(7, priceHistory.getTimestamp());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save price history", e);
        }
    }

    @Override
    public java.util.List<dev.auctify.auction.PriceHistory> getPriceHistory(String itemType, int limit) {
        java.util.List<dev.auctify.auction.PriceHistory> result = new java.util.ArrayList<>();
        String sql = "SELECT listing_id, item_material, item_name, final_price, seller_name, winner_name, timestamp FROM auctify_price_history WHERE item_material = ? ORDER BY timestamp DESC LIMIT ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, itemType);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new dev.auctify.auction.PriceHistory(
                            rs.getString("listing_id"),
                            rs.getString("item_material"),
                            rs.getString("item_name"),
                            rs.getDouble("final_price"),
                            rs.getString("seller_name"),
                            rs.getString("winner_name"),
                            rs.getLong("timestamp")));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get price history", e);
        }
        return result;
    }

    @Override
    public java.util.List<dev.auctify.auction.PriceHistory> getAllPriceHistory(int limit) {
        java.util.List<dev.auctify.auction.PriceHistory> result = new java.util.ArrayList<>();
        String sql = "SELECT listing_id, item_material, item_name, final_price, seller_name, winner_name, timestamp FROM auctify_price_history ORDER BY timestamp DESC LIMIT ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new dev.auctify.auction.PriceHistory(
                            rs.getString("listing_id"),
                            rs.getString("item_material"),
                            rs.getString("item_name"),
                            rs.getDouble("final_price"),
                            rs.getString("seller_name"),
                            rs.getString("winner_name"),
                            rs.getLong("timestamp")));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get all price history", e);
        }
        return result;
    }

    // ─── Auto-Bid System ─────────────────────────────

    @Override
    public void saveAutoBid(dev.auctify.auction.AutoBid autoBid) {
        String sql = "INSERT INTO auctify_auto_bid (player_uuid, player_name, listing_id, max_bid_amount, created_at) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), max_bid_amount = VALUES(max_bid_amount), created_at = VALUES(created_at)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, autoBid.getPlayerUUID().toString());
            ps.setString(2, autoBid.getPlayerName());
            ps.setString(3, autoBid.getListingId());
            ps.setDouble(4, autoBid.getMaxBidAmount());
            ps.setLong(5, autoBid.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save auto-bid", e);
        }
    }

    @Override
    public dev.auctify.auction.AutoBid getAutoBid(String listingId, java.util.UUID playerUUID) {
        String sql = "SELECT player_uuid, player_name, listing_id, max_bid_amount, created_at FROM auctify_auto_bid WHERE listing_id = ? AND player_uuid = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, listingId);
            ps.setString(2, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new dev.auctify.auction.AutoBid(
                            java.util.UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name"),
                            rs.getString("listing_id"),
                            rs.getDouble("max_bid_amount"),
                            rs.getLong("created_at"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get auto-bid", e);
        }
        return null;
    }

    @Override
    public java.util.List<dev.auctify.auction.AutoBid> getAutoBidsForListing(String listingId) {
        java.util.List<dev.auctify.auction.AutoBid> result = new java.util.ArrayList<>();
        String sql = "SELECT player_uuid, player_name, listing_id, max_bid_amount, created_at FROM auctify_auto_bid WHERE listing_id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, listingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new dev.auctify.auction.AutoBid(
                            java.util.UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name"),
                            rs.getString("listing_id"),
                            rs.getDouble("max_bid_amount"),
                            rs.getLong("created_at")));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get auto-bids for listing", e);
        }
        return result;
    }

    @Override
    public java.util.List<dev.auctify.auction.AutoBid> getAutoBidsForPlayer(java.util.UUID playerUUID) {
        java.util.List<dev.auctify.auction.AutoBid> result = new java.util.ArrayList<>();
        String sql = "SELECT player_uuid, player_name, listing_id, max_bid_amount, created_at FROM auctify_auto_bid WHERE player_uuid = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new dev.auctify.auction.AutoBid(
                            java.util.UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name"),
                            rs.getString("listing_id"),
                            rs.getDouble("max_bid_amount"),
                            rs.getLong("created_at")));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get auto-bids for player", e);
        }
        return result;
    }

    @Override
    public void deleteAutoBid(String listingId, java.util.UUID playerUUID) {
        String sql = "DELETE FROM auctify_auto_bid WHERE listing_id = ? AND player_uuid = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, listingId);
            ps.setString(2, playerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete auto-bid", e);
        }
    }

    @Override
    public void deleteAutoBidsForListing(String listingId) {
        String sql = "DELETE FROM auctify_auto_bid WHERE listing_id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, listingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete auto-bids for listing", e);
        }
    }

    @Override
    public void clearAutoBidsForPlayer(java.util.UUID playerUUID) {
        String sql = "DELETE FROM auctify_auto_bid WHERE player_uuid = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to clear auto-bids for player", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("MySQL pool closed.");
        }
    }
}
