package dev.auctify;

import dev.auctify.auction.AuctionManager;
import dev.auctify.commands.AuctifyCommand;
import dev.auctify.commands.TabCompleter;
import dev.auctify.economy.EconomyManager;
import dev.auctify.gui.*;
import dev.auctify.listeners.*;
import dev.auctify.scheduler.AuctionExpiryTask;
import dev.auctify.storage.*;
import dev.auctify.setup.SetupWizard;
import dev.auctify.util.ConfigUtil;
import dev.auctify.util.DependencyManager;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Main plugin class for Auctify — a professional live auction house plugin.
 * Initializes all managers in dependency order, registers commands and
 * listeners,
 * and handles graceful shutdown with item preservation.
 *
 * @author Jephyruu
 * @version 1.0.0
 */
public class Auctify extends JavaPlugin {

    /** Economy abstraction wrapping Vault. */
    private EconomyManager economyManager;

    /** Persistence backend (memory, SQLite, or MySQL). */
    private StorageManager storageManager;

    /** Core auction logic coordinator. */
    private AuctionManager auctionManager;

    /** Setup wizard for first-run configuration. */
    private SetupWizard setupWizard;

    /** GUI state tracker for open inventories. */
    private GUIManager guiManager;

    /** Main auction house GUI builder. */
    private AuctionGUI auctionGUI;

    /** Bid confirmation GUI builder. */
    private ConfirmBidGUI confirmBidGUI;

    /** Item detail GUI builder. */
    private ItemDetailGUI itemDetailGUI;

    /** Manage listing GUI builder. */
    private dev.auctify.gui.ManageListingGUI manageListingGUI;

    /** Chat bid input listener (needs direct access for startBidInput). */
    private ChatBidListener chatBidListener;

    /** Discord Webhook util. */
    private dev.auctify.util.DiscordWebhookUtil discordWebhookUtil;

    /** Claim/Mailbox GUI builder. */
    private ClaimGUI claimGUI;

    /** Shulker preview GUI builder. */
    private ShulkerPreviewGUI shulkerPreviewGUI;

    /** Rating GUI builder. */
    private RateGUI rateGUI;

    /** Admin moderation GUI builder. */
    private AdminGUI adminGUI;

    /** The expiry task for cancellation on disable. */
    private AuctionExpiryTask expiryTask;

    /** The auto-save task. */
    private org.bukkit.scheduler.BukkitTask autoSaveTask;

    // TODO: Add per-world or per-region auction house support
    // TODO: Add item blacklist (prevent listing certain materials)
    // TODO: Add GUI pagination animation (smooth item transition)
    // TODO: Add Discord webhook notification on auction end
    // TODO: Add PlaceholderAPI support for leaderboard placeholders

    /**
     * Called when the plugin is enabled. Initializes everything in dependency
     * order:
     * Config → Banner → Dependencies → Economy → Storage → AuctionManager → GUI →
     * Commands → Listeners → Tasks
     */
    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        // Save default config if not present
        saveDefaultConfig();

        // Print the startup ASCII art banner
        printBanner();

        // Initialize utility classes with plugin reference
        MessageUtil.init(this);
        ConfigUtil.init(this);

        // Register crash handler
        new dev.auctify.util.CrashHandler(this).register();

        // Check and download missing dependencies
        getLogger().info("§e⚡ §7Checking dependencies...");
        DependencyManager depManager = new DependencyManager(this);
        boolean depsOk = depManager.checkAndDownload();

        if (!depsOk) {
            getLogger().severe("§c═══════════════════════════════════════════════");
            getLogger().severe("§c  Auctify is DISABLING due to missing required");
            getLogger().severe("§c  dependencies. Install them and restart.");
            getLogger().severe("§c═══════════════════════════════════════════════");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers in dependency order
        getLogger().info("§e⚡ §7Initializing economy manager...");
        economyManager = new EconomyManager(this);

        getLogger().info("§e⚡ §7Initializing storage manager...");
        storageManager = createStorageManager();
        storageManager.initialize();

        getLogger().info("§e⚡ §7Initializing auction manager...");
        auctionManager = new AuctionManager(this, economyManager, storageManager);

        // All managers ready — now safe to resolve expired listings that need economy
        auctionManager.resolveExpiredOnStartup();

        getLogger().info("§e⚡ §7Initializing GUI system...");
        guiManager = new GUIManager();
        auctionGUI = new AuctionGUI(this);
        confirmBidGUI = new ConfirmBidGUI(this);
        itemDetailGUI = new ItemDetailGUI(this);
        manageListingGUI = new ManageListingGUI(this);
        claimGUI = new ClaimGUI(this);
        shulkerPreviewGUI = new ShulkerPreviewGUI(this);
        rateGUI = new RateGUI(this);
        adminGUI = new AdminGUI(this);

        discordWebhookUtil = new dev.auctify.util.DiscordWebhookUtil(this);

        setupWizard = new SetupWizard(this);

        // Register commands
        registerCommands();

        // Register event listeners
        registerListeners();

        // Start the expiry task (runs every 20 ticks = 1 second)
        expiryTask = new AuctionExpiryTask(this);
        expiryTask.runTaskTimer(this, 20L, 20L);

        // Start auto-save task
        int autoSaveMinutes = getConfig().getInt("storage.auto-save-interval", 5);
        if (autoSaveMinutes > 0) {
            long autoSaveTicks = autoSaveMinutes * 60L * 20L;
            autoSaveTask = getServer().getScheduler().runTaskTimer(this, () -> {
                int saved = auctionManager.saveAllListings();
                getLogger().info("Auto-save: " + saved + " active listings saved.");
            }, autoSaveTicks, autoSaveTicks);
        }

        // Start automatic backup task (SQLite only)
        if (getConfig().getBoolean("storage.sqlite.backup.enabled", true)) {
            int backupMinutes = getConfig().getInt("storage.sqlite.backup.interval", 60);
            if (backupMinutes > 0) {
                long backupTicks = backupMinutes * 60L * 20L;
                getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                    boolean success = storageManager.backup();
                    if (success) {
                        getLogger().info("§aAutomatic database backup completed successfully.");
                    } else {
                        getLogger().warning("§cAutomatic database backup failed! Check logs for details.");
                    }
                }, backupTicks, backupTicks);
                getLogger().info("§e⚡ §7Automatic backup enabled (every " + backupMinutes + " minutes)");
            }
        }

        // Register PlaceholderAPI expansion if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new dev.auctify.hook.AuctifyPlaceholderExpansion(this).register();
            getLogger().info("§e⚡ §7PlaceholderAPI expansion registered.");
        }

        // Initialize the public API
        AuctifyAPI.init(this);

        long elapsed = System.currentTimeMillis() - startTime;

        // Print startup summary
        printStartupSummary(elapsed);

        // Warn if economy is unavailable
        if (!economyManager.isAvailable()) {
            getLogger().warning("Economy is not available! Sell and bid commands will be disabled.");
            getLogger().warning("Install Vault + an economy provider (e.g., EssentialsX) to enable.");
        }
    }

    /**
     * Called when the plugin is disabled. Gracefully shuts down all managers,
     * cancels tasks, and ensures no items are lost.
     */
    @Override
    public void onDisable() {
        // Cancel the expiry task
        if (expiryTask != null && !expiryTask.isCancelled()) {
            expiryTask.cancel();
        }

        // Cancel the auto-save task
        if (autoSaveTask != null && !autoSaveTask.isCancelled()) {
            autoSaveTask.cancel();
        }

        // Gracefully resolve/return all active listings
        if (auctionManager != null) {
            getLogger().info("Resolving active listings before shutdown...");
            auctionManager.shutdown();
        }

        // Flush and close storage
        if (storageManager != null) {
            storageManager.shutdown();
        }

        // Print shutdown banner
        Logger log = getLogger();
        log.info("");
        log.info("§c§l  ✦ Auctify §7v" + getPluginMeta().getVersion() + " §cdisabled.");
        log.info("§7  All items have been safely preserved.");
        log.info("");
    }

    /**
     * Prints the ASCII art startup banner to the console.
     * Uses the logger to ensure proper formatting in Paper's console.
     */
    private void printBanner() {
        Logger log = getLogger();
        log.info("");
        log.info("§6    ___               __  _ ____      ");
        log.info("§6   /   | __  __ _____/ /_(_) __/__  __");
        log.info("§6  / /| |/ / / / ___/ __/ / /_/ / / / /");
        log.info("§e / ___ / /_/ / /__/ /_/ / __/ / /_/ / ");
        log.info("§e/_/  |_\\__,_/\\___/\\__/_/_/ /_/\\__, /  ");
        log.info("§e                            /____/    ");
        log.info("");
        log.info("§8  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("§7   Live Auction House §8| §fv" + getPluginMeta().getVersion());
        log.info("§7   Author§8: §eJephyruu §8| §7Paper §f1.18+");
        log.info("§8  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("");
    }

    /**
     * Prints a summary of the startup process including timing and status.
     *
     * @param elapsedMs the time taken to enable the plugin in milliseconds
     */
    private void printStartupSummary(long elapsedMs) {
        Logger log = getLogger();
        String storageType = getConfig().getString("storage.type", "sqlite").toUpperCase();
        int listings = auctionManager.getActiveListings().size();
        String economyStatus = economyManager.isAvailable() ? "§a✔ Connected" : "§c✗ Unavailable";
        String discordWebhook = getConfig().getString("discord.webhook-url", "");
        String discordStatus = !discordWebhook.isEmpty() ? "§a✔ Connected" : "§c✗ Disabled";

        log.info("");
        log.info("§8|==================================================");
        log.info("§8| §a§l✓ §fAuctify enabled successfully!");
        log.info("§8|==================================================");
        log.info("§8| §7Time:§8    §f" + elapsedMs + "ms");
        log.info("§8| §7Storage:§8  §f" + storageType);
        log.info("§8| §7Economy:§8  " + economyStatus);
        log.info("§8| §7Discord:§8  " + discordStatus);
        log.info("§8| §7Loaded:§8   §f" + listings + " listing(s)");
        log.info("§8|==================================================");
        log.info("");
    }

    /**
     * Pads a string for log alignment (strips color codes for length calculation).
     */
    private String padLog(String text, int width) {
        String plain = text.replaceAll("§[0-9a-fk-or]", "");
        int padWidth = width + (text.length() - plain.length());
        if (text.length() >= padWidth)
            return text;
        return text + " ".repeat(padWidth - text.length());
    }

    /**
     * Creates the appropriate StorageManager based on the config setting.
     *
     * @return the configured storage manager instance
     */
    private StorageManager createStorageManager() {
        String type = getConfig().getString("storage.type", "sqlite").toLowerCase();
        return switch (type) {
            case "mysql" -> {
                getLogger().info("  Using MySQL storage backend.");
                yield new MySQLStorage(this);
            }
            case "memory" -> {
                getLogger().warning("  Using in-memory storage. Data will be lost on restart!");
                yield new MemoryStorage();
            }
            default -> {
                getLogger().info("  Using SQLite storage backend.");
                yield new SQLiteStorage(this);
            }
        };
    }

    /**
     * Registers the /ac command and its tab completer.
     */
    private void registerCommands() {
        PluginCommand acCommand = getCommand("ac");
        if (acCommand != null) {
            AuctifyCommand commandExecutor = new AuctifyCommand(this);
            acCommand.setExecutor(commandExecutor);
            acCommand.setTabCompleter(new TabCompleter(this));
        } else {
            getLogger().severe("Failed to register /ac command! Check plugin.yml.");
        }
    }

    /**
     * Registers all event listeners with the plugin manager.
     */
    private void registerListeners() {
        chatBidListener = new ChatBidListener(this);
        getServer().getPluginManager().registerEvents(new GUIClickListener(this), this);
        getServer().getPluginManager().registerEvents(chatBidListener, this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(this), this);
    }

    /** @return the economy manager */
    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    /** @return the storage manager */
    public StorageManager getStorageManager() {
        return storageManager;
    }

    /** @return the auction manager */
    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    /** @return the setup wizard */
    public SetupWizard getSetupWizard() {
        return setupWizard;
    }

    /** @return the GUI state manager */
    public GUIManager getGUIManager() {
        return guiManager;
    }

    /** @return the main auction GUI builder */
    public AuctionGUI getAuctionGUI() {
        return auctionGUI;
    }

    /** @return the bid confirmation GUI builder */
    public ConfirmBidGUI getConfirmBidGUI() {
        return confirmBidGUI;
    }

    /** @return the item detail GUI builder */
    public ItemDetailGUI getItemDetailGUI() {
        return itemDetailGUI;
    }

    /** @return the manage listing GUI builder */
    public dev.auctify.gui.ManageListingGUI getManageListingGUI() {
        return manageListingGUI;
    }

    /** @return the chat bid listener (for starting bid input mode) */
    public ChatBidListener getChatBidListener() {
        return chatBidListener;
    }

    public dev.auctify.util.DiscordWebhookUtil getDiscordWebhookUtil() {
        return discordWebhookUtil;
    }

    /** @return the claim/mailbox GUI builder */
    public ClaimGUI getClaimGUI() {
        return claimGUI;
    }

    /** @return the shulker preview GUI builder */
    public ShulkerPreviewGUI getShulkerPreviewGUI() {
        return shulkerPreviewGUI;
    }

    /** @return the rating GUI builder */
    public RateGUI getRateGUI() {
        return rateGUI;
    }

    /** @return the admin GUI builder */
    public AdminGUI getAdminGUI() {
        return adminGUI;
    }
}
