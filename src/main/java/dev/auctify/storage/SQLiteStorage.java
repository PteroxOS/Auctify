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
 * SQLite implementation of {@link StorageManager} using HikariCP connection
 * pooling.
 */
public class SQLiteStorage implements StorageManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private HikariDataSource dataSource;

    /** @param plugin the main plugin instance */
    public SQLiteStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        String fileName = plugin.getConfig().getString("storage.sqlite.file", "auctify.db");
        File dbFile = new File(plugin.getDataFolder(), fileName);
        if (!dbFile.getParentFile().exists())
            dbFile.getParentFile().mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(0); // SQLite does not benefit from idle timeout
        config.setMaxLifetime(0); // SQLite connections can live indefinitely
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("Auctify-SQLite");
        dataSource = new HikariDataSource(config);

        executeSchema();
        migrateSchema();
        logger.info("SQLite storage initialized: " + dbFile.getAbsolutePath());
    }

    private void executeSchema() {
        try (InputStream is = plugin.getResource("schema/sqlite_schema.sql")) {
            if (is == null) {
                logger.severe("sqlite_schema.sql not found!");
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

    private void migrateSchema() {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            // Add bin_only column if missing (schema migration)
            try {
                st.execute("ALTER TABLE auctify_listings ADD COLUMN bin_only INTEGER NOT NULL DEFAULT 0");
            } catch (SQLException ignored) {
                // Column already exists
            }
            // Add tax_exempt column if missing (schema migration)
            try {
                st.execute("ALTER TABLE auctify_listings ADD COLUMN tax_exempt INTEGER NOT NULL DEFAULT 0");
            } catch (SQLException ignored) {
                // Column already exists
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Schema migration failed", e);
        }
    }

    @Override
    public void saveListing(AuctionListing l) {
        long remainingMs = Math.max(0, l.getEndTime() - System.currentTimeMillis());
        String sql = "INSERT OR REPLACE INTO auctify_listings (id,seller_uuid,seller_name,item_data,start_price,buyout_price,current_bid,top_bidder_uuid,top_bidder_name,created_at,end_time,bin_only,tax_exempt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
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

    /** {@inheritDoc} */
    @Override
    public void saveHistory(AuctionHistory h) {
        String sql = "INSERT OR REPLACE INTO auctify_history (id,seller_uuid,seller_name,winner_uuid,winner_name,item_data,start_price,final_price,tax_amount,resolved_at,reason) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
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
            logger.log(Level.SEVERE, "Load pending deliveries failed", e);
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

    // ─── Blacklist System ────────────────────────────

    @Override
    public void addBlacklist(UUID playerUUID, String reason, String blacklistedBy) {
        String sql = "INSERT OR REPLACE INTO auctify_blacklist (player_uuid,reason,blacklisted_by,created_at) VALUES (?,?,?,?)";
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

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("SQLite pool closed.");
        }
    }
}
