package dev.auctify.util;

import dev.auctify.Auctify;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages caching for frequently accessed data to improve performance.
 * Implements time-based expiration and size limits.
 */
public class CacheManager {

    private final Auctify plugin;
    private final Map<String, CacheEntry> cache;
    private final long defaultTTL; // Time to live in milliseconds
    private final int maxSize;

    public CacheManager(Auctify plugin, long defaultTTLMinutes, int maxSize) {
        this.plugin = plugin;
        this.defaultTTL = TimeUnit.MINUTES.toMillis(defaultTTLMinutes);
        this.maxSize = maxSize;
        this.cache = new ConcurrentHashMap<>();

        // Start cleanup task
        startCleanupTask();
    }

    /**
     * Caches a value with the default TTL.
     */
    public void put(String key, Object value) {
        put(key, value, defaultTTL);
    }

    /**
     * Caches a value with a custom TTL.
     */
    public void put(String key, Object value, long ttlMillis) {
        if (cache.size() >= maxSize) {
            evictOldest();
        }
        cache.put(key, new CacheEntry(value, System.currentTimeMillis() + ttlMillis));
    }

    /**
     * Retrieves a value from cache.
     * Returns null if not found or expired.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return (T) entry.value;
    }

    /**
     * Checks if a key exists in cache and is not expired.
     */
    public boolean contains(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }
        return true;
    }

    /**
     * Removes a specific entry from cache.
     */
    public void remove(String key) {
        cache.remove(key);
    }

    /**
     * Clears all cached data.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Gets the current cache size.
     */
    public int size() {
        return cache.size();
    }

    /**
     * Evicts the oldest entry when cache is full.
     */
    private void evictOldest() {
        String oldestKey = null;
        long oldestExpiry = Long.MAX_VALUE;

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().expiry < oldestExpiry) {
                oldestExpiry = entry.getValue().expiry;
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }

    /**
     * Starts a periodic cleanup task to remove expired entries.
     */
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            int removed = 0;

            Iterator<Map.Entry<String, CacheEntry>> it = cache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, CacheEntry> entry = it.next();
                if (entry.getValue().isExpired()) {
                    it.remove();
                    removed++;
                }
            }

            if (removed > 0) {
                plugin.getLogger().fine("§7[Cache] Cleaned up " + removed + " expired entries.");
            }
        }, 1200L, 1200L); // Run every minute (1200 ticks)
    }

    /**
     * Cache entry with expiration time.
     */
    private static class CacheEntry {
        final Object value;
        final long expiry;

        CacheEntry(Object value, long expiry) {
            this.value = value;
            this.expiry = expiry;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiry;
        }
    }

    /**
     * Cache keys for common data.
     */
    public static class Keys {
        public static String playerStats(UUID playerUUID) {
            return "player_stats:" + playerUUID;
        }

        public static String listing(String listingId) {
            return "listing:" + listingId;
        }

        public static String categoryStats(String category) {
            return "category_stats:" + category;
        }

        public static String marketTrend() {
            return "market_trend";
        }
    }
}
