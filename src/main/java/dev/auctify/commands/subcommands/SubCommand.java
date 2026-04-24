package dev.auctify.commands.subcommands;

import org.bukkit.command.CommandSender;

/**
 * Interface for all /ac subcommands. Each subcommand implements this to define
 * its behavior, whether it requires a player context, and its execution logic.
 */
public interface SubCommand {

    /** Executes the subcommand. */
    void execute(CommandSender sender, String[] args);

    /**
     * Returns true if this subcommand can only be run by a Player (not console).
     */
    boolean isPlayerOnly();
}
