package dev.auctify.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Wraps Vault's {@link Economy} provider with comprehensive null safety.
 * Never throws unchecked exceptions to callers — instead returns
 * {@link TransactionResult}
 * objects with descriptive success/failure states.
 *
 * <p>
 * If Vault or an economy provider is not found at startup, all methods
 * gracefully return failure results and log appropriate warnings.
 * </p>
 */
public class EconomyManager {

    /** The plugin's logger for economy-related messages. */
    private final Logger logger;

    /** The Vault economy provider, or null if unavailable. */
    private Economy economy;

    /** Whether a valid economy provider was found and hooked. */
    private boolean economyAvailable;

    /**
     * Creates a new EconomyManager and attempts to hook into Vault's economy
     * provider.
     *
     * @param plugin the main plugin instance for service registration lookup
     */
    public EconomyManager(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
        this.economyAvailable = false;
        setupEconomy();
    }

    /**
     * Attempts to hook into the Vault economy provider via Bukkit's service
     * manager.
     * If Vault is not installed or no economy plugin is registered, logs a warning
     * and sets {@code economyAvailable} to false.
     */
    private void setupEconomy() {
        // Check if Vault plugin is present on the server
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            logger.warning("Vault not found! Economy features will be disabled.");
            return;
        }

        // Attempt to get the registered Economy service provider
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            logger.warning("No economy provider found! Install an economy plugin (e.g., EssentialsX).");
            return;
        }

        economy = rsp.getProvider();
        economyAvailable = true;
        logger.info("Successfully hooked into economy provider: " + economy.getName());
    }

    /**
     * Deposits money into a player's account.
     *
     * @param playerUUID the UUID of the player to deposit to
     * @param amount     the amount to deposit (must be positive)
     * @return a TransactionResult indicating success or failure with reason
     */
    public TransactionResult deposit(UUID playerUUID, double amount) {
        if (!economyAvailable) {
            return TransactionResult.failure("Economy is not available.");
        }
        if (amount <= 0) {
            return TransactionResult.failure("Deposit amount must be positive.");
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        EconomyResponse response = economy.depositPlayer(player, amount);

        if (response.transactionSuccess()) {
            return TransactionResult.SUCCESS;
        } else {
            return TransactionResult.failure("Deposit failed: " + response.errorMessage);
        }
    }

    /**
     * Withdraws money from a player's account.
     *
     * @param playerUUID the UUID of the player to withdraw from
     * @param amount     the amount to withdraw (must be positive)
     * @return a TransactionResult indicating success or failure with reason
     */
    public TransactionResult withdraw(UUID playerUUID, double amount) {
        if (!economyAvailable) {
            return TransactionResult.failure("Economy is not available.");
        }
        if (amount <= 0) {
            return TransactionResult.failure("Withdrawal amount must be positive.");
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);

        // Pre-check balance before attempting withdrawal to avoid partial failures
        if (!economy.has(player, amount)) {
            return TransactionResult.failure("Insufficient funds.");
        }

        EconomyResponse response = economy.withdrawPlayer(player, amount);

        if (response.transactionSuccess()) {
            return TransactionResult.SUCCESS;
        } else {
            return TransactionResult.failure("Withdrawal failed: " + response.errorMessage);
        }
    }

    /**
     * Gets the balance of a player's account.
     *
     * @param playerUUID the UUID of the player
     * @return the player's balance, or 0.0 if economy is unavailable
     */
    public double getBalance(UUID playerUUID) {
        if (!economyAvailable) {
            return 0.0;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return economy.getBalance(player);
    }

    /**
     * Checks whether a player has at least the specified amount of money.
     *
     * @param playerUUID the UUID of the player
     * @param amount     the amount to check against
     * @return true if the player has sufficient funds, false otherwise or if
     *         economy unavailable
     */
    public boolean has(UUID playerUUID, double amount) {
        if (!economyAvailable) {
            return false;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return economy.has(player, amount);
    }

    /**
     * Formats an amount using Vault's currency formatting.
     * Falls back to a simple two-decimal format if economy is unavailable.
     *
     * @param amount the amount to format
     * @return the formatted currency string (e.g., "$1,000.00")
     */
    public String format(double amount) {
        if (!economyAvailable) {
            return String.format("$%.2f", amount);
        }
        return economy.format(amount);
    }

    /**
     * Returns whether a valid economy provider is available.
     *
     * @return true if Vault and an economy provider are hooked
     */
    public boolean isAvailable() {
        return economyAvailable;
    }

    /**
     * Deposits an amount into a named server/bank account via Vault.
     * If the account does not exist and the economy provider supports it, it will
     * be created.
     *
     * @param accountName the Vault account name (from config:
     *                    economy.tax-account-name)
     * @param amount      the amount to deposit
     * @return TransactionResult indicating success or failure
     */
    public TransactionResult depositToAccount(String accountName, double amount) {
        if (!economyAvailable) {
            return TransactionResult.failure("Economy not available");
        }
        if (amount <= 0) {
            return TransactionResult.failure("Amount must be positive");
        }
        try {
            // Vault's bankDeposit method deposits into a named bank account
            EconomyResponse response = economy.bankDeposit(accountName, amount);
            if (response.transactionSuccess()) {
                return TransactionResult.success("Deposited to " + accountName);
            } else {
                // Fallback: some economy plugins don't support bank accounts.
                // Log and treat as "void" — tax is collected but not stored anywhere.
                logger.warning("[Auctify] Bank deposit to '" + accountName + "' failed: "
                        + response.errorMessage + ". Tax will be voided instead.");
                return TransactionResult.failure(response.errorMessage);
            }
        } catch (Exception e) {
            logger.severe("[Auctify] Exception during bank deposit to '" + accountName + "': " + e.getMessage());
            return TransactionResult.failure(e.getMessage());
        }
    }
}
