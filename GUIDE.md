# Auctify Usage Guide

Complete technical reference for commands, permissions, configuration, and security features.

## Quick Reference

| Command                                    | Description                      | Permission              |
| :----------------------------------------- | :------------------------------- | :---------------------- |
| `/ac`                                      | Opens the main Auction House GUI | `auctify.use`           |
| `/ac sell <start> [buyout] [duration]`     | List held item for auction       | `auctify.sell`          |
| `/ac bid <id> <amount>`                    | Place bid on a listing           | `auctify.bid`           |
| `/ac bidhistory <id>`                      | View bid history for a listing   | `auctify.bid`           |
| `/ac watchlist [id]`                       | View or toggle watchlist         | `auctify.watchlist`     |
| `/ac search <query>`                       | Search items by name/seller      | `auctify.use`           |
| `/ac history`                              | View your transaction history    | `auctify.history`       |
| `/ac claim`                                | Collect pending items/refunds    | `auctify.claim`         |
| `/ac cancel <id>`                          | Cancel your listing              | `auctify.cancel`        |
| `/ac extend <id> <minutes>`                | Extend auction expiry            | `auctify.sell`          |
| `/ac bulkcancel`                           | Cancel all your auctions         | `auctify.sell`          |
| `/ac pricehistory [item]`                  | View price trends for items      | `auctify.pricehistory`  |
| `/ac autobid <action>`                     | Manage auto-bid settings         | `auctify.autobid`       |
| `/ac notifications <action>`               | Manage notification preferences  | `auctify.notifications` |
| `/ac admin`                                | Open admin moderation panel      | `auctify.admin`         |
| `/ac admin blacklist <add\|remove\|list>`  | Manage player blacklist          | `auctify.admin`         |
| `/ac admin cancel <id>`                    | Cancel any listing (admin)       | `auctify.admin`         |
| `/ac admin backup`                         | Backup database                  | `auctify.admin`         |
| `/ac setup`                                | Run interactive setup wizard     | `auctify.admin`         |
| `/ac reload`                               | Reload config and locales        | `auctify.admin`         |
| `/ac ping`                                 | View plugin status               | `auctify.ping`          |
| `/ac about`                                | View plugin information          | `auctify.about`         |
| `/ac filter <type> <value>`                | Set advanced search filters      | `auctify.filter`        |
| `/ac buyorder <action>`                    | Manage buy orders                | `auctify.buyorder`      |
| `/ac bulksell <start> <buyout> [duration]` | Sell multiple stacks at once     | `auctify.bulksell`      |

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

### `/ac watchlist [id]` — Watchlist System (v1.0.2)

Track auctions without bidding. Useful for monitoring items you're interested in.

**Usage:**

- `/ac watchlist` — View all watched listings
- `/ac watchlist ABC123` — Toggle watch status for listing ABC123

**Features:**

- Auto-removes expired/ended listings
- Shows current bid amount in watchlist
- No limit on watchlist size

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

### Listing Limit Tiers (v1.0.1+)

Control how many active listings players can have:

| Permission                   | Max Listings | Usage Example               |
| :--------------------------- | :----------- | :-------------------------- |
| _(none)_                     | 3            | Default for regular players |
| `auctify.listings.5`         | 5            | For VIP/VIP+ ranks          |
| `auctify.listings.10`        | 10           | For MVP/MVP+ ranks          |
| `auctify.listings.unlimited` | Unlimited    | For staff/OPs               |
| Operators (OP)               | Unlimited    | Automatic unlimited access  |

**Note:** The old `auctify.bypass.maxlistings` permission is still supported but deprecated in favor of the tier system.

---

## Setup Wizard (v1.0.1+)

Auctify includes an interactive, chat-based setup wizard for first-time configuration.

### Starting the Wizard

**Automatic (First Run):**

- Plugin detects `system.first-run: true` in config
- Welcome message appears to all online admins
- Click "[ YES, Setup Now ]" to begin

**Manual:**

```bash
/ac setup        # Start/restart the wizard
/ac setup skip   # Skip and use defaults
```

### Setup Steps

| Step | Setting         | Options                                           |
| :--: | :-------------- | :------------------------------------------------ |
|  1   | **Language**    | English, Indonesian                               |
|  2   | **Storage**     | SQLite (file), MySQL (database), Memory (testing) |
|  3   | **Tax %**       | 0%, 5%, 10%, Custom (0-100)                       |
|  4   | **Duration**    | 5 min, 15 min, 1 hour                             |
|  5   | **Bid Timeout** | 15 sec, 30 sec, 60 sec                            |
|  6   | **Discord**     | Enable (enter URL) or Skip                        |
|  7   | **Backup**      | Disable, 1h, 6h, 24h interval                     |

### Re-running Setup

To reconfigure later:

1. Run `/ac setup` in-game
2. Or set `system.first-run: true` in config and restart

The wizard automatically saves config and reloads the plugin when complete.

---

## Backup System (v1.0.1+)

### Automatic Backups (SQLite Only)

Database backups run automatically with configurable retention:

```yaml
storage:
  sqlite:
    backup:
      enabled: true # Enable auto-backup
      interval: 60 # Minutes between backups
      keep-count: 10 # Max backups to keep (oldest auto-deleted)
```

**Backup Location:** `plugins/Auctify/backups/auctify_backup_YYYY-MM-DD_HH-mm-ss.db`

### Manual Backup

```bash
/ac admin backup   # Creates instant backup
```

### Restore from Backup

1. Stop server
2. Delete/rename corrupted `auctify.db`
3. Copy backup file to `plugins/Auctify/auctify.db`
4. Start server

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

## Transaction Logs

All auction activity is logged to `plugins/Auctify/logs/` with daily file rotation:

| Log Type    | Description                |
| ----------- | -------------------------- |
| `[LISTING]` | New listing created        |
| `[BID]`     | Bid placed on auction      |
| `[SALE]`    | Successful auction sale    |
| `[BUYOUT]`  | Instant buyout purchase    |
| `[EXPIRED]` | Auction ended without bids |
| `[CANCEL]`  | Listing cancelled          |
| `[CLAIM]`   | Items/money claimed        |
| `[ADMIN]`   | Admin actions performed    |

**Log Format:**

```
[14:32:15] [LISTING] Notch listed Diamond Sword for 1000.00 (ID: ABCD1234)
[14:45:22] [BID] Steve bid 1500.00 on Notch's Diamond Sword (ID: ABCD1234)
[15:00:01] [SALE] Steve bought Diamond Sword from Notch for 1500.00 (ID: ABCD1234)
```

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

### Crash Notifications (v1.0.2)

Separate webhook for crash alerts:

```yaml
discord:
  crash-webhook:
    enabled: true
    url: "https://discord.com/api/webhooks/CRASH_URL"
```

When enabled:

- Captures all uncaught exceptions
- Logs to `plugins/Auctify/crash.txt`
- Sends alert to Discord with timestamp and exception type

## Localization

Language files in `locales/` folder:

| File        | Language           | Flag |
| ----------- | ------------------ | ---- |
| `en.yml`    | English (default)  | 🇺🇸   |
| `id.yml`    | Bahasa Indonesia   | 🇮🇩   |
| `es.yml`    | Español (Spanish)  | 🇪🇸   |
| `pt_br.yml` | Português (Brazil) | 🇧🇷   |
| `ru.yml`    | Русский (Russian)  | 🇷🇺   |
| `de.yml`    | Deutsch (German)   | 🇩🇪   |
| `fr.yml`    | Français (French)  | 🇫🇷   |
| `pl.yml`    | Polski (Polish)    | 🇵🇱   |
| `tr.yml`    | Türkçe (Turkish)   | 🇹🇷   |
| `zh_cn.yml` | 简体中文 (Chinese) | 🇨🇳   |
| `ja.yml`    | 日本語 (Japanese)  | 🇯🇵   |
| `ko.yml`    | 한국어 (Korean)    | 🇰🇷   |
| `nl.yml`    | Nederlands (Dutch) | 🇳🇱   |

Change language: `general.language` in `config.yml`

**Create your own:** Copy `en.yml` and translate all values. Filename should be the 2-letter language code (e.g., `fr.yml`, `de.yml`).

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
- `auctify_watchlist` — Player watchlists (MemoryStorage only for now)
- `auctify_price_history` — Price trends and statistics (v1.1.0)
- `auctify_auto_bid` — Auto-bid configurations (v1.1.0)

---

## New Features (v1.1.0)

### Price History System

Track price trends for items to make informed buying/selling decisions.

#### `/ac pricehistory [item_type]`

View price history for a specific item type or all recent sales.

**Parameters:**

- `item_type` (optional) — Item material name (e.g., `DIAMOND_SWORD`, `IRON_INGOT`)

**Examples:**

```bash
# View all recent price history
/ac pricehistory

# View price history for diamond swords
/ac pricehistory DIAMOND_SWORD
```

**Features:**

- GUI-based display with pagination
- Statistics: total sales, average price, minimum, maximum
- Filter by item material
- Shows seller, winner, price, and timestamp for each sale
- Configurable retention period (default: 30 days)

**Configuration:**

```yaml
price-history:
  enabled: true
  retention-days: 30
  max-entries-per-item: 100
```

---

### Auto-Bid System

Set maximum bid amounts and let the system automatically bid for you when you're outbid.

#### `/ac autobid <action>`

Manage auto-bid settings.

**Actions:**

- `set <listing_id> <max_amount>` — Set auto-bid for a listing
- `remove <listing_id>` — Remove auto-bid for a listing
- `clear` — Clear all your auto-bids
- (no action) — View your active auto-bids

**Examples:**

```bash
# View your active auto-bids
/ac autobid

# Set auto-bid for listing ABC123 with max bid of 10000
/ac autobid set ABC123 10000

# Remove auto-bid for listing ABC123
/ac autobid remove ABC123

# Clear all your auto-bids
/ac autobid clear
```

**Features:**

- Automatically places bids when you're outbid
- Respects minimum bid increments
- Configurable maximum auto-bids per player (default: 5)
- Cannot bid on your own listings
- Shows remaining budget in status display

**Configuration:**

```yaml
autobid:
  enabled: true
  max-auto-bids-per-player: 5
  min-bid-increment: 0.1
```

---

### Notification System

Customize which events trigger notifications to stay informed about auction activity.

#### `/ac notifications <action>`

Manage notification preferences.

**Actions:**

- `toggle <type>` — Toggle specific notification type
- `all <on|off>` — Enable/disable all notifications
- (no action) — View current notification preferences

**Notification Types:**

- `outbid` — Notified when someone outbids you
- `buyout` — Notified when someone buys out your listing
- `auction-won` — Notified when you win an auction
- `item-sold` — Notified when your item sells
- `expiration` — Notified when your auction expires

**Examples:**

```bash
# View current notification preferences
/ac notifications

# Toggle outbid notifications
/ac notifications toggle outbid

# Enable all notifications
/ac notifications all on

# Disable all notifications
/ac notifications all off
```

**Features:**

- Per-player notification preferences
- Configurable default preference for new players
- Real-time notifications for active events
- Offline notifications delivered on login

**Configuration:**

```yaml
notifications:
  enabled: true
  default-enabled: true
  types:
    outbid: true
    buyout: true
    auction-won: true
    item-sold: true
    expiration: true
```

---

## Permissions Reference

| Permission                       | Description                 | Default |
| :------------------------------- | :-------------------------- | :------ |
| `auctify.use`                    | Access /ac command and GUI  | true    |
| `auctify.open`                   | Open auction house GUI      | true    |
| `auctify.sell`                   | Create and cancel listings  | true    |
| `auctify.bid`                    | Place bids on auctions      | true    |
| `auctify.buyorder`               | Manage buy orders           | true    |
| `auctify.watchlist`              | Use /ac watchlist           | true    |
| `auctify.history`                | View own auction history    | true    |
| `auctify.claim`                  | Collect pending items       | true    |
| `auctify.cancel`                 | Cancel listings             | true    |
| `auctify.search`                 | Search listings             | true    |
| `auctify.filter`                 | Use advanced filters        | true    |
| `auctify.bulksell`               | Bulk sell items             | true    |
| `auctify.pricehistory`           | View price history          | true    |
| `auctify.autobid`                | Manage auto-bids            | true    |
| `auctify.notifications`          | Manage notifications        | true    |
| `auctify.ping`                   | View plugin status          | true    |
| `auctify.about`                  | View plugin information     | true    |
| `auctify.admin`                  | Access admin panel          | op      |
| `auctify.admin.history`          | View other players' history | op      |
| `auctify.admin.stats`            | View server statistics      | op      |
| `auctify.bypass.fee`             | Bypass listing fee          | op      |
| `auctify.bypass.maxlistings`     | Unlimited listings          | op      |
| `auctify.admin.blacklist.bypass` | Bypass item blacklist       | op      |
| `auctify.listings.5`             | Max 5 active listings       | op      |
| `auctify.listings.10`            | Max 10 active listings      | op      |
| `auctify.listings.unlimited`     | Unlimited listings          | op      |
