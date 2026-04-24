# Auctify - Minecraft Auction House Plugin for PaperMC & Spigot

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Paper/Spigot](https://img.shields.io/badge/API-Paper%2FSpigot%201.18%2B-green.svg)](https://papermc.io/)
[![Version](https://img.shields.io/badge/Version-1.1.5-blue.svg)](https://github.com/PteroxOS/Auctify/releases)
[![Open Source](https://img.shields.io/badge/Open%20Source-Yes-success.svg)](https://github.com/PteroxOS/Auctify)
[![Downloads](https://img.shields.io/github/downloads/PteroxOS/Auctify/total?style=for-the-badge)](https://github.com/PteroxOS/Auctify/releases)
[![Latest Release](https://img.shields.io/github/v/release/PteroxOS/Auctify?style=for-the-badge)](https://github.com/PteroxOS/Auctify/releases)

Auctify is a professional, high-performance auction house plugin for Minecraft (Paper/Spigot 1.18+) designed with a focus on real-time interaction, modern aesthetics, and **bank-grade security**. It provides a seamless bidding experience with a fully localized GUI, integrated Discord notifications, and comprehensive exploit protection.

> **Latest: v1.1.5** — H2 Database Support for improved performance!

## 🛡️ Security-First Design (v1.0.0+)

Auctify has undergone a complete security audit and refactor with the following protections:

- **🔒 Per-Listing Synchronization**: All bid/buyout/cancel operations use per-listing locks to prevent race conditions
- **💰 Economy Transaction Safety**: Failed deposits are automatically queued as "pending refunds" for delivery on next login
- **🛡️ TOCTOU Protection**: Atomic claim-and-clear operations prevent item duplication on delivery
- **📊 Tax Bypass Hardening**: Tax exemption is snapshotted at listing creation time and persisted to storage
- **🚫 Input Validation**: NaN/Infinity/negative values are rejected; bid timeouts prevent stale input sessions
- **🔍 Info Leak Prevention**: Tab completion only shows listings the player can actually interact with
- **⚡ Event State Integrity**: Events fire before state mutation to ensure clean cancellation rollbacks
- **💾 Automatic Backups**: SQLite database auto-backup with configurable retention (v1.0.1)

## Key Features

- **🧙 Interactive Setup Wizard**: 9-step chat-based configuration for first-time setup (`/ac setup`)
- **📁 Transaction Logs**: All auction activity logged to `logs/` folder with daily rotation
- **🌍 Multi-Language**: 13 languages! EN, ID, ES, PT_BR, RU, DE, FR, PL, TR, ZH_CN, JA, KO, NL
- **🌎 Per-World Auction House**: Global, per-world, or blacklist mode for world restrictions
- **🚫 Item Blacklist**: Prevent listing specific materials (admin configurable)
- **👁️ Watchlist/Bookmark**: Track auctions without bidding — `/ac watchlist [id]`
- **🔄 Auto-Relist**: Automatically relist expired auctions with configurable discount
- **💰 Listing Fee**: Percentage-based fee when creating listings (bypass with permission)
- **💾 Automatic Backups**: SQLite database auto-backup with retention policy
- **📝 Bid History**: Track all bids on every auction with `/ac bidhistory <id>`
- **⏱️ Auction Extension**: Sellers can extend auction time if no bids placed (`/ac extend`)
- **🛡️ Sniping Protection**: Auto-extend auctions by 30s when bid placed in last 30 seconds
- **🔒 Private Auctions**: Whitelist specific players who can bid on your auction
- **📦 Bulk Cancel**: Cancel all your active auctions at once with `/ac bulkcancel`
- **🔔 Offline Notifications**: Get notified of sold auctions when you rejoin the server
- **📊 Permission-based Limits**: Default 3 listings, configurable per-rank (5/10/unlimited)
- **Real-Time Bidding**: Interactive chat-based bidding system with config-driven timeout (default 30s)
- **Dynamic GUI**: Modern chest interface with auto-refresh support for live countdowns
- **Buyout System**: Set instant-buy prices alongside bidding, or list as BIN-only
- **Mailbox / Claim System**: Dedicated `/ac claim` GUI with atomic delivery (prevents duplication)
- **Pending Refunds**: Failed economy transactions are saved and delivered on player login
- **Shulker Box Preview**: Inspect contents of Shulker boxes listed for sale via right-click
- **Advanced Sorting**: Sort listings by time, price, and bid count directly from the GUI
- **Rating System**: Leave ratings for sellers after a successful purchase
- **Categorization**: Filter items by Weapons/Tools, Armor, Blocks, and Miscellaneous
- **Persistent Timers**: Auction timers only count down while the server is online
- **Discord Integration**: Webhook support with player head avatars, expired auction notifications
- **🏆 PlaceholderAPI**: Leaderboard placeholders (%auctify_top_seller%, %auctify_top_bidder%)
- **Full Localization**: Every string configurable via per-language YAML files (EN/ID/ES included)
- **Auto-Save**: Periodic database flushing to ensure data integrity
- **Admin Moderation**: Admin panel (`/ac admin`) to manage listings, blacklist, and backups
- **Tax System**: Configurable tax percentage with "void" or "server-account" destination
- **Custom Items Support**: Full compatibility with ItemsAdder, Oraxen, MythicMobs, and other custom item plugins (preserves all NBT data, lore, and custom display names)
- **📈 Price History**: Track price trends for items with `/ac pricehistory [item_type]` (v1.1.0)
- **🤖 Auto-Bid**: Set maximum bid amounts and let the system automatically bid for you (v1.1.0)
- **🔔 Notification System**: Customize which events trigger notifications (outbid, buyout, auction won, etc.) (v1.1.0)

## Dependencies

### Required

- **Vault**: Essential for economy abstraction and transaction management
- **Economy Provider**: EssentialsX, CMI, or similar linked via Vault
- **Paper/Spigot 1.18+**: Modern API features required

### Optional

- **PlaceholderAPI**: Custom placeholders for scoreboards and tab-lists
- **ProtocolLib**: Advanced packet-level protection
- **ItemsAdder/Oraxen/MythicMobs**: Custom item plugins fully supported — all NBT data and display names are preserved when listing/selling custom items

## PlaceholderAPI Support

| Placeholder                 | Description                                       |
| :-------------------------- | :------------------------------------------------ |
| `%auctify_total_listings%`  | Total number of all active auction listings       |
| `%auctify_total_active%`    | Number of listings currently active (not expired) |
| `%auctify_player_listings%` | Active listings belonging to the specific player  |
| `%auctify_player_balance%`  | Formatted economy balance of the player           |
| `%auctify_player_name%`     | Display name of the player                        |

## Quick Command Reference

```bash
# Buyer Commands
/ac open                    # Open auction house GUI
/ac bid <id> <amount>       # Place bid via command
/ac bidhistory <id>         # View bid history for a listing
/ac watchlist [id]          # View or toggle watchlist (v1.0.2)
/ac search <query>          # Search listings
/ac claim                   # Collect pending items/refunds
/ac history                 # View your auction history
/ac pricehistory [item]     # View price trends for items (v1.1.0)
/ac autobid <action>        # Manage auto-bid settings (v1.1.0)
/ac notifications <action>  # Manage notification preferences (v1.1.0)

# Seller Commands
/ac sell <start> [buyout] [duration]   # List item in hand
/ac cancel <id>             # Cancel your listing
/ac extend <id> <minutes>   # Extend auction time (v1.0.1)
/ac bulkcancel              # Cancel all your auctions (v1.0.1)

# Admin Commands
/ac setup                   # Run interactive setup wizard (v1.0.1)
/ac admin                   # Open admin moderation panel
/ac admin blacklist <add|remove|list>  # Manage blacklist
/ac admin backup            # Manual database backup (v1.0.1)
/ac reload                  # Reload config and locales
```

See [GUIDE.md](GUIDE.md) for detailed usage instructions.

## Installation

### Quick Setup (v1.1.0+ - Recommended)

1. Download the latest `Auctify-1.1.0.jar` from [releases](https://github.com/PteroxOS/Auctify/releases)
2. Place the jar in your server's `plugins/` folder
3. Start the server — Setup Wizard will prompt admins automatically
4. Click through the 7-step wizard to configure language, storage, tax, etc.
5. Plugin auto-reloads with your settings!

### Manual Setup (Legacy)

1. Download `Auctify.jar` and place in `plugins/`
2. Start server to generate default config
3. Edit `config.yml` manually
4. Run `/ac reload` or restart

## Configuration Highlights

### First-Run Setup Wizard

```yaml
system:
  first-run: true # Set to true to re-run setup wizard on next start
```

Run `/ac setup` anytime to reconfigure without editing files.

### Tax System

```yaml
economy:
  tax-percent: 5.0 # Percentage deducted from sales
  tax-destination: "void" # Options: "void", "server-account"
  tax-account-name: "server" # Vault bank account for tax (if server-account)
```

### Bid Input Timeout

```yaml
gui:
  bid-input-timeout: 30 # Seconds before chat bid input expires
```

### Storage & Backup

```yaml
storage:
  type: sqlite # Options: memory, sqlite, mysql

  sqlite:
    file: "auctify.db"
    backup:
      enabled: true # Auto-backup database
      interval: 60 # Minutes between backups
      keep-count: 10 # Number of backups to retain
```

### Price History (v1.1.0)

```yaml
price-history:
  enabled: true
  retention-days: 30 # Days to keep price history records
  max-entries-per-item: 100 # Max price history entries per item type
```

### Auto-Bid (v1.1.0)

```yaml
autobid:
  enabled: true
  max-auto-bids-per-player: 5 # Maximum active auto-bids per player
  min-bid-increment: 0.1 # Minimum bid increment for auto-bids
```

### Notification System (v1.1.0)

```yaml
notifications:
  enabled: true
  default-enabled: true # Default notification preference for new players
  types:
    outbid: true
    buyout: true
    auction-won: true
    item-sold: true
    expiration: true
```

## Documentation

- **[GUIDE.md](GUIDE.md)** — Detailed commands, permissions, and usage
- **[STRUCTURE.md](STRUCTURE.md)** — Project architecture and security design

## License

This project is licensed under the MIT License.
