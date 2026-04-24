# Auctify

Auctify is a professional, high-performance auction house plugin for Minecraft (Paper/Spigot 1.18+) designed with a focus on real-time interaction, modern aesthetics, and **bank-grade security**. It provides a seamless bidding experience with a fully localized GUI, integrated Discord notifications, and comprehensive exploit protection.

## 🛡️ Security-First Design (v1.0.0)

Auctify has undergone a complete security audit and refactor with the following protections:

- **🔒 Per-Listing Synchronization**: All bid/buyout/cancel operations use per-listing locks to prevent race conditions
- **💰 Economy Transaction Safety**: Failed deposits are automatically queued as "pending refunds" for delivery on next login
- **🛡️ TOCTOU Protection**: Atomic claim-and-clear operations prevent item duplication on delivery
- **📊 Tax Bypass Hardening**: Tax exemption is snapshotted at listing creation time and persisted to storage
- **🚫 Input Validation**: NaN/Infinity/negative values are rejected; bid timeouts prevent stale input sessions
- **🔍 Info Leak Prevention**: Tab completion only shows listings the player can actually interact with
- **⚡ Event State Integrity**: Events fire before state mutation to ensure clean cancellation rollbacks

## Key Features

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
- **Discord Integration**: Webhook support with customizable embeds for listings and sales
- **Full Localization**: Every string configurable via per-language YAML files (EN/ID included)
- **Auto-Save**: Periodic database flushing to ensure data integrity
- **Admin Moderation**: Admin panel (`/ac admin`) to manage listings and blacklist players
- **Tax System**: Configurable tax percentage with "void" or "server-account" destination

## Dependencies

### Required

- **Vault**: Essential for economy abstraction and transaction management
- **Economy Provider**: EssentialsX, CMI, or similar linked via Vault
- **Paper/Spigot 1.18+**: Modern API features required

### Optional

- **PlaceholderAPI**: Custom placeholders for scoreboards and tab-lists
- **ProtocolLib**: Advanced packet-level protection

## Dependencies

### Required

- **Vault**: Essential for economy abstraction and transaction management.
- **Economy Provider**: An active economy plugin (e.g., EssentialsX, CMI) linked via Vault.

### Optional

- **PlaceholderAPI**: Enables custom placeholders for external use in scoreboards or tab-lists.
- **ProtocolLib**: Used for advanced packet-level interface protection (optional).

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
/ac search <query>          # Search listings
/ac claim                   # Collect pending items/refunds
/ac history                 # View your auction history

# Seller Commands
/ac sell <start> [buyout] [duration]   # List item in hand
/ac cancel <id>             # Cancel your listing

# Admin Commands
/ac admin                   # Open admin moderation panel
/ac admin blacklist <add|remove|list>  # Manage blacklist
/ac reload                  # Reload config and locales
```

See [GUIDE.md](GUIDE.md) for detailed usage instructions.

## Installation

1. Download the latest `Auctify.jar` from releases
2. Place the jar in your server's `plugins/` folder
3. Start the server — Vault will be auto-downloaded if missing
4. Configure `config.yml` (database, Discord, tax settings)
5. Restart or use `/ac reload` to apply changes

## Configuration Highlights

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

### Storage Options

```yaml
storage:
  type: sqlite # Options: memory, sqlite, mysql
  # MySQL settings for production servers
```

## Documentation

- **[GUIDE.md](GUIDE.md)** — Detailed commands, permissions, and usage
- **[STRUCTURE.md](STRUCTURE.md)** — Project architecture and security design

## License

This project is licensed under the MIT License.
