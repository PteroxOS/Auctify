# Auctify

Auctify is a professional, high-performance auction house plugin for Minecraft (Paper/Spigot) designed with a focus on real-time interaction, modern aesthetics, and robust security. It provides a seamless bidding experience with a fully localized GUI and integrated Discord notifications.

## Key Features

- **Real-Time Bidding**: Interactive chat-based bidding system that updates the GUI immediately.
- **Dynamic GUI**: Modern chest interface with auto-refresh support for live countdowns.
- **Categorization**: Filter items by Weapons/Tools, Armor, Blocks, and Miscellaneous categories.
- **Persistent Timers**: Auction timers only count down while the server is online. Remaining time is preserved across restarts.
- **Discord Integration**: Comprehensive webhook support with customizable embeds for new listings and sales.
- **Full Localization**: Every string, title, and lore is configurable via per-language YAML files.
- **Automatic Dependency Management**: Automatically downloads and installs required dependencies like Vault if missing.
- **Anti-Exploit System**: Hardened state management to prevent item duplication and economy desync.
- **Auto-Save**: Periodic database flushing to ensure data integrity.

## Dependencies

### Required
- **Vault**: Essential for economy abstraction and transaction management.
- **Economy Provider**: An active economy plugin (e.g., EssentialsX, CMI) linked via Vault.

### Optional
- **PlaceholderAPI**: Enables custom placeholders for external use in scoreboards or tab-lists.
- **ProtocolLib**: Used for advanced packet-level interface protection (optional).

## PlaceholderAPI Support

The following placeholders are available when PlaceholderAPI is installed:

| Placeholder | Description |
|:---|:---|
| `%auctify_total_listings%` | Total number of all active auction listings. |
| `%auctify_total_active%` | Number of listings that are currently active (not expired). |
| `%auctify_player_listings%` | Number of active listings belonging to the specific player. |
| `%auctify_player_balance%` | Formatted economy balance of the player. |
| `%auctify_player_name%` | Display name of the player. |

## Installation

1. Download the latest `Auctify-1.0.0.jar`.
2. Place the jar in your server's `plugins/` folder.
3. Start the server. Required dependencies (Vault) will be downloaded automatically if not present.
4. Configure the database and Discord settings in `config.yml`.
5. Restart or use `/ac reload` to apply changes.

## License

This project is licensed under the MIT License.
