# Auctify - Minecraft Auction House Plugin for PaperMC & Spigot

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Paper/Spigot](https://img.shields.io/badge/API-Paper%2FSpigot%201.18%2B-green.svg)](https://papermc.io/)
[![Version](https://img.shields.io/badge/Version-1.8.0-blue.svg)](https://github.com/PteroxOS/Auctify/releases)
[![Open Source](https://img.shields.io/badge/Open%20Source-Yes-success.svg)](https://github.com/PteroxOS/Auctify)
[![Downloads](https://img.shields.io/github/downloads/PteroxOS/Auctify/total?style=for-the-badge)](https://github.com/PteroxOS/Auctify/releases)
[![Latest Release](https://img.shields.io/github/v/release/PteroxOS/Auctify?style=for-the-badge)](https://github.com/PteroxOS/Auctify/releases)

Auctify is a professional auction house plugin for Minecraft (Paper/Spigot 1.18+) with real-time bidding, comprehensive security features, and multi-language support.

## Core Features

**Security & Reliability**

- Per-Listing Synchronization - All bid/buyout/cancel operations use per-listing locks to prevent race conditions
- Economy Transaction Safety - Failed deposits are automatically queued as pending refunds for delivery on next login
- TOCTOU Protection - Atomic claim-and-clear operations prevent item duplication on delivery
- Tax Bypass Hardening - Tax exemption is snapshotted at listing creation time and persisted to storage
- Input Validation - NaN/Infinity/negative values are rejected; bid timeouts prevent stale input sessions
- Info Leak Prevention - Tab completion only shows listings the player can actually interact with
- Event State Integrity - Events fire before state mutation to ensure clean cancellation rollbacks
- Automatic Backups - Database auto-backup with configurable retention
- **Extreme Audit v3 Hardening** - 11 security fixes including auto-bid cascade limits, rating verification, thread-safe date formatting, and watchlist limits

**Auction System**

- Real-Time Bidding - Interactive chat-based bidding system with configurable timeout (default 30s)
- Dynamic GUI - Modern chest interface with auto-refresh support for live countdowns
- Buyout System - Set instant-buy prices alongside bidding, or list as BIN-only
- Bid History - Track all bids on every auction with `/ac bidhistory <id>`
- Auction Extension - Sellers can extend auction time if no bids placed (`/ac extend`)
- Sniping Protection - Auto-extend auctions by 30s when bid placed in last 30 seconds
- Private Auctions - Whitelist specific players who can bid on your auction
- Persistent Timers - Auction timers only count down while the server is online

**Item Management**

- Custom Items Support - Full compatibility with ItemsAdder, Oraxen, MythicMobs, and other custom item plugins (preserves all NBT data, lore, and custom display names)
- Shulker Box Preview - Inspect contents of Shulker boxes listed for sale via right-click
- Item Blacklist - Prevent listing specific materials (admin configurable)
- Categorization - Filter items by Weapons/Tools, Armor, Blocks, and Miscellaneous
- Advanced Sorting - Sort listings by time, price, and bid count directly from the GUI

**User Experience**

- Interactive Setup Wizard - 9-step chat-based configuration for first-time setup (`/ac setup`)
- Multi-Language - 13 languages supported: EN, ID, ES, PT_BR, RU, DE, FR, PL, TR, ZH_CN, JA, KO, NL
- Per-World Auction House - Global, per-world, or blacklist mode for world restrictions
- Watchlist/Bookmark - Track auctions without bidding (`/ac watchlist [id]`)
- Auto-Relist - Automatically relist expired auctions with configurable discount
- Bulk Cancel - Cancel all your active auctions at once with `/ac bulkcancel`
- Offline Notifications - Get notified of sold auctions when you rejoin the server
- Rating System - Leave ratings for sellers after a successful purchase

**Economy & Admin**

- Listing Fee - Percentage-based fee when creating listings (bypass with permission)
- Tax System - Configurable tax percentage with "void" or "server-account" destination
- Permission-based Limits - Default 3 listings, configurable per-rank (5/10/unlimited)
- Mailbox/Claim System - Dedicated `/ac claim` GUI with atomic delivery (prevents duplication)
- Pending Refunds - Failed economy transactions are saved and delivered on player login
- Admin Moderation - Admin panel (`/ac admin`) to manage listings, blacklist, and backups
- Transaction Logs - All auction activity logged to `logs/` folder with daily rotation
- Auto-Save - Periodic database flushing to ensure data integrity

**Advanced Features (v1.1.0+)**

- Price History - Track price trends for items with `/ac pricehistory [item_type]`
- Auto-Bid - Set maximum bid amounts and let the system automatically bid for you
- Notification System - Customize which events trigger notifications (outbid, buyout, auction won, etc.)
- Discord Integration - Webhook support with player head avatars, expired auction notifications
- PlaceholderAPI Support - Leaderboard placeholders (%auctify_top_seller%, %auctify_top_bidder%)

## Dependencies

**Required**

- Vault - Essential for economy abstraction and transaction management
- Economy Provider - EssentialsX, CMI, or similar linked via Vault
- Paper/Spigot 1.18+ - Modern API features required

**Optional**

- PlaceholderAPI - Custom placeholders for scoreboards and tab-lists
- ProtocolLib - Advanced packet-level protection
- ItemsAdder/Oraxen/MythicMobs - Custom item plugins fully supported

## PlaceholderAPI Support

| Placeholder                 | Description                                       |
| :-------------------------- | :------------------------------------------------ |
| `%auctify_total_listings%`  | Total number of all active auction listings       |
| `%auctify_total_active%`    | Number of listings currently active (not expired) |
| `%auctify_player_listings%` | Active listings belonging to the specific player  |
| `%auctify_player_balance%`  | Formatted economy balance of the player           |
| `%auctify_player_name%`     | Display name of the player                        |

## Commands

**Buyer Commands**

- `/ac open` - Open auction house GUI
- `/ac bid <id> <amount>` - Place bid via command
- `/ac bidhistory <id>` - View bid history for a listing
- `/ac watchlist [id]` - View or toggle watchlist
- `/ac search <query>` - Search listings
- `/ac claim` - Collect pending items/refunds
- `/ac history` - View your auction history
- `/ac pricehistory [item]` - View price trends for items
- `/ac autobid <action>` - Manage auto-bid settings
- `/ac notifications <action>` - Manage notification preferences

**Seller Commands**

- `/ac sell <start> [buyout] [duration]` - List item in hand
- `/ac cancel <id>` - Cancel your listing
- `/ac extend <id> <minutes>` - Extend auction time
- `/ac bulkcancel` - Cancel all your auctions

**Admin Commands**

- `/ac setup` - Run interactive setup wizard
- `/ac admin` - Open admin moderation panel
- `/ac admin blacklist <add|remove|list>` - Manage blacklist
- `/ac admin backup` - Manual database backup
- `/ac reload` - Reload config and locales

See [GUIDE.md](GUIDE.md) for detailed usage instructions.

## Installation

**Quick Setup (Recommended)**

1. Download the latest `Auctify-1.6.0.jar` from [releases](https://github.com/PteroxOS/Auctify/releases)
2. Place the jar in your server's `plugins/` folder
3. Start the server - Setup Wizard will prompt admins automatically
4. Click through the wizard to configure language, storage, tax, etc.
5. Plugin auto-reloads with your settings

**Manual Setup**

1. Download `Auctify.jar` and place in `plugins/`
2. Start server to generate default config
3. Edit `config.yml` manually
4. Run `/ac reload` or restart

## Configuration

**First-Run Setup Wizard**

```yaml
system:
  first-run: true # Set to true to re-run setup wizard on next start
```

**Tax System**

```yaml
economy:
  tax-percent: 5.0 # Percentage deducted from sales
  tax-destination: "void" # Options: "void", "server-account"
  tax-account-name: "server" # Vault bank account for tax (if server-account)
```

**Bid Input Timeout**

```yaml
gui:
  bid-input-timeout: 30 # Seconds before chat bid input expires
```

**Storage & Backup**

```yaml
storage:
  type: h2 # Options: memory, sqlite, h2, mysql

  h2:
    file: "auctify.h2.db"
    mode: "embedded"
    backup:
      enabled: true
      interval: 60
      keep-count: 10
```

**Price History**

```yaml
price-history:
  enabled: true
  retention-days: 30
  max-entries-per-item: 100
```

**Auto-Bid**

```yaml
autobid:
  enabled: true
  max-auto-bids-per-player: 5
  min-bid-increment: 0.1
```

**Notification System**

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

## Documentation

- [GUIDE.md](GUIDE.md) - Detailed commands, permissions, and usage
- [STRUCTURE.md](STRUCTURE.md) - Project architecture and security design

## License

This project is licensed under the MIT License.
