package nl.rutgerkok.worldgeneratorapi.internal.command;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Base class for all plugin commands.
 */
abstract class WorldGeneratorApiCommand {

    static final ChatColor BASE_COLOR = ChatColor.GREEN;
    static final ChatColor VALUE_COLOR = ChatColor.DARK_GREEN;
    static final ChatColor ERROR_COLOR = ChatColor.DARK_RED;

    /**
     * Gets the command syntax excluding the label, for example "get [world] [biome]
     * <property>"
     * 
     * @param label
     *            The label of the subcommand, for example "reload".
     * 
     * @return The syntax.
     */
    abstract String getSyntax(String label);

    /**
     * Executes the command.
     * @param sender The command sender.
     * @param label The label of the subcommand, for example "reload".
     * @param args The arguments, excluding the label.
     * @return True if successful, false if help should be shown.
     */
    abstract boolean onCommand(CommandSender sender, String label, String[] args);

    /**
     * Gives tab-completion possibilities.
     * @param sender The command sender.
     * @param label The label of the subcommand, for example "reload".
     * @param args The arguments supplied so far, excluding the label.
     * @return A list of tab-completion options.
     */
    abstract List<String> onTabComplete(CommandSender sender, String label, String[] args);
}
