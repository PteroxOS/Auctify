# Auctify Usage Guide

This document provides technical information regarding commands, permissions, and advanced configuration for the Auctify plugin.

## Basic Commands

| Command | Description | Permission |
|:---|:---|:---|
| `/ac` | Opens the Auction House GUI. | `auctify.use` |
| `/ac sell <price> [buyout] [duration]` | Lists the item in your hand for auction. | `auctify.sell` |
| `/ac bid <id> <amount>` | Places a bid on a specific item. | `auctify.bid` |
| `/ac search <query>` | Searches for items by name or seller. | `auctify.use` |
| `/ac history` | Views your personal transaction history. | `auctify.use` |
| `/ac cancel <id>` | Cancels your own active auction listing. | `auctify.sell` |
| `/ac reload` | Reloads the configuration and locale files. | `auctify.admin` |

## Permission System

- `auctify.use`: Basic permission to view the GUI and perform searches.
- `auctify.sell`: Permission to list items for auction.
- `auctify.bid`: Permission to place bids on items.
- `auctify.admin`: Full access for administrative commands and modifications.
- `auctify.bypass.maxlistings`: Bypasses the maximum listing limit per player.
- `auctify.bypass.tax`: Exemption from sales tax deductions.

## Discord Webhook Setup

To enable Discord notifications, follow these steps:

1. Create a Webhook in your Discord server (Server Settings > Integrations > Webhooks).
2. Copy the Webhook URL.
3. Open `config.yml` and locate the `discord` section.
4. Change `enabled: false` to `enabled: true`.
5. Paste the URL into the `webhook-url` field.
6. Save the file and run `/ac reload`.

## Localization System

Auctify supports full language customization. Language files are stored in the `locales/` folder.

- To change the primary language, adjust `general.language` in `config.yml`.
- The plugin will automatically inject new keys from updates into your existing files without overwriting your custom translations.
- Supports standard Minecraft color codes (§) and modern MiniMessage formatting.

## Data Storage

The plugin supports two storage backends:
1. **SQLite**: Default, stored in `auctify.db`. Ideal for single-server setups.
2. **MySQL**: Recommended for large servers or networks. Configure connection details in `config.yml`.

The system performs an auto-save every 5 minutes (configurable) to prevent data loss in the event of a sudden server shutdown.
