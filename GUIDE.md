# Auctify Usage Guide v1.8.0

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
| `/ac bulkbuy <action>`                     | Manage bulk buy list              | `auctify.bulkbuy`       |
| `/ac theme <action>`                       | Manage GUI themes                 | `auctify.theme`         |
| `/ac template <action>`                    | Manage listing templates          | `auctify.template`      |
| `/ac rating [player]`                      | View player ratings/reputation    | `auctify.rating`        |
| `/ac trade <action>`                       | Direct player-to-player trade     | `auctify.trade`         |
| `/ac stats [dashboard]`                    | View stats or dashboard           | `auctify.ping`          |
| `/ac about`                                | View plugin information           | `auctify.about`         |
| `/ac ping`                                 | View plugin status                | `auctify.ping`          |

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
| `auctify.bulkbuy`       | Bulk buy items             | true    |
| `auctify.theme`         | Manage GUI themes          | true    |
| `auctify.template`      | Manage listing templates   | true    |
| `auctify.rating`        | View player ratings        | true    |
| `auctify.trade`         | Direct player trading      | true    |
| `auctify.tax.exempt`    | Exempt from tax brackets   | false   |
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

### Extreme Audit v3 Hardening (v1.8.0)

The following security improvements were added in the Extreme Audit v3:

- **Auto-Bid Cascade Limit** - Prevents infinite bid loops between competing auto-bidders (max 5 levels)
- **Transaction Verification for Ratings** - Players can only rate sellers they've actually purchased from
- **Watchlist Size Limit** - Prevents database bloat with configurable per-player limit (default 50)
- **Thread-Safe Date Formatting** - All date operations now use ThreadLocal to prevent corruption under load
- **Permission Hardening** - Admin commands now validate permissions before any operation
- **TOCTOU Race Condition Fixes** - Bulk operations now properly handle concurrent state changes

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
  max-per-player: 10
  min-increment: 1.0
  # FIX-1: Maximum auto-bid cascade depth (prevent infinite loop)
  # When two players have auto-bids on the same listing, this limits
  # how many times they can automatically outbid each other
  max-cascade-depth: 5
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

### Bulk Buy

Buy multiple items at once from the auction house.

**Usage:**

```bash
/ac bulkbuy add <listing_id>     # Add item to bulk buy list
/ac bulkbuy remove <listing_id>  # Remove item from list
/ac bulkbuy list                 # View bulk buy list with total cost
/ac bulkbuy clear               # Clear all items from list
/ac bulkbuy buy                 # Purchase all items in list
```

**Configuration:**

```yaml
bulkbuy:
  enabled: true
  max-items: 10
```

**Features:**

- Add multiple listings to a shopping cart
- View total cost before purchasing
- One-click purchase of all items
- Automatic inventory space check
- Balance verification before purchase

### Category Filtering

Filter auction listings by item type in the GUI.

**Available Categories:**

- ALL - Show all listings
- WEAPONS - Swords, bows, tridents, arrows
- TOOLS - Pickaxes, axes, shovels, hoes, shears
- ARMOR - Helmets, chestplates, leggings, boots, shields
- BLOCKS - All block types
- FOOD - Edible items and food ingredients
- POTIONS - Potions, splash potions, lingering potions
- REDSTONE - Redstone components, pistons, comparators
- DECORATIONS - Carpets, flowers, glass, stairs, fences
- MISC - Miscellaneous items not in other categories

**Usage:**

Click the category button in the GUI to cycle through categories.

### Sorting Options

Sort listings by various criteria in the GUI.

**Available Sort Modes:**

- TIME_ASC - Ending soonest (default)
- TIME_DESC - Ending latest
- PRICE_ASC - Lowest total price first
- PRICE_DESC - Highest total price first
- PRICE_PER_UNIT_ASC - Lowest price per unit first
- PRICE_PER_UNIT_DESC - Highest price per unit first
- BIDS - Most bids first
- NEWEST - Newest listings first
- ENDING_SOON - Ending soonest

**Usage:**

Click the sort button (comparator) in the GUI to cycle through sort modes.

### GUI Themes

Customize the appearance of the auction house GUI with different themes.

**Usage:**

```bash
/ac theme list                 # List available themes
/ac theme set <theme_name>    # Set active theme
```

**Available Themes:**

- default - Classic gray theme
- dark - Dark purple theme
- light - Light white theme
- ocean - Blue water theme

**Configuration:**

```yaml
gui:
  theme: "default"
  themes:
    default:
      title: "§8✦ §6Auctify §8— §7Auction House §8✦"
      filler-material: GRAY_STAINED_GLASS_PANE
      border-material: GRAY_STAINED_GLASS_PANE
      highlight-material: LIME_STAINED_GLASS_PANE
      danger-material: RED_STAINED_GLASS_PANE
      info-material: BLUE_STAINED_GLASS_PANE
```

### Listing Templates

Save common listing settings as templates for quick reuse.

**Usage:**

```bash
/ac template create <name> <start_price> [buyout] [duration]  # Create template
/ac template list                                               # List your templates
/ac template delete <name>                                      # Delete template
/ac template use <name>                                         # Apply template to held item
```

**Features:**

- Save start price, buyout price, and duration
- Quick apply templates when listing items
- Per-player template storage
- Template management (create, list, delete)

**Example:**

```bash
/ac template create diamond 1000 1500 600
/ac template use diamond
```

### Player Ratings / Reputation

View seller ratings and reputation to make informed buying decisions.

**Usage:**

```bash
/ac rating [player]    # View rating for a player (or yourself if no player specified)
```

**Reputation Titles:**

- ⭐ Legendary Seller (4.8+)
- ✓ Trusted Seller (4.5+)
- ★ Good Seller (4.0+)
- - Average Seller (3.5+)
- - Below Average (3.0+)
- ✗ Poor Seller (2.0+)
- ✗✗ Avoid (<2.0)

**Features:**

- 1-5 star rating system
- Average rating calculation
- Reputation titles based on rating
- Per-seller rating history

### Direct Trade

Trade directly with other players without using the auction house.

**Usage:**

```bash
/ac trade send <player> [your_money] [their_money]    # Send trade request
/ac trade accept                                         # Accept pending trade
/ac trade cancel                                         # Cancel pending trade
```

**Features:**

- Direct item-for-item trading
- Money exchange support
- Trade request system with timeout
- Automatic verification of items and money
- Sound notifications on trade completion

**Configuration:**

```yaml
trade:
  enabled: true
  timeout: 60
```

### Buy Order Auto-Matching

New listings are automatically matched with existing buy orders.

**Features:**

- Automatic matching when listing items
- Fills highest price matching buy order
- Checks buyer inventory space before matching
- Notifies seller when listing is auto-matched

**Configuration:**

```yaml
buyorders:
  auto-match: true
```

### Bidding History Improvements

Enhanced bid history with statistics and player history.

**Usage:**

```bash
/ac bidhistory [listing_id]    # View bid history for a listing
/ac bidhistory                  # View your own bid history
```

**Features:**

- Listing bid history with statistics (avg, max, min)
- Personal bid history tracking
- Total spent calculation
- Time-ago formatting for bids

### Auction House Statistics Dashboard

Global auction house statistics for admins.

**Usage:**

```bash
/ac stats dashboard    # View global auction house dashboard (admin only)
```

**Features:**

- Real-time active listings count
- Market volume tracking
- Top categories display
- Market health indicators (trend, activity, demand)
- Time-based statistics (24h, 7d, all time)
- Activity graph visualization

### Player Economy Analytics

Detailed player economy statistics.

**Usage:**

```bash
/ac stats              # View your own economy stats
/ac stats <player>     # View another player's stats (admin only)
```

**Features:**

- Win rate calculation
- Average bid tracking
- ROI (Return on Investment) calculation
- Most sold/bought items
- Favorite category tracking
- Market position ranking (seller, buyer, overall)

### Tax Brackets

Progressive tax system based on listing value.

**Configuration:**

```yaml
tax-brackets:
  enabled: true
  brackets:
    1000: 0.5 # 0.5% tax for listings over 1000
    5000: 1.0 # 1.0% tax for listings over 5000
    10000: 1.5 # 1.5% tax for listings over 10000
    50000: 2.0 # 2.0% tax for listings over 50000
    100000: 2.5 # 2.5% tax for listings over 100000
  exempt-permission: "auctify.tax.exempt"
```

**Features:**

- Progressive tax brackets based on listing value
- Tax exemption permission support
- Automatic tax calculation on sale
- Informative tax bracket notification on listing
- Configurable tax destination (void or server account)
