package dev.auctify.listeners;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import dev.auctify.gui.AuctifyHolder;
import dev.auctify.gui.GUIManager;
import dev.auctify.gui.ShulkerPreviewGUI;
import dev.auctify.util.ItemUtil;
import dev.auctify.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

/**
 * Handles all inventory click events within Auctify GUIs.
 * Uses {@link AuctifyHolder} as the primary detection method for Auctify GUIs.
 * This ensures clicks are ALWAYS cancelled even when the GUIManager state
 * is cleared during inventory transitions.
 *
 * <p>
 * The GUIManager is still used for routing logic (which sub-handler to call),
 * but the holder-based check is the security gate.
 * </p>
 */
public class GUIClickListener implements Listener {

    private final Auctify plugin;

    /** @param plugin the main plugin instance */
    public GUIClickListener(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Main click handler. Uses AuctifyHolder to detect Auctify GUIs
     * and unconditionally cancels ALL clicks. Then routes to the
     * correct handler based on the holder's GUI type.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        // PRIMARY SECURITY: Check if the top inventory belongs to Auctify via the
        // holder
        Inventory topInv = event.getView().getTopInventory();
        if (!(topInv.getHolder() instanceof AuctifyHolder holder))
            return;

        // UNCONDITIONALLY cancel ALL clicks when an Auctify GUI is open.
        // This is the single most important line — it prevents ALL item theft.
        event.setCancelled(true);
        event.setResult(org.bukkit.event.Event.Result.DENY);

        // Only allow basic left/right clicks for button interaction
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) {
            return;
        }

        // Ignore clicks in the player's own inventory (bottom half)
        if (event.getClickedInventory() != topInv) {
            return;
        }

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        // Ignore clicks on empty slots or filler items
        if (clicked == null || clicked.getType().isAir())
            return;
        if (isFillerItem(clicked))
            return;

        // Route based on the holder's GUI type (reliable, no HashMap needed)
        String guiType = holder.getGuiType();
        switch (guiType) {
            case "MAIN" -> handleMainGUIClick(player, holder, slot, clicked, event.getClick());
            case "CONFIRM" -> handleConfirmGUIClick(player, holder, slot, clicked);
            case "DETAIL" -> handleDetailGUIClick(player, slot);
            case "MANAGE" -> handleManageGUIClick(player, holder, slot, clicked);
            case "CLAIM" -> handleClaimGUIClick(player, slot, clicked, topInv);
            case "SHULKER" -> handleShulkerGUIClick(player, holder, slot);
            case "RATE" -> handleRateGUIClick(player, holder, slot);
            case "ADMIN" -> handleAdminGUIClick(player, holder, slot, clicked);
        }
    }

    /**
     * Prevents dragging items into any Auctify GUI.
     * Uses AuctifyHolder for detection — same approach as click handler.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Inventory topInv = event.getView().getTopInventory();
        if (!(topInv.getHolder() instanceof AuctifyHolder))
            return;

        // Cancel ALL drags when an Auctify GUI is open — no exceptions
        event.setCancelled(true);
        event.setResult(org.bukkit.event.Event.Result.DENY);
    }

    /**
     * Handles clicks in the main auction GUI: pagination and listing interactions.
     */
    private void handleMainGUIClick(Player player, AuctifyHolder holder, int slot, ItemStack clicked, ClickType click) {
        var config = plugin.getConfig();
        int prevSlot = config.getInt("gui.navigation.previous-page-slot", 45);
        int nextSlot = config.getInt("gui.navigation.next-page-slot", 53);
        int searchSlot = config.getInt("gui.navigation.search-button-slot", 48);
        int infoSlot = config.getInt("gui.navigation.info-slot", 49);
        int categorySlot = config.getInt("gui.buttons.category-cycle-slot", 47);
        int claimSlot = config.getInt("gui.buttons.claim-slot", 46);
        int historySlot = config.getInt("gui.buttons.history-slot", 51);
        int sortSlot = config.getInt("gui.buttons.sort-slot", 52);
        int refreshSlot = config.getInt("gui.buttons.refresh-slot", 48);
        int watchlistSlot = config.getInt("gui.buttons.watchlist-slot", 50);
        int rows = config.getInt("gui.rows", 6);
        if (rows < 3)
            rows = 3;
        if (rows > 6)
            rows = 6;

        GUIManager guiManager = plugin.getGUIManager();
        int currentPage = holder.getPage();

        // Navigation buttons
        if (slot == prevSlot && clicked.getType() == Material.ARROW) {
            if (currentPage > 0) {
                guiManager.cancelRefreshTask(player);
                plugin.getAuctionGUI().open(player, currentPage - 1);
            }
            return;
        }
        if (slot == nextSlot && clicked.getType() == Material.ARROW) {
            // Calculate total pages to prevent going beyond
            int itemsPerPage = config.getInt("gui.items-per-page", -1);
            if (itemsPerPage <= 0)
                itemsPerPage = (rows - 1) * 9;
            int totalListings = (int) plugin.getAuctionManager().getActiveListings().stream()
                    .filter(l -> !l.isExpired())
                    .filter(l -> matchesCategory(l.getItem().getType(), holder.getCategory()))
                    .count();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalListings / itemsPerPage));

            if (currentPage < totalPages - 1) {
                guiManager.cancelRefreshTask(player);
                plugin.getAuctionGUI().open(player, currentPage + 1);
            }
            return;
        }
        if (slot == searchSlot) {
            // Trigger search input via chat
            player.closeInventory();
            plugin.getChatSearchListener().startSearchInput(player);
            return;
        }
        if (slot == infoSlot)
            return; // Info item, do nothing

        // Bottom row clicks that aren't nav buttons
        if (slot >= (rows - 1) * 9) {
            if (slot == categorySlot) {
                // Category Cycle
                String currentCat = holder.getCategory();
                String nextCat = switch (currentCat) {
                    case "ALL" -> "WEAPONS_TOOLS";
                    case "WEAPONS_TOOLS" -> "ARMOR";
                    case "ARMOR" -> "BLOCKS";
                    case "BLOCKS" -> "MISC";
                    default -> "ALL";
                };
                guiManager.cancelRefreshTask(player);
                plugin.getAuctionGUI().open(player, 0, nextCat);
            } else if (slot == refreshSlot && clicked.getType() == Material.SUNFLOWER) {
                guiManager.cancelRefreshTask(player);
                plugin.getAuctionGUI().open(player, currentPage, holder.getCategory());
            } else if (slot == claimSlot && clicked.getType() == Material.CHEST) {
                // Claim/Mailbox button
                guiManager.cancelRefreshTask(player);
                plugin.getClaimGUI().open(player);
            } else if (slot == historySlot && clicked.getType() == Material.CLOCK) {
                plugin.getServer().getScheduler().runTask(plugin, () -> player.closeInventory());
                player.performCommand("ac history");
            } else if (slot == sortSlot && clicked.getType() == Material.COMPARATOR) {
                // Sort cycle
                String currentSort = holder.getSortMode();
                String nextSort = switch (currentSort) {
                    case "TIME_ASC" -> "TIME_DESC";
                    case "TIME_DESC" -> "PRICE_ASC";
                    case "PRICE_ASC" -> "PRICE_DESC";
                    case "PRICE_DESC" -> "BIDS";
                    default -> "TIME_ASC";
                };
                guiManager.cancelRefreshTask(player);
                plugin.getAuctionGUI().open(player, 0, holder.getCategory(), nextSort);
            }
            return;
        }

        // Clicked on a listing item — find which listing this is
        int itemsPerPage = config.getInt("gui.items-per-page", -1);
        if (itemsPerPage <= 0)
            itemsPerPage = (rows - 1) * 9;

        var listings = plugin.getAuctionManager().getActiveListings().stream()
                .filter(l -> !l.isExpired())
                .filter(l -> l.getItem() != null && matchesCategory(l.getItem().getType(), holder.getCategory()))
                .sorted((a, b) -> Long.compare(a.getEndTime(), b.getEndTime()))
                .toList();

        int listingIndex = currentPage * itemsPerPage + slot;
        if (listingIndex >= listings.size())
            return;

        AuctionListing listing = listings.get(listingIndex);

        if (listing.getSellerUUID().equals(player.getUniqueId())) {
            guiManager.cancelRefreshTask(player);
            plugin.getManageListingGUI().open(player, listing);
            return;
        }

        if (click == ClickType.RIGHT) {
            // Right-click: shulker preview if applicable, otherwise item detail
            if (listing.getItem() != null && ShulkerPreviewGUI.isShulkerBox(listing.getItem())) {
                guiManager.cancelRefreshTask(player);
                plugin.getShulkerPreviewGUI().open(player, listing.getItem(), listing.getId());
            } else {
                guiManager.cancelRefreshTask(player);
                plugin.getItemDetailGUI().open(player, listing);
            }
        } else if (click == ClickType.LEFT) {
            // BIN-only listings go straight to buyout
            if (listing.isBinOnly()) {
                guiManager.cancelRefreshTask(player);
                double buyoutPrice = listing.getBuyoutPrice();
                double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
                if (balance < buyoutPrice) {
                    String formattedPrice = plugin.getEconomyManager().format(buyoutPrice);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.closeInventory();
                        MessageUtil.send(player, "insufficient-funds", Map.of("amount", formattedPrice));
                    });
                    return;
                }
                String buyListingId = listing.getId();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                    plugin.getAuctionManager().buyout(player, buyListingId);
                });
            } else {
                guiManager.cancelRefreshTask(player);
                plugin.getConfirmBidGUI().open(player, listing);
            }
        }
    }

    /**
     * Handles clicks in the bid confirmation GUI: confirm, cancel, or buyout.
     */
    private void handleConfirmGUIClick(Player player, AuctifyHolder holder, int slot, ItemStack clicked) {
        String listingId = holder.getListingId();

        if (listingId == null) {
            // No listing tracked, go back to main
            plugin.getAuctionGUI().open(player);
            return;
        }

        Optional<AuctionListing> optListing = plugin.getAuctionManager().getListingById(listingId);
        if (optListing.isEmpty() || optListing.get().isExpired()) {
            MessageUtil.send(player, "listing-not-found", Map.of("listing_id", listingId));
            plugin.getAuctionGUI().open(player);
            return;
        }

        AuctionListing listing = optListing.get();

        if (slot == 11 && clicked.getType() == Material.LIME_WOOL) {
            // Money Verification
            var config = plugin.getConfig();
            double minIncrement = config.getDouble("bidding.min-increment", 10);
            double minBid = listing.hasBids() ? listing.getCurrentBid() + minIncrement : listing.getStartPrice();
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());

            if (balance < minBid) {
                // Insufficient funds — close GUI and inform player
                String formattedMin = plugin.getEconomyManager().format(minBid);
                String formattedBal = plugin.getEconomyManager().format(balance);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                    MessageUtil.send(player, "insufficient-funds",
                            Map.of("amount", formattedMin));
                });
                return;
            }

            // Confirm bid — close GUI first, THEN start chat bid mode
            String listingIdForBid = listing.getId();
            String itemName = ItemUtil.getDisplayName(listing.getItem());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                plugin.getChatBidListener().startBidInput(player, listingIdForBid);
                MessageUtil.send(player, "chat-prompt-bid", Map.of("item", itemName));
            });
        } else if (slot == 15 && clicked.getType() == Material.RED_WOOL) {
            // Cancel — go back to main GUI
            plugin.getAuctionGUI().open(player);
        } else if (slot == 22 && clicked.getType() == Material.GOLD_INGOT) {
            // Money verification for buyout
            double buyoutPrice = listing.getBuyoutPrice();
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
            if (balance < buyoutPrice) {
                String formattedPrice = plugin.getEconomyManager().format(buyoutPrice);
                String formattedBal = plugin.getEconomyManager().format(balance);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                    MessageUtil.send(player, "insufficient-funds",
                            Map.of("amount", formattedPrice));
                });
                return;
            }

            // Buyout
            String buyListingId = listing.getId();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                plugin.getAuctionManager().buyout(player, buyListingId);
            });
        }
    }

    /**
     * Handles clicks in the item detail GUI: only the back button.
     */
    private void handleDetailGUIClick(Player player, int slot) {
        if (slot == 22) {
            // Back button
            plugin.getAuctionGUI().open(player);
        }
    }

    /**
     * Handles clicks in the seller's management GUI.
     */
    private void handleManageGUIClick(Player player, AuctifyHolder holder, int slot, ItemStack clicked) {
        String listingId = holder.getListingId();

        if (listingId == null) {
            plugin.getAuctionGUI().open(player);
            return;
        }

        if (slot == 15 && clicked.getType() == Material.RED_WOOL) {
            // Cancel Listing
            String cancelListingId = listingId;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                plugin.getAuctionManager().cancelListing(player, cancelListingId);
            });
        } else if (slot == 22) {
            // Back button
            plugin.getAuctionGUI().open(player);
        }
    }

    /**
     * Handles clicks in the Claim/Mailbox GUI.
     */
    private void handleClaimGUIClick(Player player, int slot, ItemStack clicked, Inventory inv) {
        int size = inv.getSize();

        if (slot == size - 9 && clicked.getType() == Material.ARROW) {
            // Back button
            plugin.getAuctionGUI().open(player);
            return;
        }

        if (slot == size - 5 && clicked.getType() == Material.LIME_WOOL) {
            // Claim All
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                // MEDIUM-4: Use atomic claimAndClear to prevent TOCTOU duplication
                java.util.List<ItemStack> pending = plugin.getStorageManager()
                        .claimAndClearDeliveries(player.getUniqueId());
                if (pending.isEmpty()) {
                    MessageUtil.send(player, "no-pending-items", null);
                    return;
                }

                int claimed = 0;
                for (ItemStack item : pending) {
                    java.util.HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
                    if (overflow.isEmpty()) {
                        claimed++;
                    } else {
                        // Drop on ground if full
                        for (ItemStack drop : overflow.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop.clone());
                        }
                        claimed++;
                    }
                    // Log each claimed item
                    plugin.getLoggerManager().logClaim(player.getName(),
                            ItemUtil.getDisplayName(item), "ITEM");
                }
                MessageUtil.send(player, "claim-success", java.util.Map.of("count", String.valueOf(claimed)));
            });
        }
    }

    /**
     * Handles clicks in the Shulker Preview GUI.
     */
    private void handleShulkerGUIClick(Player player, AuctifyHolder holder, int slot) {
        if (slot == 31) {
            // Back button — go to detail view
            String listingId = holder.getListingId();
            if (listingId != null) {
                plugin.getAuctionManager().getListingById(listingId)
                        .ifPresent(listing -> plugin.getItemDetailGUI().open(player, listing));
            } else {
                plugin.getAuctionGUI().open(player);
            }
        }
    }

    /**
     * Handles clicks in the Rating GUI.
     */
    private void handleRateGUIClick(Player player, AuctifyHolder holder, int slot) {
        if (slot == 22 && holder.getTargetPlayerUUID() != null) {
            // Skip button
            plugin.getServer().getScheduler().runTask(plugin, () -> player.closeInventory());
            return;
        }

        // Star slots: 11, 12, 13, 14, 15 = 1-5 stars
        if (slot >= 11 && slot <= 15 && holder.getTargetPlayerUUID() != null) {
            int rating = slot - 10;
            java.util.UUID sellerUUID = java.util.UUID.fromString(holder.getTargetPlayerUUID());
            plugin.getStorageManager().saveRating(sellerUUID, player.getUniqueId(), rating);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                MessageUtil.send(player, "rate-success", java.util.Map.of("stars", String.valueOf(rating)));
            });
        }
    }

    /**
     * Handles clicks in the Admin GUI.
     */
    private void handleAdminGUIClick(Player player, AuctifyHolder holder, int slot, ItemStack clicked) {
        int currentPage = holder.getPage();

        // Navigation
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            if (currentPage > 0)
                plugin.getAdminGUI().open(player, currentPage - 1);
            return;
        }
        if (slot == 53 && clicked.getType() == Material.ARROW) {
            plugin.getAdminGUI().open(player, currentPage + 1);
            return;
        }
        if (slot >= 45)
            return; // Bottom nav row, ignore

        // Clicked on a listing — force cancel
        var listings = plugin.getAuctionManager().getActiveListings().stream()
                .filter(l -> l.isActive())
                .sorted((a, b) -> Long.compare(a.getEndTime(), b.getEndTime()))
                .toList();

        int listingIndex = currentPage * 45 + slot;
        if (listingIndex >= listings.size())
            return;

        var listing = listings.get(listingIndex);
        String cancelId = listing.getId();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getAuctionManager().cancelListing(player, cancelId);
            plugin.getAdminGUI().open(player, currentPage);
        });
    }

    /**
     * Checks if an item is a GUI filler (glass pane with blank name).
     */
    private boolean isFillerItem(ItemStack item) {
        if (item == null)
            return false;
        String name = item.getType().name();
        return name.endsWith("_STAINED_GLASS_PANE") || name.equals("GLASS_PANE");
    }

    private boolean matchesCategory(Material mat, String category) {
        if (category == null || "ALL".equals(category))
            return true;
        String name = mat.name();
        switch (category) {
            case "WEAPONS_TOOLS":
                return name.endsWith("_SWORD") || name.endsWith("_PICKAXE") || name.endsWith("_AXE")
                        || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.equals("BOW")
                        || name.equals("CROSSBOW") || name.equals("TRIDENT");
            case "ARMOR":
                return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                        || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.equals("SHIELD");
            case "BLOCKS":
                return mat.isBlock();
            case "MISC":
                return !mat.isBlock() && !name.endsWith("_SWORD") && !name.endsWith("_PICKAXE")
                        && !name.endsWith("_AXE") && !name.endsWith("_SHOVEL") && !name.endsWith("_HOE")
                        && !name.endsWith("_HELMET") && !name.endsWith("_CHESTPLATE")
                        && !name.endsWith("_LEGGINGS") && !name.endsWith("_BOOTS")
                        && !name.equals("BOW") && !name.equals("CROSSBOW") && !name.equals("TRIDENT")
                        && !name.equals("SHIELD");
            default:
                return true;
        }
    }
}
