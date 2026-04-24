package dev.auctify.setup;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Interactive chat-based setup wizard for first-time installation. Guides admin
 * through configuration with clickable options.
 */
public class SetupWizard implements Listener {

    private final Auctify plugin;
    private final Map<UUID, SetupState> activeSetups = new HashMap<>();
    private final Set<UUID> waitingForWebhookInput = new HashSet<>();

    public SetupWizard(Auctify plugin) {
        this.plugin = plugin;
        // Register this as a listener for chat events (webhook input protection)
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Dedicated chat listener for webhook input with HIGH priority. This provides a
     * second layer of protection to ensure webhook URLs are never broadcast to
     * other players. Cancels the event if player is in webhook input mode.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Check if player is waiting for webhook input
        if (waitingForWebhookInput.contains(player.getUniqueId())) {
            // Always cancel the event to prevent URL from being broadcast
            event.setCancelled(true);

            // Process the webhook URL privately
            String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

            // Handle skip
            if (message.equalsIgnoreCase("skip")) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    waitingForWebhookInput.remove(player.getUniqueId());
                    showStep9_Backup(player);
                    MessageUtil.sendPlain(player, "§7Skipped Discord webhook setup.");
                });
                return;
            }

            // Process webhook URL on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                onPlayerChat(player, message);
            });
        }
    }

    /**
     * Called when player sends chat message while in setup webhook input mode.
     */
    public void onPlayerChat(Player player, String message) {
        if (!waitingForWebhookInput.contains(player.getUniqueId()))
            return;

        // Cancel the chat event from broadcasting
        // This is called from ChatBidListener which cancels the event

        String url = message.trim();

        // Validate URL format
        if (!isValidDiscordWebhook(url)) {
            MessageUtil.sendPlain(player, "§cInvalid webhook URL!");
            MessageUtil.sendPlain(player, "§7URL must start with: §fhttps://discord.com/api/webhooks/");
            MessageUtil.sendPlain(player, "§ePlease enter a valid Discord webhook URL:");
            return;
        }

        // Test the webhook
        MessageUtil.sendPlain(player, "§eTesting webhook connection...");

        testDiscordWebhook(url, success -> {
            if (success) {
                waitingForWebhookInput.remove(player.getUniqueId());
                SetupState state = activeSetups.get(player.getUniqueId());
                if (state != null) {
                    state.discordWebhook = url;
                    MessageUtil.sendPlain(player, "§a✓ Webhook test successful!");
                    showStep9_Backup(player);
                }
            } else {
                MessageUtil.sendPlain(player, "§c✗ Webhook test failed!");
                MessageUtil.sendPlain(player, "§7Possible causes:");
                MessageUtil.sendPlain(player, "§7- Invalid webhook URL");
                MessageUtil.sendPlain(player, "§7- Discord rate limiting");
                MessageUtil.sendPlain(player, "§7- Network issues");
                MessageUtil.sendPlain(player, "");
                MessageUtil.sendPlain(player, "§eEnter a different webhook URL or type §fskip §eto skip:");
            }
        });
    }

    /**
     * Checks if player is currently in webhook input mode.
     */
    public boolean isWaitingForWebhookInput(Player player) {
        return waitingForWebhookInput.contains(player.getUniqueId());
    }

    /**
     * Skips webhook setup and proceeds to next step.
     */
    public void skipWebhookInput(Player player) {
        waitingForWebhookInput.remove(player.getUniqueId());
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state != null) {
            showStep9_Backup(player);
        }
    }

    private boolean isValidDiscordWebhook(String url) {
        return url.startsWith("https://discord.com/api/webhooks/") ||
                url.startsWith("https://discordapp.com/api/webhooks/");
    }

    private void testDiscordWebhook(String webhookUrl, java.util.function.Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = false;
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                // Send test payload
                String payload = "{\"content\":\"✓ Auctify webhook test successful!\"}";
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.getBytes("utf-8"));
                }

                int responseCode = conn.getResponseCode();
                success = (responseCode == 204 || responseCode == 200);
                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("[Auctify] Webhook test failed: " + e.getMessage());
            }

            final boolean result = success;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    /**
     * Checks if this is the first run and starts wizard if needed.
     */
    public void checkFirstRun() {
        if (plugin.getConfig().getBoolean("system.first-run", true)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Notify online admins
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isOp() || player.hasPermission("auctify.admin")) {
                        showWelcomeMessage(player);
                    }
                }
                // Also log to console
                plugin.getLogger().info("");
                plugin.getLogger().info("§e═══════════════════════════════════════════");
                plugin.getLogger().info("§e  Auctify Setup Wizard Ready!");
                plugin.getLogger().info("§e  Run §f/ac setup §efrom in-game or console");
                plugin.getLogger().info("§e═══════════════════════════════════════════");
                plugin.getLogger().info("");
            }, 40L); // 2 seconds after startup
        }
    }

    /**
     * Called when a player joins - shows welcome if first-run and admin.
     */
    public void onPlayerJoin(Player player) {
        if (plugin.getConfig().getBoolean("system.first-run", true)) {
            if (player.isOp() || player.hasPermission("auctify.admin")) {
                // Delay slightly to ensure player fully loaded
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        showWelcomeMessage(player);
                    }
                }, 20L); // 1 second delay
            }
        }
    }

    /**
     * Starts the setup wizard for a player.
     */
    public void startSetup(Player player) {
        if (!player.isOp() && !player.hasPermission("auctify.admin")) {
            MessageUtil.sendPlain(player, "§cYou don't have permission to run the setup wizard.");
            return;
        }

        activeSetups.put(player.getUniqueId(), new SetupState());
        showStep1_Language(player);
    }

    /**
     * Starts console-based setup (simplified text input).
     */
    public void startConsoleSetup(CommandSender sender) {
        plugin.getLogger().info("Console setup not yet implemented. Please edit config.yml directly.");
    }

    private void showWelcomeMessage(Player player) {
        MessageUtil.sendPlain(player, "");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|                                                |");
        MessageUtil.sendPlain(player, "§8|       §6✦ §e§lWELCOME TO AUCTIFY §6✦");
        MessageUtil.sendPlain(player, "§8|                                                |");
        MessageUtil.sendPlain(player, "§8| §7This is your first time installing Auctify!");
        MessageUtil.sendPlain(player, "§8| §7Would you like to run the setup wizard?");
        MessageUtil.sendPlain(player, "§8|                                                |");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "");

        // Clickable buttons
        TextComponent yesButton = new TextComponent("    ");
        TextComponent yes = new TextComponent("[ §a§lYES, Setup Now §r]");
        yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ac setup"));
        yes.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Click to start setup wizard").create()));
        yesButton.addExtra(yes);

        TextComponent noButton = new TextComponent("    ");
        TextComponent no = new TextComponent("[ §c§lNO, Use Defaults §r]");
        no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ac setup skip"));
        no.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Skip setup and use default settings").create()));
        noButton.addExtra(no);

        player.spigot().sendMessage(yesButton);
        player.spigot().sendMessage(noButton);
        MessageUtil.sendPlain(player, "");
    }

    // ========== STEP 1: LANGUAGE ==========
    private void showStep1_Language(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        MessageUtil.sendPlain(player, "");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|          §6§lSETUP WIZARD §7(Step 1/9)            §8|");
        MessageUtil.sendPlain(player, "§8|                                                §8|");
        MessageUtil.sendPlain(player, "§8|  §eSelect your language / Dil seçin:           §8|");
        MessageUtil.sendPlain(player, "§8|                                                §8|");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "");

        sendClickableOption(player, "  [ §aEnglish §r]", "en", "Click to select English", "/ac setup step1 en");
        sendClickableOption(player, "  [ §2Indonesia §r]", "id", "Bahasa Indonesia", "/ac setup step1 id");
        sendClickableOption(player, "  [ §6Español §r]", "es", "Spanish", "/ac setup step1 es");
        sendClickableOption(player, "  [ §aPortuguês §r]", "pt_br", "Portuguese", "/ac setup step1 pt_br");
        sendClickableOption(player, "  [ §9Русский §r]", "ru", "Russian", "/ac setup step1 ru");
        sendClickableOption(player, "  [ §eDeutsch §r]", "de", "German", "/ac setup step1 de");
        sendClickableOption(player, "  [ §bFrançais §r]", "fr", "French", "/ac setup step1 fr");
        sendClickableOption(player, "  [ §cPolski §r]", "pl", "Polish", "/ac setup step1 pl");
        sendClickableOption(player, "  [ §6Türkçe §r]", "tr", "Turkish", "/ac setup step1 tr");
        sendClickableOption(player, "  [ §c中文 §r]", "zh_cn", "Chinese", "/ac setup step1 zh_cn");
        sendClickableOption(player, "  [ §d日本語 §r]", "ja", "Japanese", "/ac setup step1 ja");
        sendClickableOption(player, "  [ §b한국어 §r]", "ko", "Korean", "/ac setup step1 ko");
        sendClickableOption(player, "  [ §aNederlands §r]", "nl", "Dutch", "/ac setup step1 nl");
        MessageUtil.sendPlain(player, "");
    }

    // ========== STEP 2: STORAGE ==========
    private void showStep2_Storage(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        String title = state.locale.equals("id") ? "SETUP WIZARD (Step 2/9)" : "SETUP WIZARD (Step 2/9)";
        String prompt = state.locale.equals("id") ? "Pilih tipe penyimpanan:" : "Choose storage type:";

        MessageUtil.sendPlain(player, "");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|           §6§l" + title);
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|  §e" + padRight(prompt, 38));
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "");

        sendClickableOption(player, "  [ §aSQLite §r]§7 - File-based, easy setup", "sqlite",
                "Best for small-medium servers", "/ac setup step2 sqlite");
        sendClickableOption(player, "  [ §bMySQL §r]§7 - Database server", "mysql",
                "Best for large servers", "/ac setup step2 mysql");
        sendClickableOption(player, "  [ §eMemory §r]§7 - No persistence (testing)", "memory",
                "Data lost on restart!", "/ac setup step2 memory");
        MessageUtil.sendPlain(player, "");
    }

    // ========== STEP 3: ECONOMY TAX ==========
    private void showStep3_Economy(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        boolean isId = state.locale.equals("id");
        String title = isId ? "SETUP WIZARD (Step 3/9)" : "SETUP WIZARD (Step 3/9)";
        String prompt = isId ? "Pengaturan pajak penjual:" : "Seller tax settings:";

        MessageUtil.sendPlain(player, "");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|           §6§l" + title);
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|  §e" + padRight(prompt, 38));
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "");

        sendClickableOption(player, "  [ §a0% §r]§7 - No tax", "0",
                isId ? "Tidak ada pajak" : "No tax on sales", "/ac setup step3 0");
        sendClickableOption(player, "  [ §e5% §r]§7 - Recommended", "5",
                isId ? "Pajak 5% (default)" : "5% tax (default)", "/ac setup step3 5");
        sendClickableOption(player, "  [ §c10% §r]§7 - High tax", "10",
                isId ? "Pajak 10%" : "10% tax", "/ac setup step3 10");
        sendClickableOption(player, "  [ §bCustom §r]§7 - Enter percentage", "custom",
                isId ? "Masukkan angka 0-100" : "Enter 0-100", "/ac setup step3 custom");
        MessageUtil.sendPlain(player, "");
        MessageUtil.sendPlain(player, isId ? "§7§oPajak: persen dari harga jual yang diambil server"
                : "§7§oTax: percentage taken from seller's earnings");
    }

    // ========== STEP 4: GENERAL ==========
    private void showStep4_General(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        boolean isId = state.locale.equals("id");
        String title = isId ? "SETUP WIZARD (Step 4/9)" : "SETUP WIZARD (Step 4/9)";
        String prompt = isId ? "Pengaturan umum lelang:" : "General auction settings:";

        MessageUtil.sendPlain(player, "");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|           §6§l" + title);
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|  §e" + padRight(prompt, 38));
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "");

        sendClickableOption(player, "  [ §a5 min §r]§7 - Quick auctions", "300",
                isId ? "Durasi 5 menit" : "5 minutes duration", "/ac setup step4 300");
        sendClickableOption(player, "  [ §e15 min §r]§7 - Standard", "900",
                isId ? "Durasi 15 menit (default)" : "15 minutes (default)", "/ac setup step4 900");
        sendClickableOption(player, "  [ §b1 hour §r]§7 - Long auctions", "3600",
                isId ? "Durasi 1 jam" : "1 hour duration", "/ac setup step4 3600");
        MessageUtil.sendPlain(player, "");
    }

    // ========== STEP 5: GUI ==========
    private void showStep5_GUI(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        boolean isId = state.locale.equals("id");
        String title = isId ? "SETUP WIZARD (Step 5/9)" : "SETUP WIZARD (Step 5/9)";
        String prompt = isId ? "Waktu timeout input bid:" : "Bid input timeout:";
        String desc = isId ? "Waktu untuk memasukkan harga bid via chat" : "Time to enter bid price via chat";

        MessageUtil.sendPlain(player, "");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|           §6§l" + title);
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|  §e" + padRight(prompt, 38));
        MessageUtil.sendPlain(player, "§8|  §7" + padRight(desc, 38));
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "");

        sendClickableOption(player, "  [ §a15 sec §r]§7 - Fast", "15",
                isId ? "15 detik" : "15 seconds", "/ac setup step5 15");
        sendClickableOption(player, "  [ §e30 sec §r]§7 - Standard (default)", "30",
                isId ? "30 detik (default)" : "30 seconds (default)", "/ac setup step5 30");
        sendClickableOption(player, "  [ §b60 sec §r]§7 - Relaxed", "60",
                isId ? "60 detik" : "60 seconds", "/ac setup step5 60");
        MessageUtil.sendPlain(player, "");
    }

    // ========== STEP 6: PER-WORLD ==========
    private void showStep6_PerWorld(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        boolean isId = state.locale.equals("id");
        String title = isId ? "SETUP WIZARD (Step 6/9)" : "SETUP WIZARD (Step 6/9)";
        String prompt = isId ? "Mode auction house per-world:" : "Per-world auction house mode:";

        MessageUtil.sendPlain(player, "");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|           §6§l" + title);
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|  §e" + padRight(prompt, 38));
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "");

        sendClickableOption(player, "  [ §aGlobal §r]§7 - All worlds share auction", "global",
                isId ? "Semua world pakai auction yang sama" : "All worlds use same auction house",
                "/ac setup step6 global");
        sendClickableOption(player, "  [ §bPer-World §r]§7 - Separate per world", "per-world",
                isId ? "Tiap world punya auction sendiri" : "Each world has separate auction house",
                "/ac setup step6 per-world");
        sendClickableOption(player, "  [ §cBlacklist §r]§7 - Disable in specific worlds", "blacklist",
                isId ? "Nonaktifkan di world tertentu" : "Disable auction in certain worlds",
                "/ac setup step6 blacklist");
        MessageUtil.sendPlain(player, "");
    }

    // ========== STEP 7: ADVANCED FEATURES ==========
    private void showStep7_Advanced(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        boolean isId = state.locale.equals("id");
        String title = isId ? "SETUP WIZARD (Step 7/9)" : "SETUP WIZARD (Step 7/9)";
        String prompt = isId ? "Fitur advanced (bisa diubah nanti):" : "Advanced features (can change later):";

        MessageUtil.sendPlain(player, "");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|           §6§l" + title);
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|  §e" + padRight(prompt, 38));
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "");

        sendClickableOption(player, "  [ §aStandard §r]§7 - No extra features", "standard",
                isId ? "Tanpa auto-relist & listing fee" : "No auto-relist or listing fee", "/ac setup step7 standard");
        sendClickableOption(player, "  [ §eAuto-Relist §r]§7 - Relist expired items", "relist",
                isId ? "Relist otomatis item yang expired" : "Auto-relist expired items with discount",
                "/ac setup step7 relist");
        sendClickableOption(player, "  [ §c+Listing Fee §r]§7 - Charge fee to list", "fee",
                isId ? "Kenakan biaya untuk buat listing" : "Charge percentage fee when listing",
                "/ac setup step7 fee");
        MessageUtil.sendPlain(player, "");
    }

    // ========== STEP 8: DISCORD ==========
    private void showStep8_Discord(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        boolean isId = state.locale.equals("id");
        String title = isId ? "SETUP WIZARD (Step 8/9)" : "SETUP WIZARD (Step 8/9)";
        String prompt = isId ? "Notifikasi Discord (opsional):" : "Discord notifications (optional):";

        MessageUtil.sendPlain(player, "");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|           §6§l" + title);
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|  §e" + padRight(prompt, 38));
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "");

        sendClickableOption(player, "  [ §cSkip §r]§7 - No Discord", "skip",
                isId ? "Lewati" : "Skip this step", "/ac setup step8 skip");
        sendClickableOption(player, "  [ §aEnable §r]§7 - Enter webhook URL", "enable",
                isId ? "Masukkan URL webhook" : "Enter webhook URL", "/ac setup step8 enable");
        MessageUtil.sendPlain(player, "");
        MessageUtil.sendPlain(player,
                "§7§o" + (isId ? "Webhook URL didapat dari channel Discord" : "Get webhook URL from Discord channel"));
    }

    // ========== STEP 9: BACKUP ==========
    private void showStep9_Backup(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        boolean isId = state.locale.equals("id");
        String title = isId ? "SETUP WIZARD (Step 9/9)" : "SETUP WIZARD (Step 9/9)";
        String prompt = isId ? "Pengaturan backup otomatis:" : "Automatic backup settings:";

        MessageUtil.sendPlain(player, "");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|           §6§l" + title);
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|  §e" + padRight(prompt, 38));
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "");

        sendClickableOption(player, "  [ §cDisable §r]§7 - No backup", "disable",
                isId ? "Matikan backup" : "Disable backup", "/ac setup step9 disable");
        sendClickableOption(player, "  [ §a1 hour §r]§7 - Frequent", "60",
                isId ? "Backup tiap 1 jam" : "Every 1 hour", "/ac setup step9 60");
        sendClickableOption(player, "  [ §e6 hours §r]§7 - Balanced", "360",
                isId ? "Backup tiap 6 jam" : "Every 6 hours", "/ac setup step9 360");
        sendClickableOption(player, "  [ §b24 hours §r]§7 - Daily", "1440",
                isId ? "Backup tiap 24 jam" : "Every 24 hours", "/ac setup step9 1440");
        MessageUtil.sendPlain(player, "");
    }

    // ========== COMPLETE ==========
    private void completeSetup(Player player) {
        SetupState state = activeSetups.remove(player.getUniqueId());
        if (state == null)
            return;

        // Apply all settings
        plugin.getConfig().set("general.language", state.locale);
        plugin.getConfig().set("storage.type", state.storage);
        plugin.getConfig().set("tax.percent", state.taxPercent);
        plugin.getConfig().set("tax.destination", "void");
        plugin.getConfig().set("general.default-duration", state.duration);
        plugin.getConfig().set("gui.bid-input-timeout", state.bidTimeout);

        // New v1.0.2 settings
        plugin.getConfig().set("worlds.mode", state.worldMode);
        plugin.getConfig().set("auto-relist.enabled", state.autoRelist);
        plugin.getConfig().set("listing-fee.enabled", state.listingFeePercent > 0);
        plugin.getConfig().set("listing-fee.percent", state.listingFeePercent);

        if (!state.discordWebhook.isEmpty()) {
            plugin.getConfig().set("discord.webhook-url", state.discordWebhook);
            plugin.getConfig().set("discord.enabled", true);
        }

        plugin.getConfig().set("storage.sqlite.backup.enabled", state.backupEnabled);
        plugin.getConfig().set("storage.sqlite.backup.interval", state.backupInterval);

        plugin.getConfig().set("system.first-run", false);
        plugin.saveConfig();

        // Reload to apply changes
        plugin.reloadConfig();

        boolean isId = state.locale.equals("id");
        MessageUtil.sendPlain(player, "");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|      §a§l✓ SETUP COMPLETED SUCCESSFULLY!");
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player,
                "§8|  §7" + padRight(isId ? "Semua pengaturan telah disimpan!" : "All settings saved!", 38));
        MessageUtil.sendPlain(player,
                "§8|  §7" + padRight(isId ? "Plugin akan di-reload..." : "Reloading plugin...", 38));
        MessageUtil.sendPlain(player, "§8|                                                ");
        MessageUtil.sendPlain(player, "§8|================================================");
        MessageUtil.sendPlain(player, "");

        // Log to console
        plugin.getLogger().info("[Auctify] Setup completed by " + player.getName() + " - reloading plugin...");

        // Reload via /ac reload command
        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ac reload");
            if (!success) {
                plugin.getLogger().warning("[Auctify] /ac reload command failed!");
                MessageUtil.sendPlain(player, "§c§l⚠ Reload failed! Please run §f/ac reload §cmanually.");
            }
        });
    }

    public void skipSetup(Player player) {
        plugin.getConfig().set("system.first-run", false);
        plugin.saveConfig();
        activeSetups.remove(player.getUniqueId());
        MessageUtil.sendPlain(player,
                "§7Setup skipped. Using default configuration. Run §f/ac setup §7anytime to configure.");
    }

    // ========== HANDLE INPUT ==========
    public void handleStep(Player player, int step, String value) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null) {
            MessageUtil.sendPlain(player, "§cSetup session expired. Run §f/ac setup §cto restart.");
            return;
        }

        switch (step) {
            case 1 -> {
                state.locale = value;
                showStep2_Storage(player);
            }
            case 2 -> {
                state.storage = value;
                showStep3_Economy(player);
            }
            case 3 -> {
                if (value.equals("custom")) {
                    MessageUtil.sendPlain(player, "§eEnter tax percentage (0-100):");
                    // Would need chat listener for custom input
                } else {
                    state.taxPercent = Integer.parseInt(value);
                    showStep4_General(player);
                }
            }
            case 4 -> {
                state.duration = Integer.parseInt(value);
                showStep5_GUI(player);
            }
            case 5 -> {
                state.bidTimeout = Integer.parseInt(value);
                showStep6_PerWorld(player);
            }
            case 6 -> {
                state.worldMode = value;
                showStep7_Advanced(player);
            }
            case 7 -> {
                switch (value) {
                    case "standard" -> {
                        state.autoRelist = false;
                        state.listingFeePercent = 0;
                    }
                    case "relist" -> {
                        state.autoRelist = true;
                        state.listingFeePercent = 0;
                    }
                    case "fee" -> {
                        state.autoRelist = false;
                        state.listingFeePercent = 5; // Default 5%
                    }
                }
                showStep8_Discord(player);
            }
            case 8 -> {
                if (value.equals("skip")) {
                    showStep9_Backup(player);
                } else {
                    waitingForWebhookInput.add(player.getUniqueId());
                    MessageUtil.sendPlain(player, "§ePlease enter your Discord webhook URL in chat:");
                    MessageUtil.sendPlain(player,
                            "§7(The URL should look like: §fhttps://discord.com/api/webhooks/...§7)");
                    MessageUtil.sendPlain(player, "§7Type §fskip §7to skip this step.");
                }
            }
            case 9 -> {
                if (value.equals("disable")) {
                    state.backupEnabled = false;
                } else {
                    state.backupEnabled = true;
                    state.backupInterval = Integer.parseInt(value);
                }
                completeSetup(player);
            }
        }
    }

    // ========== UTILITIES ==========
    private void sendClickableOption(Player player, String label, String value, String tooltip, String command) {
        TextComponent component = new TextComponent(label);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(tooltip).create()));
        player.spigot().sendMessage(component);
    }

    private String padRight(String text, int width) {
        if (text.length() >= width)
            return text.substring(0, width);
        return text + " ".repeat(width - text.length());
    }

    // ========== STATE CLASS ==========
    private static class SetupState {
        String locale = "en";
        String storage = "sqlite";
        int taxPercent = 5;
        int duration = 900;
        int bidTimeout = 30;
        String worldMode = "global"; // global, per-world, blacklist
        boolean autoRelist = false;
        int listingFeePercent = 0;
        String discordWebhook = "";
        boolean backupEnabled = true;
        int backupInterval = 60;
    }
}
