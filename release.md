# Release Notes

This document summarizes release highlights per version (newest to oldest).

## [1.8.0] - 2026-04-28

### Security Hardening (Extreme Audit v3)

- **FIX-1**: Added auto-bid cascade depth limit to prevent infinite loops.
- **FIX-2**: Added blacklist and BIN-only validation for auto-bid setup.
- **FIX-4**: Added `buyout > start` validation in bulk sell flow.
- **FIX-5**: Replaced shared date formatter usage with `ThreadLocal<SimpleDateFormat>`.
- **FIX-6**: Added early permission validation in `bulkCancelAdmin`.
- **FIX-7**: Fixed TOCTOU race in bulk cancel flow.
- **FIX-8**: Added rating transaction verification and duplicate-rating prevention.
- **FIX-9**: Added watchlist cap per player.
- **FIX-11**: Moved auto-relist counters out of `config.yml` to in-memory tracking.

### Configuration

```yaml
autobid:
  max-cascade-depth: 5

general:
  max-watchlist-per-player: 50
```

### Localization

- Added keys:
  - `autobid-bin-not-allowed`
  - `rate-self-not-allowed`
  - `rate-no-transaction`
  - `rate-already-rated`
  - `watchlist-full`

### Build

- Version: `1.8.0`
- Build status: `SUCCESS` (`mvn clean package -DskipTests`)

---

## [1.7.0] - 2026-04-27

### Security Fixes (Critical)

- Fixed GUI click handling to prevent item duplication vectors.
- Added admin permission gate for setup wizard.
- Fixed tab-completion information leakage.
- Hardened listing cancel permission check inside synchronized path.

### Security Improvements (High)

- Added sell validation for `buyout > start`.
- Refactored buyout economy flow for better atomicity.
- Improved GUI state cleanup documentation and behavior.

### Technical

- Version bump from `1.6.0` to `1.7.0`.

---

## [1.6.0] - Major Feature Expansion

### Overview

`v1.6.0` introduced major gameplay and market features in one consolidated release: bulk buying, GUI themes, listing templates, player reputation, direct trading, buy order auto-match, and expanded analytics/localization.

### Version History Note

- The jump from `v1.1.0` to `v1.6.0` was an intentional consolidation of multiple internal development cycles.
- Goals:
  - Better integration between new systems
  - More complete end-to-end testing
  - Lower deployment complexity for server admins
  - Consistent localization across new features

### New Features

#### Bulk Buying System

- Command: `/ac bulkbuy <add|remove|list|clear|buy>`
- Shopping-cart style multi-listing purchase
- Total cost preview before checkout
- One-click mass purchase
- Inventory space and balance verification

#### GUI Themes

- Command: `/ac theme <list|set>`
- Theme variants: `default`, `dark`, `light`, `ocean`
- Per-player theme preference support
- Configurable GUI materials/colors

#### Listing Templates

- Command: `/ac template <create|list|delete|use>`
- Save reusable listing presets (start, buyout, duration)
- Fast apply workflow when selling
- Per-player template storage

#### Player Ratings / Reputation

- Command: `/ac rating [player]`
- 1-5 star rating flow after successful purchases
- Reputation tiers based on average rating
- Seller history/statistics visibility

#### Direct Player Trading

- Command: `/ac trade <send|accept|cancel>`
- Direct player-to-player trading flow
- Item and money exchange support
- Timeout + pre-completion verification checks

#### Buy Order Auto-Matching

- New listings can auto-match existing buy orders
- Highest matching buy order prioritized first
- Buyer inventory checks during matching
- Seller notification on successful auto-match

#### Bidding History Enhancements

- Command: `/ac bidhistory [listing_id]`
- Added stats (average, max, min bids)
- Added personal bid history and total spend visibility

#### Auction House Statistics Dashboard

- Command: `/ac stats dashboard` (admin)
- Active listing, volume, category and market indicators
- Time-window stats (`24h`, `7d`, `all-time`)

#### Player Economy Analytics

- Command: `/ac stats [player]`
- Win rate, average bid, ROI, category preference, rankings

#### Tax Brackets System

- Progressive tax by listing value
- Configurable bracket thresholds/percentages
- Permission exemption: `auctify.tax.exempt`

#### Category & Sorting Improvements

- Added categories: `FOOD`, `POTIONS`, `REDSTONE`, `DECORATIONS`
- Added sort modes: `PRICE_PER_UNIT_ASC`, `PRICE_PER_UNIT_DESC`, `NEWEST`, `ENDING_SOON`

### Localization

- Locale updates completed for 13 languages (`en`, `id`, `de`, `es`, `fr`, `ja`, `ko`, `nl`, `pl`, `pt_br`, `ru`, `tr`, `zh_cn`).
- `.gitignore` updated to keep `en.yml` and `id.yml` as tracked reference locales.

### Permissions Added

- `auctify.watchlist`, `auctify.claim`, `auctify.cancel`, `auctify.bulksell`
- `auctify.bulkbuy`, `auctify.filter`, `auctify.theme`, `auctify.template`
- `auctify.rating`, `auctify.trade`, `auctify.pricehistory`, `auctify.autobid`
- `auctify.notifications`, `auctify.buyorder`, `auctify.ping`, `auctify.about`
- `auctify.admin.history`, `auctify.admin.stats`, `auctify.bypass.fee`, `auctify.tax.exempt`

### Bug Fixes

- Fixed duplicated admin GUI header in `id.yml`
- Added missing category keys to `id.yml`
- Added missing sort keys to `id.yml`
- Added missing bid-history player keys to `id.yml`
- Added missing tax-brackets section to `id.yml`
- Synced `id.yml` with `en.yml`

### Configuration Added

```yaml
gui:
  theme: "default"

tax-brackets:
  enabled: true

trade:
  enabled: true
  timeout: 60

bulkbuy:
  enabled: true
  max-items: 10

buyorders:
  auto-match: true
```

### Database Changes

- New tables:
  - `auctify_templates`
  - `auctify_ratings`
  - `auctify_trade_requests`
  - `auctify_tax_brackets`
- Updated tables:
  - `auctify_listings`
  - `auctify_price_history`

### Compatibility & Migration

- Backward compatible (no breaking changes)
- Automatic migration on startup from older schemas
- Recommended update path:
  1. Backup database
  2. Stop server
  3. Replace jar with `Auctify-1.6.0.jar`
  4. Start server and let migration run
  5. Review new config sections

---

## [1.1.5] - 2026-04-25

### Added

- H2 database backend support.
- Setup wizard enhancement with H2 selection.

### Changed

- Storage configuration updated to include H2 settings.
- Documentation updated for H2 support.

### Technical

- Added H2 dependency (`2.2.224`) and storage implementation.
- Added H2 relocation in shade plugin.

### Storage Options

- SQLite (default)
- H2 (recommended file-based performance)
- MySQL (large servers)
- Memory (testing)

---

## [1.1.0] - Previous Release

### Added

- Price history
- Auto-bid system
- Notification system
- Bulk selling
- Advanced search filters
- Admin command improvements

---

## [1.0.2] - Previous Release

### Added

- Watchlist/bookmark
- Auto-relist
- Bulk cancel
- Per-world auction modes
- Item blacklist

---

## [1.0.1] - Previous Release

### Added

- Automatic database backups
- Backup retention policy
- Manual backup command
- Schema migration support

---

## [1.0.0] - Initial Release

### Features

- Real-time bidding
- Buyout system
- GUI auction house
- Multi-language support
- Rating system
- Tax system
- Discord webhook integration
- Custom item support
- Race-condition-aware design
- Pending refund queue
- Shulker box preview
