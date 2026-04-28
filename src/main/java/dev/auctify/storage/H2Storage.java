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
 * H2 implementation of StorageManager using HikariCP connection pooling.
 * H2 is a fast, file-based SQL database with excellent performance for
 * read/write operations.
 */
public class H2Storage implements StorageManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private HikariDataSource dataSource;

    /** Constructor. */
    public H2Storage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @Override
    public void initialize() {
        String fileName = plugin.getConfig().getString("storage.h2.file", "auctify.h2.db");
        File dbFile = new File(plugin.getDataFolder(), fileName);
        if (!dbFile.getParentFile().exists())
            dbFile.getParentFile().mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:" + dbFile.getAbsolutePath() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("Auctify-H2");
        dataSource = new HikariDataSource(config);

        executeSchema();
        migrateSchema();
        logger.info("H2 storage initialized: " + dbFile.getAbsolutePath());
    }

    private void executeSchema() {
        try (InputStream is = plugin.getResource("schema/h2_schema.sql")) {
            if (is == null) {
                logger.warning("h2_schema.sql not found, falling back to sqlite_schema.sql");
                try (InputStream sqliteIs = plugin.getResource("schema/sqlite_schema.sql")) {
                    if (sqliteIs != null) {
                        String schema = new String(sqliteIs.readAllBytes());
                        adaptAndExecuteSchema(schema);
                    }
                }
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
            logger.log(Level.SEVERE, "Schema exec failed", e);
        }
    }

    private void adaptAndExecuteSchema(String schema) {
        // Adapt SQLite schema to H2 syntax
        schema = schema.replace("INTEGER PRIMARY KEY AUTOINCREMENT", "INT AUTO_INCREMENT PRIMARY KEY");
        schema = schema.replace("INSERT OR REPLACE", "MERGE");
        try (Connection conn = dataSource.getConnection()) {
            for (String s : schema.split(";")) {
                String t = s.trim();
                if (!t.isEmpty())
                    try (Statement st = conn.createStatement()) {
                        st.execute(t);
                    }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Schema adaptation failed", e);
        }
    }

    private void migrateSchema() {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            // Add bin_only column if missing
            try {
                st.execute("ALTER TABLE auctify_listings ADD COLUMN IF NOT EXISTS bin_only INT NOT NULL DEFAULT 0");
            } catch (SQLException e) {
                // FIX M-1: H2 menggunakan IF NOT EXISTS seharusnya aman, tapi tetap log error
                // yang unexpected
                if (!e.getMessage().toLowerCase().contains("duplicate")
                        && !e.getMessage().toLowerCase().contains("already exists")) {
                    logger.log(Level.WARNING,
                            "[Auctify] H2 schema migration warning adding bin_only: " + e.getMessage());
                }
            }
            // Add tax_exempt column if missing
            try {
                st.execute("ALTER TABLE auctify_listings ADD COLUMN IF NOT EXISTS tax_exempt INT NOT NULL DEFAULT 0");
            } catch (SQLException e) {
                // FIX M-1: Log error yang unexpected
                if (!e.getMessage().toLowerCase().contains("duplicate")
                        && !e.getMessage().toLowerCase().contains("already exists")) {
                    logger.log(Level.WARNING,
                            "[Auctify] H2 schema migration warning adding tax_exempt: " + e.getMessage());
                }
            }
            // Create bid_history table if not exists
            try {
                st.execute(
                        "CREATE TABLE IF NOT EXISTS auctify_bid_history (id INT AUTO_INCREMENT PRIMARY KEY, listing_id VARCHAR(36) NOT NULL, bidder_uuid VARCHAR(36) NOT NULL, bidder_name VARCHAR(100) NOT NULL, amount DOUBLE NOT NULL, bid_time BIGINT NOT NULL)");
            } catch (SQLException e) {
                // FIX M-1: Log error yang unexpected
                if (!e.getMessage().toLowerCase().contains("already exists")) {
                    logger.log(Level.SEVERE,
                            "[Auctify] H2 schema migration failed creating bid_history: " + e.getMessage());
                }
            }
            // Create pending_notifications table if not exists
            try {
                st.execute(
                        "CREATE TABLE IF NOT EXISTS auctify_pending_notifications (id INT AUTO_INCREMENT PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, type VARCHAR(50) NOT NULL, item_name VARCHAR(255), winner_name VARCHAR(100), amount VARCHAR(50), net_amount VARCHAR(50), created_at BIGINT NOT NULL)");
            } catch (SQLException e) {
                // FIX M-1: Log error yang unexpected
                if (!e.getMessage().toLowerCase().contains("already exists")) {
                    logger.log(Level.SEVERE,
                            "[Auctify] H2 schema migration failed creating pending_notifications: " + e.getMessage());
                }
            }
            // Create price_history table if not exists
            try {
                st.execute(
                        "CREATE TABLE IF NOT EXISTS auctify_price_history (id INT AUTO_INCREMENT PRIMARY KEY, listing_id VARCHAR(36) NOT NULL, item_material VARCHAR(100) NOT NULL, item_name VARCHAR(255) NOT NULL, final_price DOUBLE NOT NULL, seller_name VARCHAR(100) NOT NULL, winner_name VARCHAR(100) NOT NULL, timestamp BIGINT NOT NULL)");
            } catch (SQLException e) {
                // FIX M-1: Log error yang unexpected
                if (!e.getMessage().toLowerCase().contains("already exists")) {
                    logger.log(Level.SEVERE,
                            "[Auctify] H2 schema migration failed creating price_history: " + e.getMessage());
                }
            }
            // Create auto_bid table if not exists
            try {
                st.execute(
                        "CREATE TABLE IF NOT EXISTS auctify_auto_bid (id INT AUTO_INCREMENT PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, player_name VARCHAR(100) NOT NULL, listing_id VARCHAR(36) NOT NULL, max_bid_amount DOUBLE NOT NULL, created_at BIGINT NOT NULL, CONSTRAINT UNIQUE(player_uuid, listing_id))");
            } catch (SQLException e) {
                // FIX M-1: Log error yang unexpected
                if (!e.getMessage().toLowerCase().contains("already exists")) {
                    logger.log(Level.SEVERE,
                            "[Auctify] H2 schema migration failed creating auto_bid: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Schema migration failed", e);
        }
    }

    @Override
    public void saveListing(AuctionListing l) {
        long remainingMs = Math.max(0, l.getEndTime() - System.currentTimeMillis());
        String sql = "MERGE INTO auctify_listings (id,seller_uuid,seller_name,item_data,start_price,buyout_price,current_bid,top_bidder_uuid,top_bidder_name,created_at,end_time,bin_only,tax_exempt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
            ps.setLong(11, remainingMs);
            ps.setInt(12, l.isBinOnly() ? 1 : 0);
            ps.setInt(13, l.isTaxExempt() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Save listing failed: " + l.getId(), e);
        }
    }

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

    @Override
    public List<AuctionListing> getAllListings() {
        List<AuctionListing> res = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM auctify_listings")) {
            while (rs.next()) {
                AuctionListing l = deserializeListing(rs);
                if (l != null)
                    res.add(l);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Load listings failed", e);
        }
        return res;
    }

    private AuctionListing deserializeListing(ResultSet rs) throws SQLException {
        ItemStack item = ItemUtil.deserializeFromBase64(rs.getString("item_data"));
        if (item == null) {
            logger.warning("Bad item data for listing " + rs.getString("id"));
            return null;
        }
        String tbuStr = rs.getString("top_bidder_uuid");
        UUID tbu = tbuStr != null ? UUID.fromString(tbuStr) : null;
        long remainingMs = rs.getLong("end_time");
        long absoluteEndTime = System.currentTimeMillis() + remainingMs;
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
        AuctionListing listing = new AuctionListing(rs.getString("id"), UUID.fromString(rs.getString("seller_uuid")),
                rs.getString("seller_name"), item, rs.getDouble("start_price"), rs.getDouble("buyout_price"),
                rs.getDouble("current_bid"), tbu, rs.getString("top_bidder_name"),
                rs.getLong("created_at"), absoluteEndTime, binOnly);
        listing.setTaxExempt(taxExempt);
        return listing;
    }

    @Override
    public void saveHistory(AuctionHistory h) {
        String sql = "MERGE INTO auctify_history (id,seller_uuid,seller_name,winner_uuid,winner_name,item_data,start_price,final_price,tax_amount,resolved_at,reason) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
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
            logger.log(Level.SEVERE, "Save history failed: " + h.id(), e);
        }
    }

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
            logger.log(Level.SEVERE, "Load pending deliveries failed", e);
        }
        return res;
    }

    @Override
    public void clearPendingDeliveries(UUID playerUUID) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c
                        .prepareStatement("DELETE FROM auctify_pending_deliveries WHERE player_uuid=?")) {
            ps.setString(1, playerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Clear pending deliveries failed", e);
        }
    }

    // ─── Rating System ───────────────────────────────

    @Override
    public void saveRating(UUID sellerUUID, UUID raterUUID, int rating) {
        String sql = "INSERT INTO auctify_ratings (seller_uuid,rater_uuid,rating,created_at) VALUES (?,?,?,?)";
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

    @Override
    public boolean hasTransactionWith(UUID raterUUID, UUID sellerUUID) {
        // FIX-8: Check if rater has ever won an auction from seller (reason = 'SOLD'
        // and winner = rater and seller = seller)
        String sql = "SELECT COUNT(*) as cnt FROM auctify_history " +
                "WHERE winner_uuid = ? AND seller_uuid = ? AND reason = 'SOLD'";
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, raterUUID.toString());
            ps.setString(2, sellerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("cnt") > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Check has transaction with failed", e);
        }
        return false;
    }

    // ─── Blacklist System ────────────────────────────

    @Override
    public void addBlacklist(UUID playerUUID, String reason, String blacklistedBy) {
        String sql = "MERGE INTO auctify_blacklist (player_uuid,reason,blacklisted_by,created_at) VALUES (?,?,?,?)";
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

    @Override
    public boolean backup() {
        String fileName = plugin.getConfig().getString("storage.h2.file", "auctify.h2.db");
        File dbFile = new File(plugin.getDataFolder(), fileName);
        if (!dbFile.exists()) {
            logger.warning("Cannot backup: database file does not exist");
            return false;
        }

        // Create backup directory
        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        // Generate timestamped filename
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
        String backupName = "auctify_backup_" + timestamp + ".h2.db";
        File backupFile = new File(backupDir, backupName);

        // Copy database file
        try (java.nio.channels.FileChannel source = java.nio.channels.FileChannel.open(dbFile.toPath(),
                java.nio.file.StandardOpenOption.READ);
                java.nio.channels.FileChannel dest = java.nio.channels.FileChannel.open(backupFile.toPath(),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE)) {
            dest.transferFrom(source, 0, source.size());
            logger.info("Database backup created: " + backupFile.getName());

            // Clean up old backups if configured
            cleanupOldBackups(backupDir);

            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Database backup failed", e);
            return false;
        }
    }

    private void cleanupOldBackups(File backupDir) {
        int maxBackups = plugin.getConfig().getInt("storage.h2.backup.keep-count", 10);
        if (maxBackups <= 0)
            return;

        File[] backups = backupDir
                .listFiles((dir, name) -> name.startsWith("auctify_backup_") && name.endsWith(".h2.db"));
        if (backups == null || backups.length <= maxBackups)
            return;

        // Sort by last modified (oldest first)
        java.util.Arrays.sort(backups, java.util.Comparator.comparingLong(File::lastModified));

        // Delete oldest backups
        int toDelete = backups.length - maxBackups;
        for (int i = 0; i < toDelete; i++) {
            if (backups[i].delete()) {
                logger.fine("Deleted old backup: " + backups[i].getName());
            }
        }
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

    @Override
    public java.util.List<dev.auctify.auction.BidRecord> getPlayerBidHistory(UUID playerUUID) {
        java.util.List<dev.auctify.auction.BidRecord> bids = new java.util.ArrayList<>();
        String sql = "SELECT bidder_uuid, bidder_name, amount, bid_time FROM auctify_bid_history WHERE bidder_uuid = ? ORDER BY bid_time DESC";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                bids.add(new dev.auctify.auction.BidRecord(
                        UUID.fromString(rs.getString("bidder_uuid")),
                        rs.getString("bidder_name"),
                        rs.getDouble("amount"),
                        rs.getLong("bid_time")));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get player bid history", e);
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
        // FIX M-4: Query auctify_price_history table dengan item_material column (bukan
        // LIKE pada Base64)
        String sql = "SELECT MIN(final_price) as min_price, MAX(final_price) as max_price, " +
                "AVG(final_price) as avg_price, COUNT(*) as count " +
                "FROM auctify_price_history WHERE item_material = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, itemType);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new double[] {
                        rs.getDouble("min_price"),
                        rs.getDouble("max_price"),
                        rs.getDouble("avg_price"),
                        rs.getInt("count")
                };
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[Auctify] Failed to get price stats for " + itemType + ": " + e.getMessage());
        }
        return new double[] { 0, 0, 0, 0 };
    }

    // ─── Buy Order Implementation ───────────────────

    @Override
    public void saveBuyOrder(dev.auctify.auction.BuyOrder order) {
        String sql = "MERGE INTO auctify_buy_orders (id,buyer_uuid,buyer_name,item_type,amount,price_per_unit,created_at,expiry_time,active) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, order.getId());
            ps.setString(2, order.getBuyerUUID().toString());
            ps.setString(3, order.getBuyerName());
            ps.setString(4, order.getItemType().name());
            ps.setInt(5, order.getAmount());
            ps.setDouble(6, order.getPricePerUnit());
            ps.setLong(7, order.getCreatedAt());
            ps.setLong(8, order.getExpiryTime());
            ps.setInt(9, order.isActive() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save buy order", e);
        }
    }

    @Override
    public void deleteBuyOrder(String orderId) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM auctify_buy_orders WHERE id=?")) {
            ps.setString(1, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete buy order", e);
        }
    }

    @Override
    public java.util.List<dev.auctify.auction.BuyOrder> getAllBuyOrders() {
        java.util.List<dev.auctify.auction.BuyOrder> res = new java.util.ArrayList<>();
        try (Connection c = dataSource.getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM auctify_buy_orders WHERE active=1")) {
            while (rs.next()) {
                dev.auctify.auction.BuyOrder order = deserializeBuyOrder(rs);
                if (order != null)
                    res.add(order);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get buy orders", e);
        }
        return res;
    }

    @Override
    public java.util.List<dev.auctify.auction.BuyOrder> getBuyOrdersByPlayer(UUID playerUUID) {
        java.util.List<dev.auctify.auction.BuyOrder> res = new java.util.ArrayList<>();
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c
                        .prepareStatement("SELECT * FROM auctify_buy_orders WHERE buyer_uuid=? AND active=1")) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dev.auctify.auction.BuyOrder order = deserializeBuyOrder(rs);
                    if (order != null)
                        res.add(order);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get player buy orders", e);
        }
        return res;
    }

    private dev.auctify.auction.BuyOrder deserializeBuyOrder(ResultSet rs) throws SQLException {
        try {
            String id = rs.getString("id");
            UUID buyerUUID = UUID.fromString(rs.getString("buyer_uuid"));
            String buyerName = rs.getString("buyer_name");
            org.bukkit.Material itemType = org.bukkit.Material.valueOf(rs.getString("item_type"));
            int amount = rs.getInt("amount");
            double pricePerUnit = rs.getDouble("price_per_unit");
            long createdAt = rs.getLong("created_at");
            long expiryTime = rs.getLong("expiry_time");

            dev.auctify.auction.BuyOrder order = new dev.auctify.auction.BuyOrder(
                    id, buyerUUID, buyerName, itemType, amount, pricePerUnit, createdAt, expiryTime);
            order.setActive(rs.getInt("active") == 1);
            return order;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("H2 storage shutdown complete");
        }
    }

    // ─── Watchlist Implementation ───────────────────

    @Override
    public void addToWatchlist(UUID playerUUID, String listingId) {
        String sql = "INSERT INTO auctify_watchlist (player_uuid,listing_id,added_at) VALUES (?,?,?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, listingId);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to add to watchlist", e);
        }
    }

    @Override
    public void removeFromWatchlist(UUID playerUUID, String listingId) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c
                        .prepareStatement("DELETE FROM auctify_watchlist WHERE player_uuid=? AND listing_id=?")) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, listingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to remove from watchlist", e);
        }
    }

    @Override
    public boolean isInWatchlist(UUID playerUUID, String listingId) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c
                        .prepareStatement("SELECT 1 FROM auctify_watchlist WHERE player_uuid=? AND listing_id=?")) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, listingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to check watchlist", e);
        }
        return false;
    }

    @Override
    public java.util.List<String> getWatchlist(UUID playerUUID) {
        java.util.List<String> res = new java.util.ArrayList<>();
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT listing_id FROM auctify_watchlist WHERE player_uuid=? ORDER BY added_at DESC")) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(rs.getString("listing_id"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get watchlist", e);
        }
        return res;
    }

    @Override
    public void clearWatchlist(UUID playerUUID) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM auctify_watchlist WHERE player_uuid=?")) {
            ps.setString(1, playerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to clear watchlist", e);
        }
    }

    // ─── Pending Buy Order Deliveries ───────────

    @Override
    public void addPendingBuyDelivery(UUID playerUUID, org.bukkit.inventory.ItemStack item, String orderId) {
        String sql = "INSERT INTO auctify_pending_buy_deliveries (player_uuid,item_data,order_id,created_at) VALUES (?,?,?,?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, dev.auctify.util.ItemUtil.serializeToBase64(item));
            ps.setString(3, orderId);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save pending buy delivery", e);
        }
    }

    @Override
    public java.util.List<org.bukkit.inventory.ItemStack> getPendingBuyDeliveries(UUID playerUUID) {
        java.util.List<org.bukkit.inventory.ItemStack> res = new java.util.ArrayList<>();
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c
                        .prepareStatement("SELECT item_data FROM auctify_pending_buy_deliveries WHERE player_uuid=?")) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    org.bukkit.inventory.ItemStack item = dev.auctify.util.ItemUtil
                            .deserializeFromBase64(rs.getString("item_data"));
                    if (item != null)
                        res.add(item);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get pending buy deliveries", e);
        }
        return res;
    }

    @Override
    public void clearPendingBuyDeliveries(UUID playerUUID) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c
                        .prepareStatement("DELETE FROM auctify_pending_buy_deliveries WHERE player_uuid=?")) {
            ps.setString(1, playerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to clear pending buy deliveries", e);
        }
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
        String sql = "MERGE INTO auctify_auto_bid (player_uuid, player_name, listing_id, max_bid_amount, created_at) VALUES (?, ?, ?, ?, ?)";
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
    public java.util.List<dev.auctify.auction.AutoBid> getAutoBidsForPlayer(UUID playerUUID) {
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
    public void deleteAutoBid(String listingId, UUID playerUUID) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c
                        .prepareStatement("DELETE FROM auctify_auto_bid WHERE listing_id=? AND player_uuid=?")) {
            ps.setString(1, listingId);
            ps.setString(2, playerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete auto-bid", e);
        }
    }

    @Override
    public void deleteAutoBidsForListing(String listingId) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM auctify_auto_bid WHERE listing_id=?")) {
            ps.setString(1, listingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete auto-bids for listing", e);
        }
    }

    @Override
    public void clearAutoBidsForPlayer(UUID playerUUID) {
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM auctify_auto_bid WHERE player_uuid=?")) {
            ps.setString(1, playerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to clear auto-bids for player", e);
        }
    }
}
