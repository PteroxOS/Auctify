# Auctify v1.0.2 Release Notes

**Release Date:** April 2026  
**Minecraft Version:** Paper/Spigot 1.18+  
**Java Version:** 17+

---

## ✨ New Features (v1.0.2)

### 1. 👁️ Watchlist/Bookmark System
Track auctions without bidding!
- `/ac watchlist` — View your watchlist
- `/ac watchlist <id>` — Toggle watch status for a listing
- Auto-removes expired/ended listings
- Shows current bid amount in watchlist
- **Permission:** `auctify.watchlist`

### 2. 🔄 Auto-Relist System
Automatically relist expired auctions without bids:
- Configurable discount percentage (default: 10%)
- Maximum relist attempts (default: 3)
- Relisted items get new listing ID
- Sellers notified of auto-relist with discount info
- **Config:** `auto-relist.*` in config.yml

### 3. 💰 Listing Fee System
Charge fees when creating listings:
- Percentage-based fee of start price
- Configurable minimum and maximum caps
- Bypass permission for VIP/staff: `auctify.bypass.fee`
- Insufficient funds check before listing
- **Config:** `listing.fee-*` in config.yml

### 4. 🔥 Crash Handler System
Robust error handling and reporting:
- All uncaught exceptions logged to `crash.txt`
- Discord webhook integration for crash alerts
- Separate crash-webhook URL from main notifications
- Timestamp, exception type, and stack trace preserved
- **Files:** `plugins/Auctify/crash.txt`
- **Config:** `discord.crash-webhook.*` in config.yml

---

## 📋 Commands Summary

| Command | Description | Permission |
|---------|-------------|------------|
| `/ac watchlist [id]` | View or toggle watchlist | `auctify.watchlist` |
| `/ac bidhistory <id>` | View bid history | `auctify.bid` |
| `/ac extend <id> <min>` | Extend auction | `auctify.sell` |
| `/ac bulkcancel` | Cancel all your auctions | `auctify.sell` |

---

## 🔐 New Permissions

- `auctify.watchlist` — Use watchlist system (default: true)
- `auctify.bypass.fee` — Bypass listing fee (default: op)

---

## ⚙️ New Configuration Options

```yaml
# Auto-Relist Settings
auto-relist:
  enabled: false
  discount-percent: 10
  max-attempts: 3

# Listing Fee Settings
listing:
  fee-percent: 0
  fee-min: 0
  fee-max: 0

# Crash Notifications
discord:
  crash-webhook:
    enabled: false
    url: "https://discord.com/api/webhooks/..."
```

---

## 🐛 Bug Fixes

- Hardcoded English strings in GUI lore ("Not available", "No bids yet")
- Listing broadcast messages now fully localized
- Bid broadcast messages now fully localized
- Sniping protection broadcast now fully localized

---

## 🗺️ Localization

All new features fully localized:
- **EN** (English) — Complete
- **ID** (Bahasa Indonesia) — Complete

New locale keys added:
- `watchlist-*` (5 keys)
- `auto-relist-success`
- `listing-fee-*` (2 keys)
- `admin-npc-*` (4 keys — removed in final release)

---

## 🔧 Technical Changes

- StorageManager: Added Watchlist interface methods
- MemoryStorage: Watchlist implementation (in-memory)
- SQLite/MySQL: Placeholder methods (future implementation)
- AuctionManager: Added `autoRelistAuction()` method
- AuctionExpiryTask: Added auto-relist check
- CrashHandler: New utility class for exception handling
- DiscordWebhookUtil: Added `sendCrashNotification()`

---

## 📦 Files Added/Modified

**New Files:**
- `WatchlistSubCommand.java`
- `CrashHandler.java`
- `AdminNPCSubCommand.java` (removed in final)
- `AuctionNPC.java` (removed in final)

**Modified:**
- `pom.xml` — Version 1.0.2
- `Auctify.java` — Crash handler registration, NPC removed
- `AuctionManager.java` — Listing fee, auto-relist
- `AuctionExpiryTask.java` — Auto-relist logic
- `TabCompleter.java` — Watchlist tab completion
- `StorageManager.java` — Watchlist interface
- `MemoryStorage.java` — Watchlist implementation
- `DiscordWebhookUtil.java` — Crash notification
- `config.yml` — New config sections
- `en.yml` / `id.yml` — New locale keys
- `README.md` — Updated features list
- `GUIDE.md` — Documentation updates

---

## 🚀 Upgrade Notes

**From v1.0.1:**
1. Stop server
2. Replace JAR file
3. New config options will auto-generate
4. Start server
5. Edit `config.yml` to enable new features (all disabled by default)
6. `/ac reload`

**Database:** No migration needed for SQLite/MySQL. Watchlist uses MemoryStorage (in-memory only) for v1.0.2.

---

## 📝 Known Issues

- Watchlist is MemoryStorage only (resets on restart) — SQLite/MySQL implementation in future release

---

## 🎯 Future Roadmap

- NPC Auctioneer (external plugin integration)
- MySQL/SQLite Watchlist persistence
- Web interface for auction management
- Auction statistics and analytics

---

**Full Changelog:** [GitHub Commits](https://github.com/PteroxOS/Auctify/commits/main)

**Download:** `Auctify-1.0.2.jar` (in `target/` folder after build)

**Support:** [GitHub Issues](https://github.com/PteroxOS/Auctify/issues)

---

*Built with ❤️ by Jephyruu / PteroxOS*
