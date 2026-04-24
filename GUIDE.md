# Auctify Usage Guide

Complete technical reference for commands, permissions, configuration, and security features.

## Quick Reference

| Command                                   | Description                      | Permission      |
| :---------------------------------------- | :------------------------------- | :-------------- |
| `/ac`                                     | Opens the main Auction House GUI | `auctify.use`   |
| `/ac sell <start> [buyout] [duration]`    | List held item for auction       | `auctify.sell`  |
| `/ac bid <id> <amount>`                   | Place bid on a listing           | `auctify.bid`   |
| `/ac search <query>`                      | Search items by name/seller      | `auctify.use`   |
| `/ac history`                             | View your transaction history    | `auctify.use`   |
| `/ac claim`                               | Collect pending items/refunds    | `auctify.use`   |
| `/ac cancel <id>`                         | Cancel your listing              | `auctify.sell`  |
| `/ac admin`                               | Open admin moderation panel      | `auctify.admin` |
| `/ac admin blacklist <add\|remove\|list>` | Manage blacklist                 | `auctify.admin` |
| `/ac reload`                              | Reload config and locales        | `auctify.admin` |

_Aliases: `/ah`, `/au`, `/auction`, `/auctify`_

---

## Detailed Command Usage

### `/ac sell <start_price> [buyout_price] [duration]`

List the item in your main hand for auction.

**Parameters:**

- `start_price` (required) — Starting bid amount
- `buyout_price` (optional) — Instant buy price (set to 0 or omit for no buyout)
- `duration` (optional) — Auction duration in seconds (default: 300 / 5 minutes)

**Examples:**

```bash
# Bidding only, 5 minute duration (default)
/ac sell 1000

# With buyout option (1500 instant buy)
/ac sell 1000 1500

# With custom duration (10 minutes = 600 seconds)
/ac sell 1000 1500 600

# High-value item, 1 hour duration
/ac sell 50000 75000 3600
```

**Constraints:**

- Buyout must be ≥ `start_price × buyout-min-multiplier` (default 1.5x)
- Duration must be between `min-duration` and `max-duration` (config)
- You cannot exceed `max-listings-per-player` (bypass with `auctify.bypass.maxlistings`)

---

### `/ac bid <listing_id> <amount>`

Place a bid via command (alternative to GUI bidding).

**Examples:**

```bash
/ac bid ABC123 5000
```

**Notes:**

- Bid must be ≥ current bid + `min-increment` (default 10)
- Bid is rejected if you're already top bidder
- Cannot bid on your own listings
- Blacklisted players cannot bid

---

### GUI Bidding (Recommended)

1. `/ac open` to open auction house
2. **Left-click** item → Opens Confirm Bid GUI
3. Click "✔ Confirm Bid" → Type amount in chat
4. Type your bid amount or `cancel` to abort

**Bid Input Timeout:** Configurable via `gui.bid-input-timeout` (default 30 seconds)

---

### Buyout Purchase

Buy instantly without bidding:

1. `/ac open` to open auction house
2. **Right-click** item with buyout price → Item Detail GUI
3. Click "⚡ Buy Now!" button
4. Item delivered immediately, seller paid (minus tax)

---

### `/ac claim` — Mailbox System

Collect items and refunds from offline delivery:

- Items won while offline
- Items from cancelled listings
- **Pending money refunds** (failed economy deposits)

The claim system uses **atomic operations** — no duplication possible even if clicked rapidly.

---

## Permission System

| Permission                   | Description                        | Default |
| :--------------------------- | :--------------------------------- | :------ |
| `auctify.use`                | Access GUI, search, claim, history | true    |
| `auctify.sell`               | Create and cancel listings         | true    |
| `auctify.bid`                | Place bids on items                | true    |
| `auctify.admin`              | Full admin access                  | op      |
| `auctify.bypass.maxlistings` | Ignore listing limit               | op      |
| `auctify.bypass.tax`         | Exempt from sales tax              | false   |

---

## Security Features

### Economy Transaction Safety

Failed deposits (e.g., economy plugin offline) are **not lost**:

1. System detects deposit failure
2. Amount + reason saved as `PendingRefund`
3. Player receives refund automatically on next login
4. Full audit trail logged to console

### Race Condition Protection

All auction operations use **per-listing synchronization**:

- Multiple players bidding simultaneously → safe
- Buyout while another player bidding → safe
- Cancel while bidding in progress → safe

### TOCTOU Protection

**Time-of-check to time-of-use** vulnerabilities eliminated:

- Claim operations are atomic (fetch + delete in one transaction)
- Pending refunds use `claimAndClearRefunds()` — no double-delivery
- Item delivery clones ItemStack to prevent reference mutation

### Input Hardening

- NaN/Infinity/negative values rejected
- Bid timeouts prevent stale input sessions
- Tab completion filtered by permissions (can't see others' private listings)

---

## Discord Webhook Setup

1. Discord Server → Settings → Integrations → Webhooks → New Webhook
2. Copy Webhook URL
3. Edit `config.yml`:

```yaml
discord:
  enabled: true
  webhook-url: "https://discord.com/api/webhooks/YOUR_URL"
```

4. `/ac reload`

Events:

- `on-new-listing` — When item is listed
- `on-sale` — When auction ends with winner

## Localization

Language files in `locales/` folder:

- `en.yml` — English (default)
- `id.yml` — Bahasa Indonesia

Change language: `general.language` in `config.yml`

Supports Minecraft color codes (`§`) and MiniMessage formatting.

---

## Database Schema

### SQLite (Default)

File: `auctify.db` in plugin folder

### MySQL (Production)

Recommended for large servers. Configure in `config.yml`:

```yaml
storage:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: auctify
    username: root
    password: password
    pool-size: 10
```

### Tables

- `auctify_listings` — Active auctions
- `auctify_history` — Completed/cancelled auctions
- `auctify_pending_deliveries` — Items for offline players
- `auctify_pending_refunds` — Failed economy deposits
- `auctify_ratings` — Player ratings
- `auctify_blacklist` — Banned players
