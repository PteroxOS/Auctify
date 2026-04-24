# Auctify Project Structure

Auctify uses a **layered, security-first architecture** with strict separation of concerns, comprehensive thread safety, and audit-ready logging.

## Package Overview (`dev.auctify`)

| Package        | Responsibility                                                                          |
| :------------- | :-------------------------------------------------------------------------------------- |
| `Auctify.java` | Plugin lifecycle, manager initialization (dependency order), command/event registration |
| `auction`      | Core auction logic, thread-safe bid operations, event firing, expiry resolution         |
| `commands`     | SubCommand pattern with permission gates and input validation                           |
| `economy`      | Vault abstraction with TransactionResult pattern and failure recovery                   |
| `gui`          | Inventory GUIs with `AuctifyHolder` state management, click protection                  |
| `hook`         | PlaceholderAPI expansion with dynamic placeholders                                      |
| `listeners`    | Event handling: GUI clicks, chat input, join/quit cleanup                               |
| `setup`        | **v1.0.1+** — Interactive chat-based setup wizard for first-time configuration          |
| `storage`      | SQLite/MySQL with HikariCP, atomic transactions, schema migration, backups              |
| `util`         | Color, config, time formatting, Discord webhooks, dependency download                   |

---

## Core Architecture

## Data Models

### Core Records

| Class             | Purpose                                                 | Thread Safety                           |
| :---------------- | :------------------------------------------------------ | :-------------------------------------- |
| `AuctionListing`  | Auction state (item, bids, timing, tax status)          | Synchronized methods, defensive cloning |
| `BidRecord`       | Immutable bid history entry (bidder, amount, timestamp) | Record (immutable)                      |
| `AuctionHistory`  | Completed auction archive                               | Record (immutable)                      |
| `PendingRefund`   | Failed economy deposit queue entry                      | Record (immutable)                      |
| `AuctifyBidEvent` | Cancellable event for bid interception                  | Bukkit event system                     |

### State Management

**`AuctionManager`** — Central coordinator:

- `ConcurrentHashMap<String, AuctionListing>` — Active listings
- `synchronized(listing)` blocks — Per-listing operation locks
- `ConcurrentHashMap.newKeySet()` — Claim in-progress tracking

---

## Storage Layer

### Interface: `StorageManager`

```java
void saveListing(AuctionListing listing)
void savePendingRefund(PendingRefund refund)
List<ItemStack> claimAndClearDeliveries(UUID player)  // Atomic
List<PendingRefund> claimAndClearRefunds(UUID player)   // Atomic
```

### Implementations

| Backend         | Use Case                          | Connection Pool              |
| :-------------- | :-------------------------------- | :--------------------------- |
| `MemoryStorage` | Development/testing               | N/A (in-memory)              |
| `SQLiteStorage` | Single server, small-medium scale | HikariCP (pool size 1)       |
| `MySQLStorage`  | Production, multi-server networks | HikariCP (configurable pool) |

### Schema Migration

Automatic migration on startup:

- `tax_exempt` column added to `auctify_listings` if missing
- `auctify_pending_refunds` table created if missing
- Graceful handling of "column already exists" errors

---

## Security Architecture

### Synchronization Strategy

```
┌─────────────────────────────────────┐
│  Player A bids on Listing #123      │
│  → synchronized(listing#123)        │
│    → Check active/expired           │
│    → Check minimum bid              │
│    → Withdraw from Player A         │
│    → Refund previous bidder         │
│    → Apply bid                      │
│    → Save to storage                │
│  ← End synchronized                 │
└─────────────────────────────────────┘
         ↓
┌─────────────────────────────────────┐
│  Player B buyouts Listing #123      │
│  → BLOCKED until A's bid completes  │
│  → Then: synchronized(listing#123)  │
│    → Check listing still active     │
│    → Deactivate (setActive=false)   │
│    → Remove from activeListings     │
│    → Resolve auction                │
└─────────────────────────────────────┘
```

### Economy Safety Flow

```
Auction Resolution
    ↓
Deliver item to winner ✓
    ↓
Calculate tax and net amount
    ↓
Attempt deposit to seller ─┬─→ Success ✓
                           └─→ Failure → Save PendingRefund → Log SEVERE
    ↓
Attempt tax to server-account ─┬─→ Success ✓
                               └─→ Failure → Log WARNING (voided)
```

### TOCTOU Elimination

**Before (Vulnerable):**

```java
List<ItemStack> items = storage.getPendingDeliveries(player);  // Check
storage.clearPendingDeliveries(player);                         // Use
// Race: another thread could fetch same items between check and use!
```

**After (Secure):**

```java
// Atomic transaction
conn.setAutoCommit(false);
SELECT item_data FROM pending_deliveries WHERE player=?;
DELETE FROM pending_deliveries WHERE player=?;
conn.commit();
```

---

## Security Checklist

| Vulnerability                | Mitigation                                      | Location                                    |
| :--------------------------- | :---------------------------------------------- | :------------------------------------------ |
| Race condition on bid/buyout | Per-listing `synchronized` blocks               | `AuctionManager.placeBid()`, `buyout()`     |
| Item duplication on claim    | Atomic `claimAndClearDeliveries()`              | `SQLiteStorage`, `MySQLStorage`             |
| Double refund delivery       | Atomic `claimAndClearRefunds()`                 | All storage backends                        |
| Economy deposit failure      | `PendingRefund` queue with auto-delivery        | `safeDeposit()`, `PlayerQuitListener`       |
| Tax bypass at resolution     | `taxExempt` snapshotted at listing creation     | `AuctionListing.setTaxExempt()`             |
| NaN/Infinity exploits        | Input validation with `Double.isNaN/isInfinite` | `BidSubCommand`, `ChatBidListener`          |
| Info leak via tab complete   | Filter by ownership/permissions                 | `TabCompleter`                              |
| Stale bid input              | Configurable timeout with cleanup               | `ChatBidListener.cleanupExpiredBidInputs()` |
| Event cancellation state     | Fire event BEFORE state mutation                | `AuctionManager.placeBid()`                 |

---

## Thread Safety Summary

| Component                | Thread-Safe Mechanism                 |
| :----------------------- | :------------------------------------ |
| `activeListings` map     | `ConcurrentHashMap`                   |
| `claimingPlayers` set    | `ConcurrentHashMap.newKeySet()`       |
| Per-listing operations   | `synchronized(listing)` blocks        |
| Storage I/O              | HikariCP connection pools             |
| Chat bid tracking        | `ConcurrentHashMap` for `awaitingBid` |
| Pending refunds (Memory) | `Collections.synchronizedList`        |

---

## Build & Deployment

```bash
# Build shaded jar with HikariCP
mvn clean package

# Output: target/Auctify-1.0.0.jar
# Dependencies shaded: com.zaxxer.HikariCP
```

### Runtime Requirements

- **Java 17+**
- **Paper/Spigot 1.18+**
- **Vault + Economy Provider**

---

## License

MIT License — See [LICENSE](LICENSE)
