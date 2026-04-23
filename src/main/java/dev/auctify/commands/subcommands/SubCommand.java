package dev.auctify.commands.subcommands;

import org.bukkit.command.CommandSender;

/**
 * Interface for all /ac subcommands.
 * Each subcommand implements this to define its behavior,
 * whether it requires a player context, and its execution logic.
 */
public interface SubCommand {

    /**
     * Executes the subcommand.
     *
     * @param sender the command sender
     * @param args   the full command arguments (including the subcommand name at index 0)
     */
    void execute(CommandSender sender, String[] args);

    /**
     * Whether this subcommand can only be run by a Player (not console).
     *
     * @return true if player-only
     */
    boolean isPlayerOnly();
}
