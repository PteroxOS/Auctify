# Release Notes

## [1.1.5] - 2026-04-25

### Added
- **H2 Database Support**: Added H2 as a new storage backend option
  - H2 is a fast, file-based SQL database with excellent performance for read/write operations
  - File-based like SQLite but significantly faster
  - Built-in support, no additional installation required
  - Full SQL support, easy migration from SQLite
  - Used by popular plugins like LuckPerms
- **Setup Wizard Enhancement**: Added H2 option to the storage selection step
  - H2 is marked as recommended for its performance benefits
  - Backup settings now properly configured based on storage type (SQLite or H2)

### Changed
- Updated version from 1.1.0 to 1.1.5
- Updated README.md with new version and release notes
- Updated wiki.md with current version information
- Updated storage configuration in config.yml to include H2 settings

### Technical Details
- Added H2 dependency (v2.2.224) to pom.xml
- Created H2Storage.java class implementing StorageManager interface
- Added H2 relocation in maven-shade-plugin to avoid conflicts
- Updated Auctify.java to handle H2 storage initialization
- H2 uses MySQL compatibility mode for easier migration

### Storage Options
- **SQLite**: File-based, easy setup (default)
- **H2**: Fast file-based, recommended for performance (NEW)
- **MySQL**: Database server, best for large servers
- **Memory**: No persistence, testing only

### Configuration
```yaml
storage:
  type: h2  # New option
  h2:
    file: "auctify.h2.db"
    mode: "embedded"
    backup:
      enabled: true
      interval: 60
      keep-count: 10
```

### Migration
To migrate from SQLite to H2:
1. Stop the server
2. Change `storage.type` to `h2` in config.yml
3. Start the server (H2 will create a new database)
4. Data migration can be done manually or via SQL export/import

---

## [1.1.0] - Previous Release

### Added
- Price History system
- Auto-Bid feature
- Notification System
- Bulk Selling
- Advanced Search Filters
- Auction Notifications
- Notification Preferences
- Admin Commands enhancements

---

## [1.0.2] - Previous Release

### Added
- Watchlist/Bookmark system
- Auto-Relist feature
- Bulk Cancel command
- Per-world auction house modes
- Item blacklist system

---

## [1.0.1] - Previous Release

### Added
- Automatic database backups
- SQLite backup with retention policy
- Manual backup command
- Schema migration system

---

## [1.0.0] - Initial Release

### Features
- Real-time bidding system
- Buyout system
- GUI-based auction house
- Multi-language support (13 languages)
- Rating system
- Tax system
- Discord webhook integration
- Custom items support (ItemsAdder, Oraxen, MythicMobs)
- Security-first design with race condition protection
- Pending refunds for failed transactions
- Shulker box preview
