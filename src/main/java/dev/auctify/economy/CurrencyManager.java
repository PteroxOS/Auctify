package dev.auctify.economy;

import dev.auctify.Auctify;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages currency support for the auction house.
 * Provides abstraction for single or multiple currency systems.
 * Currently supports Vault's single currency system with framework for future
 * multi-currency expansion.
 */
public class CurrencyManager {

    private final Auctify plugin;
    private final EconomyManager economyManager;

    /** Currency definitions from config. */
    private final Map<String, Currency> currencies = new HashMap<>();

    /** Default currency ID. */
    private String defaultCurrency = "default";

    public CurrencyManager(Auctify plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        loadCurrencies();
    }

    /**
     * Loads currency definitions from config.
     */
    private void loadCurrencies() {
        var config = plugin.getConfig();

        // Load default currency settings
        String defaultName = config.getString("currency.name", "Coins");
        String defaultSymbol = config.getString("currency.symbol", "$");
        int defaultDecimals = config.getInt("currency.decimals", 2);

        currencies.put("default", new Currency("default", defaultName, defaultSymbol, defaultDecimals));

        // Placeholder for future multi-currency support
        // Additional currencies can be loaded from config in future versions
        if (config.getBoolean("currency.multi-currency.enabled", false)) {
            plugin.getLogger().warning("Multi-currency support is not yet fully implemented.");
            plugin.getLogger().warning("Using single currency mode via Vault.");
        }
    }

    /**
     * Gets a player's balance in the default currency.
     */
    public double getBalance(UUID playerUUID) {
        return economyManager.getBalance(playerUUID);
    }

    /**
     * Gets a player's balance in a specific currency.
     * Currently only supports default currency.
     */
    public double getBalance(UUID playerUUID, String currencyId) {
        if (!currencies.containsKey(currencyId)) {
            currencyId = defaultCurrency;
        }
        // For now, all currencies map to the same Vault economy
        return economyManager.getBalance(playerUUID);
    }

    /**
     * Sets a player's balance in the default currency.
     */
    public void setBalance(UUID playerUUID, double amount) {
        double current = getBalance(playerUUID);
        double difference = amount - current;

        if (difference > 0) {
            economyManager.deposit(playerUUID, difference);
        } else if (difference < 0) {
            economyManager.withdraw(playerUUID, -difference);
        }
    }

    /**
     * Sets a player's balance in a specific currency.
     * Currently only supports default currency.
     */
    public void setBalance(UUID playerUUID, double amount, String currencyId) {
        if (!currencies.containsKey(currencyId)) {
            currencyId = defaultCurrency;
        }
        setBalance(playerUUID, amount);
    }

    /**
     * Deposits money to a player's account.
     */
    public boolean deposit(UUID playerUUID, double amount) {
        return economyManager.deposit(playerUUID, amount).success();
    }

    /**
     * Withdraws money from a player's account.
     */
    public boolean withdraw(UUID playerUUID, double amount) {
        return economyManager.withdraw(playerUUID, amount).success();
    }

    /**
     * Checks if a player has enough money.
     */
    public boolean has(UUID playerUUID, double amount) {
        return getBalance(playerUUID) >= amount;
    }

    /**
     * Formats a monetary amount with the currency symbol.
     */
    public String format(double amount) {
        return format(amount, defaultCurrency);
    }

    /**
     * Formats a monetary amount with a specific currency.
     */
    public String format(double amount, String currencyId) {
        Currency currency = currencies.getOrDefault(currencyId, currencies.get(defaultCurrency));
        if (currency == null) {
            return String.format("%.2f", amount);
        }

        return String.format("%s%s", currency.symbol, String.format("%." + currency.decimals + "f", amount));
    }

    /**
     * Formats a monetary amount with full currency name.
     */
    public String formatLong(double amount) {
        return formatLong(amount, defaultCurrency);
    }

    /**
     * Formats a monetary amount with full currency name for a specific currency.
     */
    public String formatLong(double amount, String currencyId) {
        Currency currency = currencies.getOrDefault(currencyId, currencies.get(defaultCurrency));
        if (currency == null) {
            return String.format("%.2f", amount);
        }

        return String.format("%s %.2f %s", currency.symbol, amount, currency.name);
    }

    /**
     * Gets the default currency ID.
     */
    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    /**
     * Gets a currency by ID.
     */
    public Currency getCurrency(String currencyId) {
        return currencies.get(currencyId);
    }

    /**
     * Checks if multi-currency mode is enabled.
     */
    public boolean isMultiCurrencyEnabled() {
        return plugin.getConfig().getBoolean("currency.multi-currency.enabled", false);
    }

    /**
     * Currency definition.
     */
    public static class Currency {
        private final String id;
        private final String name;
        private final String symbol;
        private final int decimals;

        public Currency(String id, String name, String symbol, int decimals) {
            this.id = id;
            this.name = name;
            this.symbol = symbol;
            this.decimals = decimals;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSymbol() {
            return symbol;
        }

        public int getDecimals() {
            return decimals;
        }
    }
}
