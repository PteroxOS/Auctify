package dev.auctify.economy;

/**
 * Immutable result object for economy transactions.
 * Wraps the success/failure state and an optional reason message,
 * providing a safe alternative to throwing exceptions for expected failures
 * like insufficient funds.
 */
public record TransactionResult(boolean success, String reason) {

    /** A shared success result for common use to avoid unnecessary allocations. */
    public static final TransactionResult SUCCESS = new TransactionResult(true, "Transaction successful.");

    /** Creates a failure result with the given reason. */
    public static TransactionResult failure(String reason) {
        return new TransactionResult(false, reason);
    }

    /** Creates a success result with a custom reason/description. */
    public static TransactionResult success(String reason) {
        return new TransactionResult(true, reason);
    }
}
