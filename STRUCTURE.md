# Auctify Project Structure

Auctify is built with a modular architecture to facilitate maintenance and scalability. Below is an overview of the package structure and the responsibilities of each component.

## Main Packages (`dev.auctify`)

- **`Auctify.java`**: The main class handling the plugin lifecycle, manager initialization, and command/event registration.
- **`auction`**: Core auction logic, including the `AuctionListing` data model, `AuctionManager` coordinator, and expiry handling.
- **`commands`**: Implementation of the command system using a SubCommand pattern for easy navigation.
- **`economy`**: Abstraction of economy transactions connected via the Vault API.
- **`gui`**: Inventory-based user interface system. Uses `AuctifyHolder` for GUI state management. Includes main Auction, Admin, Claim, Rate, and Shulker Preview GUIs.
- **`hook`**: Third-party integrations such as the PlaceholderAPI expansion.
- **`listeners`**: Handling of Bukkit events, including GUI interactions and real-time chat input.
- **`storage`**: Data persistence abstraction with SQLite and MySQL support using HikariCP for connection pooling. Manages listings, history, ratings, pending deliveries, and blacklists.
- **`util`**: Utility classes for color handling, configuration, dependency downloading, and Discord webhooks.

## Data Models

- **`AuctionListing`**: A single representation of an auction. This object is thread-safe and handles internal bid validation and BIN (Buy-It-Now) states.
- **`BidRecord`**: A record of bid history including the bidder's name, amount, and timestamp.
- **`AuctionHistory`**: Persistence data for auctions that have been completed or cancelled.

## Security & Data Integrity

- **State Validation**: Every GUI click is validated against the listing ID stored in the `AuctifyHolder`.
- **Atomic Transactions**: Withdrawals and deposits are performed sequentially with failure checks to prevent exploitation.
- **Defensive Copying**: ItemStacks in listings and pending deliveries are always cloned when stored or displayed to prevent accidental reference mutation.
- **Pause-on-Offline**: Auction timing logic converts to remaining time during storage, ensuring that auction timers only run when the server is active.
- **Blacklist Moderation**: Staff can prevent malicious players from using the economy system through an integrated blacklist.
