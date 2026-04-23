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
 * SQLite implementation of {@link StorageManager} using HikariCP connection pooling.
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
        if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("Auctify-SQLite");
        dataSource = new HikariDataSource(config);

        executeSchema();
        logger.info("SQLite storage initialized: " + dbFile.getAbsolutePath());
    }

    private void executeSchema() {
        try (InputStream is = plugin.getResource("schema/sqlite_schema.sql")) {
            if (is == null) { logger.severe("sqlite_schema.sql not found!"); return; }
            String schema = new String(is.readAllBytes());
            try (Connection conn = dataSource.getConnection()) {
                for (String s : schema.split(";")) {
                    String t = s.trim();
                    if (!t.isEmpty()) try (Statement st = conn.createStatement()) { st.execute(t); }
                }
            }
        } catch (IOException | SQLException e) { logger.log(Level.SEVERE, "Schema exec failed", e); }
    }

    @Override
    public void saveListing(AuctionListing l) {
        // Store remaining seconds instead of absolute endTime so timer pauses when server is offline
        long remainingMs = Math.max(0, l.getEndTime() - System.currentTimeMillis());
        String sql = "INSERT OR REPLACE INTO auctify_listings (id,seller_uuid,seller_name,item_data,start_price,buyout_price,current_bid,top_bidder_uuid,top_bidder_name,created_at,end_time) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, l.getId()); ps.setString(2, l.getSellerUUID().toString());
            ps.setString(3, l.getSellerName()); ps.setString(4, ItemUtil.serializeToBase64(l.getItem()));
            ps.setDouble(5, l.getStartPrice()); ps.setDouble(6, l.getBuyoutPrice());
            ps.setDouble(7, l.getCurrentBid());
            ps.setString(8, l.getTopBidderUUID() != null ? l.getTopBidderUUID().toString() : null);
            ps.setString(9, l.getTopBidderName()); ps.setLong(10, l.getCreatedAt());
            ps.setLong(11, remainingMs); ps.executeUpdate();
        } catch (SQLException e) { logger.log(Level.SEVERE, "Save listing failed: " + l.getId(), e); }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteListing(String id) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM auctify_listings WHERE id=?")) {
            ps.setString(1, id); ps.executeUpdate();
        } catch (SQLException e) { logger.log(Level.SEVERE, "Delete listing failed: " + id, e); }
    }

    /** {@inheritDoc} */
    @Override
    public List<AuctionListing> getAllListings() {
        List<AuctionListing> res = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM auctify_listings")) {
            while (rs.next()) {
                AuctionListing l = deserializeListing(rs);
                if (l != null) res.add(l);
            }
        } catch (SQLException e) { logger.log(Level.SEVERE, "Load listings failed", e); }
        return res;
    }

    private AuctionListing deserializeListing(ResultSet rs) throws SQLException {
        ItemStack item = ItemUtil.deserializeFromBase64(rs.getString("item_data"));
        if (item == null) { logger.warning("Bad item data for listing " + rs.getString("id")); return null; }
        String tbuStr = rs.getString("top_bidder_uuid");
        UUID tbu = tbuStr != null ? UUID.fromString(tbuStr) : null;
        // end_time column now stores remaining milliseconds, convert back to absolute
        long remainingMs = rs.getLong("end_time");
        long absoluteEndTime = System.currentTimeMillis() + remainingMs;
        return new AuctionListing(rs.getString("id"), UUID.fromString(rs.getString("seller_uuid")),
                rs.getString("seller_name"), item, rs.getDouble("start_price"), rs.getDouble("buyout_price"),
                rs.getDouble("current_bid"), tbu, rs.getString("top_bidder_name"),
                rs.getLong("created_at"), absoluteEndTime);
    }

    /** {@inheritDoc} */
    @Override
    public void saveHistory(AuctionHistory h) {
        String sql = "INSERT OR REPLACE INTO auctify_history (id,seller_uuid,seller_name,winner_uuid,winner_name,item_data,start_price,final_price,tax_amount,resolved_at,reason) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, h.id()); ps.setString(2, h.sellerUUID().toString());
            ps.setString(3, h.sellerName());
            ps.setString(4, h.winnerUUID() != null ? h.winnerUUID().toString() : null);
            ps.setString(5, h.winnerName()); ps.setString(6, h.itemData());
            ps.setDouble(7, h.startPrice()); ps.setDouble(8, h.finalPrice());
            ps.setDouble(9, h.taxAmount()); ps.setLong(10, h.resolvedAt());
            ps.setString(11, h.reason()); ps.executeUpdate();
        } catch (SQLException e) { logger.log(Level.SEVERE, "Save history failed: " + h.id(), e); }
    }

    /** {@inheritDoc} */
    @Override
    public List<AuctionHistory> getHistory(UUID playerUUID, int limit) {
        List<AuctionHistory> res = new ArrayList<>();
        String sql = "SELECT * FROM auctify_history WHERE seller_uuid=? OR winner_uuid=? ORDER BY resolved_at DESC LIMIT ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String u = playerUUID.toString(); ps.setString(1, u); ps.setString(2, u); ps.setInt(3, limit);
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
        } catch (SQLException e) { logger.log(Level.SEVERE, "Load history failed", e); }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public void savePendingDelivery(UUID playerUUID, ItemStack item) {
        String sql = "INSERT INTO auctify_pending_deliveries (player_uuid,item_data,created_at) VALUES (?,?,?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString()); ps.setString(2, ItemUtil.serializeToBase64(item));
            ps.setLong(3, System.currentTimeMillis()); ps.executeUpdate();
        } catch (SQLException e) { logger.log(Level.SEVERE, "Save pending delivery failed", e); }
    }

    /** {@inheritDoc} */
    @Override
    public List<ItemStack> getPendingDeliveries(UUID playerUUID) {
        List<ItemStack> res = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT item_data FROM auctify_pending_deliveries WHERE player_uuid=?")) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { ItemStack i = ItemUtil.deserializeFromBase64(rs.getString("item_data")); if (i != null) res.add(i); }
            }
        } catch (SQLException e) { logger.log(Level.SEVERE, "Load pending deliveries failed", e); }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public void clearPendingDeliveries(UUID playerUUID) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM auctify_pending_deliveries WHERE player_uuid=?")) {
            ps.setString(1, playerUUID.toString()); ps.executeUpdate();
        } catch (SQLException e) { logger.log(Level.SEVERE, "Clear pending deliveries failed", e); }
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) { dataSource.close(); logger.info("SQLite pool closed."); }
    }
}
