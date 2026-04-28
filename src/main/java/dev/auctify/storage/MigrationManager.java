package dev.auctify.storage;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages database migrations between different storage backends.
 * Supports migration from SQLite to H2, MySQL, and vice versa.
 */
public class MigrationManager {

    private final Auctify plugin;

    public MigrationManager(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Migrates data from source storage to target storage.
     * 
     * @param source The source storage manager
     * @param target The target storage manager
     * @return CompletableFuture with migration result
     */
    public CompletableFuture<MigrationResult> migrate(StorageManager source, StorageManager target) {
        return CompletableFuture.supplyAsync(() -> {
            MigrationResult result = new MigrationResult();
            long startTime = System.currentTimeMillis();

            plugin.getLogger().info("§e[Migration] Starting data migration...");

            try {
                // Migrate active listings via AuctionManager
                plugin.getLogger().info("§e[Migration] Migrating active listings...");
                List<AuctionListing> listings = plugin.getAuctionManager().getActiveListings();
                for (AuctionListing listing : listings) {
                    target.saveListing(listing);
                    result.listingsMigrated++;
                }
                plugin.getLogger().info("§a[Migration] Migrated " + result.listingsMigrated + " active listings.");

                // Migrate blacklist
                plugin.getLogger().info("§e[Migration] Migrating blacklist...");
                List<String[]> blacklist = source.getBlacklist();
                for (String[] entry : blacklist) {
                    if (entry.length > 0) {
                        try {
                            UUID uuid = UUID.fromString(entry[0]);
                            String reason = entry.length > 1 ? entry[1] : "Migrated from previous storage";
                            String blacklistedBy = entry.length > 2 ? entry[2] : "System";
                            target.addBlacklist(uuid, reason, blacklistedBy);
                            result.blacklistMigrated++;
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("§c[Migration] Invalid UUID in blacklist: " + entry[0]);
                        }
                    }
                }
                plugin.getLogger().info("§a[Migration] Migrated " + result.blacklistMigrated + " blacklist entries.");

                // Note: History and ratings migration would require additional storage methods
                plugin.getLogger().info(
                        "§e[Migration] History and ratings migration skipped (requires additional storage methods).");

                result.success = true;
                result.duration = System.currentTimeMillis() - startTime;

                plugin.getLogger().info("§a[Migration] Migration completed successfully in " + result.duration + "ms.");

            } catch (Exception e) {
                result.success = false;
                result.error = e.getMessage();
                result.duration = System.currentTimeMillis() - startTime;
                plugin.getLogger().log(Level.SEVERE, "§c[Migration] Migration failed: " + e.getMessage(), e);
            }

            return result;
        });
    }

    /**
     * Validates if a migration is possible between two storage types.
     * 
     * @param sourceType Source storage type
     * @param targetType Target storage type
     * @return true if migration is supported
     */
    public boolean isMigrationSupported(String sourceType, String targetType) {
        // All combinations are supported
        return true;
    }

    /**
     * Creates a backup of the current database before migration.
     * 
     * @param storage The storage manager to backup
     * @return true if backup was successful
     */
    public boolean createBackup(StorageManager storage) {
        try {
            plugin.getLogger().info("§e[Migration] Creating database backup...");
            boolean success = storage.backup();
            if (success) {
                plugin.getLogger().info("§a[Migration] Backup created successfully.");
            } else {
                plugin.getLogger().warning("§c[Migration] Backup creation failed!");
            }
            return success;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "§c[Migration] Backup failed: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Result of a migration operation.
     */
    public static class MigrationResult {
        public boolean success;
        public int listingsMigrated;
        public int historyMigrated;
        public int blacklistMigrated;
        public int ratingsMigrated;
        public long duration;
        public String error;

        public String getSummary() {
            if (success) {
                return String.format("Migration successful: %d listings, %d blacklist entries in %dms",
                        listingsMigrated, blacklistMigrated, duration);
            } else {
                return "Migration failed: " + error;
            }
        }
    }
}
