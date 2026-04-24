package dev.auctify.setup;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Interactive chat-based setup wizard for first-time installation.
 * Guides admin through configuration with clickable options.
 */
public class SetupWizard {

    private final Auctify plugin;
    private final Map<UUID, SetupState> activeSetups = new HashMap<>();

    public SetupWizard(Auctify plugin) {
        this.plugin = plugin;
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
            MessageUtil.sendRaw(player, "§cYou don't have permission to run the setup wizard.");
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
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|     §6✦ §e§lWELCOME TO AUCTIFY §6✦              §8|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|  §7This is your first time installing Auctify! §8|");
        MessageUtil.sendRaw(player, "§8|  §7Would you like to run the setup wizard?    §8|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "");

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
        MessageUtil.sendRaw(player, "");
    }

    // ========== STEP 1: LANGUAGE ==========
    private void showStep1_Language(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "§8==================================================");
        MessageUtil.sendRaw(player, "§8|          §6§lSETUP WIZARD §7(Step 1/7)            §8|");
        MessageUtil.sendRaw(player, "§8|                                                §8|");
        MessageUtil.sendRaw(player, "§8|  §eSelect your language / Pilih bahasa:        §8|");
        MessageUtil.sendRaw(player, "§8|                                                §8|");
        MessageUtil.sendRaw(player, "§8==================================================");
        MessageUtil.sendRaw(player, "");

        sendClickableOption(player, "  [ §aEnglish §r]", "en",
                "Click to select English", "/ac setup step1 en");
        sendClickableOption(player, "  [ §2Bahasa Indonesia §r]", "id",
                "Klik untuk pilih Indonesia", "/ac setup step1 id");
        MessageUtil.sendRaw(player, "");
    }

    // ========== STEP 2: STORAGE ==========
    private void showStep2_Storage(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        String title = state.locale.equals("id") ? "SETUP WIZARD (Step 2/7)" : "SETUP WIZARD (Step 2/7)";
        String prompt = state.locale.equals("id") ? "Pilih tipe penyimpanan:" : "Choose storage type:";

        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "§8|           §6§l" + title + "         §8|");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|  §e" + padRight(prompt, 38) + "§8|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "");

        sendClickableOption(player, "  [ §aSQLite §r]§7 - File-based, easy setup", "sqlite",
                "Best for small-medium servers", "/ac setup step2 sqlite");
        sendClickableOption(player, "  [ §bMySQL §r]§7 - Database server", "mysql",
                "Best for large servers", "/ac setup step2 mysql");
        sendClickableOption(player, "  [ §eMemory §r]§7 - No persistence (testing)", "memory",
                "Data lost on restart!", "/ac setup step2 memory");
        MessageUtil.sendRaw(player, "");
    }

    // ========== STEP 3: ECONOMY TAX ==========
    private void showStep3_Economy(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        boolean isId = state.locale.equals("id");
        String title = isId ? "SETUP WIZARD (Step 3/7)" : "SETUP WIZARD (Step 3/7)";
        String prompt = isId ? "Pengaturan pajak penjual:" : "Seller tax settings:";

        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "§8|           §6§l" + title + "         §8|");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|  §e" + padRight(prompt, 38) + "§8|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "");

        sendClickableOption(player, "  [ §a0% §r]§7 - No tax", "0",
                isId ? "Tidak ada pajak" : "No tax on sales", "/ac setup step3 0");
        sendClickableOption(player, "  [ §e5% §r]§7 - Recommended", "5",
                isId ? "Pajak 5% (default)" : "5% tax (default)", "/ac setup step3 5");
        sendClickableOption(player, "  [ §c10% §r]§7 - High tax", "10",
                isId ? "Pajak 10%" : "10% tax", "/ac setup step3 10");
        sendClickableOption(player, "  [ §bCustom §r]§7 - Enter percentage", "custom",
                isId ? "Masukkan angka 0-100" : "Enter 0-100", "/ac setup step3 custom");
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, isId ? "§7§oPajak: persen dari harga jual yang diambil server"
                : "§7§oTax: percentage taken from seller's earnings");
    }

    // ========== STEP 4: GENERAL ==========
    private void showStep4_General(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        boolean isId = state.locale.equals("id");
        String title = isId ? "SETUP WIZARD (Step 4/7)" : "SETUP WIZARD (Step 4/7)";
        String prompt = isId ? "Pengaturan umum lelang:" : "General auction settings:";

        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "§8|           §6§l" + title + "         §8|");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|  §e" + padRight(prompt, 38) + "§8|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "");

        sendClickableOption(player, "  [ §a5 min §r]§7 - Quick auctions", "300",
                isId ? "Durasi 5 menit" : "5 minutes duration", "/ac setup step4 300");
        sendClickableOption(player, "  [ §e15 min §r]§7 - Standard", "900",
                isId ? "Durasi 15 menit (default)" : "15 minutes (default)", "/ac setup step4 900");
        sendClickableOption(player, "  [ §b1 hour §r]§7 - Long auctions", "3600",
                isId ? "Durasi 1 jam" : "1 hour duration", "/ac setup step4 3600");
        MessageUtil.sendRaw(player, "");
    }

    // ========== STEP 5: GUI ==========
    private void showStep5_GUI(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        boolean isId = state.locale.equals("id");
        String title = isId ? "SETUP WIZARD (Step 5/7)" : "SETUP WIZARD (Step 5/7)";
        String prompt = isId ? "Waktu timeout input bid:" : "Bid input timeout:";
        String desc = isId ? "Waktu untuk memasukkan harga bid via chat" : "Time to enter bid price via chat";

        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "§8|           §6§l" + title + "         §8|");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|  §e" + padRight(prompt, 38) + "§8|");
        MessageUtil.sendRaw(player, "§8|  §7" + padRight(desc, 38) + "§8|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "");

        sendClickableOption(player, "  [ §a15 sec §r]§7 - Fast", "15",
                isId ? "15 detik" : "15 seconds", "/ac setup step5 15");
        sendClickableOption(player, "  [ §e30 sec §r]§7 - Standard (default)", "30",
                isId ? "30 detik (default)" : "30 seconds (default)", "/ac setup step5 30");
        sendClickableOption(player, "  [ §b60 sec §r]§7 - Relaxed", "60",
                isId ? "60 detik" : "60 seconds", "/ac setup step5 60");
        MessageUtil.sendRaw(player, "");
    }

    // ========== STEP 6: DISCORD ==========
    private void showStep6_Discord(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        boolean isId = state.locale.equals("id");
        String title = isId ? "SETUP WIZARD (Step 6/7)" : "SETUP WIZARD (Step 6/7)";
        String prompt = isId ? "Notifikasi Discord (opsional):" : "Discord notifications (optional):";

        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "§8|           §6§l" + title + "         §8|");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|  §e" + padRight(prompt, 38) + "§8|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "");

        sendClickableOption(player, "  [ §cSkip §r]§7 - No Discord", "skip",
                isId ? "Lewati" : "Skip this step", "/ac setup step6 skip");
        sendClickableOption(player, "  [ §aEnable §r]§7 - Enter webhook URL", "enable",
                isId ? "Masukkan URL webhook" : "Enter webhook URL", "/ac setup step6 enable");
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player,
                "§7§o" + (isId ? "Webhook URL didapat dari channel Discord" : "Get webhook URL from Discord channel"));
    }

    // ========== STEP 7: BACKUP ==========
    private void showStep7_Backup(Player player) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null)
            return;

        boolean isId = state.locale.equals("id");
        String title = isId ? "SETUP WIZARD (Step 7/7)" : "SETUP WIZARD (Step 7/7)";
        String prompt = isId ? "Pengaturan backup otomatis:" : "Automatic backup settings:";

        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "§8|           §6§l" + title + "         §8|");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|  §e" + padRight(prompt, 38) + "§8|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "");

        sendClickableOption(player, "  [ §cDisable §r]§7 - No backup", "disable",
                isId ? "Matikan backup" : "Disable backup", "/ac setup step7 disable");
        sendClickableOption(player, "  [ §a1 hour §r]§7 - Frequent", "60",
                isId ? "Backup tiap 1 jam" : "Every 1 hour", "/ac setup step7 60");
        sendClickableOption(player, "  [ §e6 hours §r]§7 - Balanced", "360",
                isId ? "Backup tiap 6 jam" : "Every 6 hours", "/ac setup step7 360");
        sendClickableOption(player, "  [ §b24 hours §r]§7 - Daily", "1440",
                isId ? "Backup tiap 24 jam" : "Every 24 hours", "/ac setup step7 1440");
        MessageUtil.sendRaw(player, "");
    }

    // ========== COMPLETE ==========
    private void completeSetup(Player player) {
        SetupState state = activeSetups.remove(player.getUniqueId());
        if (state == null)
            return;

        // Apply all settings
        plugin.getConfig().set("locale", state.locale);
        plugin.getConfig().set("storage.type", state.storage);
        plugin.getConfig().set("economy.tax-percent", state.taxPercent);
        plugin.getConfig().set("economy.tax-destination", state.taxPercent > 0 ? "void" : "void");
        plugin.getConfig().set("general.default-duration", state.duration);
        plugin.getConfig().set("gui.bid-input-timeout", state.bidTimeout);

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
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "§8|                                                |");
        MessageUtil.sendRaw(player, "§8|      §a§l✓ SETUP COMPLETED SUCCESSFULLY!        §8|");
        MessageUtil.sendRaw(player, "§8|                                                §8|");
        MessageUtil.sendRaw(player,
                "§8|  §7" + padRight(isId ? "Semua pengaturan telah disimpan!" : "All settings saved!", 38) + "§8|");
        MessageUtil.sendRaw(player,
                "§8|  §7" + padRight(isId ? "Plugin akan di-reload..." : "Reloading plugin...", 38) + "§8|");
        MessageUtil.sendRaw(player, "§8|                                                §8|");
        MessageUtil.sendRaw(player, "§8|════════════════════════════════════════════════|");
        MessageUtil.sendRaw(player, "");

        // Restart the plugin by reloading
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "plugman reload Auctify");
        });
    }

    public void skipSetup(Player player) {
        plugin.getConfig().set("system.first-run", false);
        plugin.saveConfig();
        activeSetups.remove(player.getUniqueId());
        MessageUtil.sendRaw(player,
                "§7Setup skipped. Using default configuration. Run §f/ac setup §7anytime to configure.");
    }

    // ========== HANDLE INPUT ==========
    public void handleStep(Player player, int step, String value) {
        SetupState state = activeSetups.get(player.getUniqueId());
        if (state == null) {
            MessageUtil.sendRaw(player, "§cSetup session expired. Run §f/ac setup §cto restart.");
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
                    MessageUtil.sendRaw(player, "§eEnter tax percentage (0-100):");
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
                showStep6_Discord(player);
            }
            case 6 -> {
                if (value.equals("skip")) {
                    showStep7_Backup(player);
                } else {
                    MessageUtil.sendRaw(player, "§ePlease enter your Discord webhook URL in chat:");
                    // Would need chat listener
                }
            }
            case 7 -> {
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
        String discordWebhook = "";
        boolean backupEnabled = true;
        int backupInterval = 60;
    }
}
