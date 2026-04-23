package dev.auctify.economy;

/**
 * Immutable result object for economy transactions.
 * Wraps the success/failure state and an optional reason message,
 * providing a safe alternative to throwing exceptions for expected failures
 * like insufficient funds.
 *
 * @param success whether the transaction completed successfully
 * @param reason  a human-readable reason message (especially useful on failure)
 */
public record TransactionResult(boolean success, String reason) {

    /** A shared success result for common use to avoid unnecessary allocations. */
    public static final TransactionResult SUCCESS = new TransactionResult(true, "Transaction successful.");

    /**
     * Creates a failure result with the given reason.
     *
     * @param reason the failure reason message
     * @return a new TransactionResult indicating failure
     */
    public static TransactionResult failure(String reason) {
        return new TransactionResult(false, reason);
    }

    /**
     * Creates a success result with a custom reason/description.
     *
     * @param reason the success description
     * @return a new TransactionResult indicating success
     */
    public static TransactionResult success(String reason) {
        return new TransactionResult(true, reason);
    }
}
