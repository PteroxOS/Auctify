# Auctify Usage Guide

Complete reference for commands, permissions, configuration, and security features.

---

## Commands

### Player Commands

| Command                                    | Description                       | Permission              |
| :----------------------------------------- | :-------------------------------- | :---------------------- |
| `/ac`                                      | Opens the main Auction House GUI  | `auctify.use`           |
| `/ac sell <start> [buyout] [duration]`     | List held item for auction        | `auctify.sell`          |
| `/ac bid <id> <amount>`                    | Place bid on a listing            | `auctify.bid`           |
| `/ac bidhistory <id>`                      | View bid history for a listing    | `auctify.bid`           |
| `/ac watchlist [id]`                       | View or toggle watchlist          | `auctify.watchlist`     |
| `/ac search <query>`                       | Search items by name or seller    | `auctify.use`           |
| `/ac history`                              | View your transaction history     | `auctify.history`       |
| `/ac claim`                                | Collect pending items and refunds | `auctify.claim`         |
| `/ac cancel <id>`                          | Cancel your listing               | `auctify.cancel`        |
| `/ac extend <id> <minutes>`                | Extend auction expiry             | `auctify.sell`          |
| `/ac bulkcancel`                           | Cancel all your auctions          | `auctify.sell`          |
| `/ac bulksell <start> <buyout> [duration]` | Sell multiple stacks at once      | `auctify.bulksell`      |
| `/ac pricehistory [item]`                  | View price trends for items       | `auctify.pricehistory`  |
| `/ac autobid <action>`                     | Manage auto-bid settings          | `auctify.autobid`       |
| `/ac notifications <action>`               | Manage notification preferences   | `auctify.notifications` |
| `/ac filter <type> <value>`                | Set advanced search filters       | `auctify.filter`        |
| `/ac buyorder <action>`                    | Manage buy orders                 | `auctify.buyorder`      |
| `/ac ping`                                 | View plugin status                | `auctify.ping`          |
| `/ac about`                                | View plugin information           | `auctify.about`         |

### Admin Commands

| Command                               | Description                  | Permission      |
| :------------------------------------ | :--------------------------- | :-------------- |
| `/ac admin`                           | Open admin moderation panel  | `auctify.admin` |
| `/ac admin blacklist add <player>`    | Add player to blacklist      | `auctify.admin` |
| `/ac admin blacklist remove <player>` | Remove player from blacklist | `auctify.admin` |
| `/ac admin blacklist list`            | View blacklist               | `auctify.admin` |
| `/ac admin cancel <id>`               | Cancel any listing           | `auctify.admin` |
| `/ac admin backup`                    | Backup database              | `auctify.admin` |
| `/ac setup`                           | Run interactive setup wizard | `auctify.admin` |
| `/ac reload`                          | Reload config and locales    | `auctify.admin` |

**Aliases:** `/ah` `/au` `/auction` `/auctify`

---

## Command Details

### `/ac sell`

List the item in your main hand for auction.

**Usage:** `/ac sell <start_price> [buyout_price] [duration]`

| Parameter      | Required | Description                            |
| :------------- | :------- | :------------------------------------- |
| `start_price`  | ✅ Yes   | Starting bid amount                    |
| `buyout_price` | ❌ No    | Instant buy price (omit for no buyout) |
| `duration`     | ❌ No    | Duration in seconds (default: 300)     |

**Examples:**

```bash
/ac sell 1000              # Bidding only, 5 minute duration
/ac sell 1000 1500         # With buyout option
/ac sell 1000 1500 600     # With custom duration (10 minutes)
```

### `/ac bid`

Place a bid via command.

**Usage:** `/ac bid <listing_id> <amount>`

**Example:**

```bash
/ac bid ABC123 5000
```

### GUI Bidding

1. `/ac open` to open auction house
2. Left-click item to open bid confirmation
3. Click "Confirm Bid" and type amount in chat
4. Type your bid amount or `cancel` to abort

### Buyout

1. `/ac open` to open auction house
2. Right-click item with buyout price
3. Click "Buy Now!" button

### Claim System

Collect items and refunds from offline delivery:

- Items won while offline
- Items from cancelled listings
- Pending money refunds (failed economy deposits)

**Usage:** `/ac claim`

### Watchlist

Track auctions without bidding.

**Usage:**

- `/ac watchlist` - View all watched listings
- `/ac watchlist ABC123` - Toggle watch status

## Permissions

### Basic Permissions

| Permission              | Description                | Default |
| :---------------------- | :------------------------- | :------ |
| `auctify.use`           | Access /ac command and GUI | true    |
| `auctify.sell`          | Create and cancel listings | true    |
| `auctify.bid`           | Place bids on auctions     | true    |
| `auctify.watchlist`     | Use /ac watchlist          | true    |
| `auctify.history`       | View own auction history   | true    |
| `auctify.claim`         | Collect pending items      | true    |
| `auctify.cancel`        | Cancel listings            | true    |
| `auctify.search`        | Search listings            | true    |
| `auctify.filter`        | Use advanced filters       | true    |
| `auctify.bulksell`      | Bulk sell items            | true    |
| `auctify.pricehistory`  | View price history         | true    |
| `auctify.autobid`       | Manage auto-bids           | true    |
| `auctify.notifications` | Manage notifications       | true    |
| `auctify.ping`          | View plugin status         | true    |
| `auctify.about`         | View plugin information    | true    |

### Admin Permissions

| Permission                       | Description                 | Default |
| :------------------------------- | :-------------------------- | :------ |
| `auctify.admin`                  | Access admin panel          | op      |
| `auctify.admin.history`          | View other players' history | op      |
| `auctify.admin.stats`            | View server statistics      | op      |
| `auctify.bypass.fee`             | Bypass listing fee          | op      |
| `auctify.bypass.maxlistings`     | Unlimited listings          | op      |
| `auctify.admin.blacklist.bypass` | Bypass item blacklist       | op      |

### Listing Limits

| Permission                   | Max Listings |
| :--------------------------- | :----------- |
| (none)                       | 3            |
| `auctify.listings.5`         | 5            |
| `auctify.listings.10`        | 10           |
| `auctify.listings.unlimited` | Unlimited    |

## Setup Wizard

Auctify includes an interactive setup wizard for first-time configuration.

**Start Wizard:**

```bash
/ac setup        # Start/restart the wizard
/ac setup skip   # Skip and use defaults
```

**Setup Steps:**

1. Language (English, Indonesian)
2. Storage (SQLite, H2, MySQL, Memory)
3. Tax % (0%, 5%, 10%, Custom)
4. Duration (5 min, 15 min, 1 hour)
5. Bid Timeout (15 sec, 30 sec, 60 sec)
6. Discord (Enable or Skip)
7. Backup (Disable, 1h, 6h, 24h)

## Backup System

### Automatic Backups

Database backups run automatically with configurable retention.

**Configuration:**

```yaml
storage:
  h2:
    backup:
      enabled: true
      interval: 60
      keep-count: 10
```

**Backup Location:** `plugins/Auctify/backups/auctify_backup_YYYY-MM-DD_HH-mm-ss.db`

### Manual Backup

```bash
/ac admin backup
```

### Restore from Backup

1. Stop server
2. Delete/rename corrupted database file
3. Copy backup file to `plugins/Auctify/`
4. Start server

## Security Features

### Economy Transaction Safety

Failed deposits are not lost:

1. System detects deposit failure
2. Amount + reason saved as `PendingRefund`
3. Player receives refund automatically on next login
4. Full audit trail logged to console

### Race Condition Protection

All auction operations use per-listing synchronization:

- Multiple players bidding simultaneously - safe
- Buyout while another player bidding - safe
- Cancel while bidding in progress - safe

### TOCTOU Protection

Time-of-check to time-of-use vulnerabilities eliminated:

- Claim operations are atomic (fetch + delete in one transaction)
- Pending refunds use `claimAndClearRefunds()` - no double-delivery
- Item delivery clones ItemStack to prevent reference mutation

### Input Hardening

- NaN/Infinity/negative values rejected
- Bid timeouts prevent stale input sessions
- Tab completion filtered by permissions

## Transaction Logs

All auction activity is logged to `plugins/Auctify/logs/` with daily file rotation.

**Log Types:**

- `[LISTING]` - New listing created
- `[BID]` - Bid placed on auction
- `[SALE]` - Successful auction sale
- `[BUYOUT]` - Instant buyout purchase
- `[EXPIRED]` - Auction ended without bids
- `[CANCEL]` - Listing cancelled
- `[CLAIM]` - Items/money claimed
- `[ADMIN]` - Admin actions performed

## Discord Webhook

**Setup:**

1. Discord Server - Settings - Integrations - Webhooks - New Webhook
2. Copy Webhook URL
3. Edit `config.yml`:

```yaml
discord:
  enabled: true
  webhook-url: "https://discord.com/api/webhooks/YOUR_URL"
```

4. `/ac reload`

**Events:**

- `on-new-listing` - When item is listed
- `on-sale` - When auction ends with winner

## Localization

Language files in `locales/` folder:

| File        | Language           |
| :---------- | :----------------- |
| `en.yml`    | English (default)  |
| `id.yml`    | Bahasa Indonesia   |
| `es.yml`    | Español (Spanish)  |
| `pt_br.yml` | Português (Brazil) |
| `ru.yml`    | Русский (Russian)  |
| `de.yml`    | Deutsch (German)   |
| `fr.yml`    | Français (French)  |
| `pl.yml`    | Polski (Polish)    |
| `tr.yml`    | Türkçe (Turkish)   |
| `zh_cn.yml` | 简体中文 (Chinese) |
| `ja.yml`    | 日本語 (Japanese)  |
| `ko.yml`    | 한국어 (Korean)    |
| `nl.yml`    | Nederlands (Dutch) |

Change language: `general.language` in `config.yml`

## Database

### Storage Options

**H2 (Recommended)**

- File-based, high performance
- No external database required
- Automatic backups supported

**SQLite**

- File-based, easy setup
- Good for single servers
- Automatic backups supported

**MySQL**

- Production, multi-server networks
- Requires external database
- Configurable connection pool

**Memory**

- Development/testing only
- No persistence
- Not recommended for production

### MySQL Configuration

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

- `auctify_listings` - Active auctions
- `auctify_history` - Completed/cancelled auctions
- `auctify_pending_deliveries` - Items for offline players
- `auctify_pending_refunds` - Failed economy deposits
- `auctify_ratings` - Player ratings
- `auctify_blacklist` - Banned players
- `auctify_watchlist` - Player watchlists
- `auctify_price_history` - Price trends and statistics
- `auctify_auto_bid` - Auto-bid configurations

## Advanced Features

### Price History

Track price trends for items.

**Usage:** `/ac pricehistory [item_type]`

**Configuration:**

```yaml
price-history:
  enabled: true
  retention-days: 30
  max-entries-per-item: 100
```

### Auto-Bid

Set maximum bid amounts and let the system automatically bid for you.

**Usage:**

```bash
/ac autobid                    # View active auto-bids
/ac autobid set ABC123 10000   # Set auto-bid
/ac autobid remove ABC123       # Remove auto-bid
/ac autobid clear              # Clear all auto-bids
```

**Configuration:**

```yaml
autobid:
  enabled: true
  max-auto-bids-per-player: 5
  min-bid-increment: 0.1
```

### Notifications

Customize which events trigger notifications.

**Usage:**

```bash
/ac notifications                  # View preferences
/ac notifications toggle outbid    # Toggle specific type
/ac notifications all on           # Enable all
/ac notifications all off          # Disable all
```

**Notification Types:**

- `outbid` - Notified when someone outbids you
- `buyout` - Notified when someone buys out your listing
- `auction-won` - Notified when you win an auction
- `item-sold` - Notified when your item sells
- `expiration` - Notified when your auction expires

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
