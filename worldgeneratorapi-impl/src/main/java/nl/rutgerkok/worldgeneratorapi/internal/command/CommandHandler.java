package nl.rutgerkok.worldgeneratorapi.internal.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;

import nl.rutgerkok.worldgeneratorapi.property.PropertyRegistry;

public final class CommandHandler implements TabExecutor {

    /**
     * Chops off the first element of the given array, if any.
     *
     * @param args
     *            The array.
     * @return A new array.
     */
    private static String[] removeFirst(String[] args) {
        if (args.length == 0) {
            return args;
        }
        String[] result = new String[args.length - 1];
        System.arraycopy(args, 1, result, 0, result.length);
        return result;
    }

    private final Map<String, WorldGeneratorApiCommand> commands = new HashMap<>();

    public CommandHandler(Runnable reloader, PropertyRegistry propertyRegistry) {
        commands.put("set", new PropertyChangeCommand(propertyRegistry));
        commands.put("get", new PropertyChangeCommand(propertyRegistry));
        commands.put("reload", new ReloadCommand(reloader));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        WorldGeneratorApiCommand subCommand = this.commands.get(args[0]);
        if (subCommand == null) {
            return false;
        }
        if (!subCommand.onCommand(sender, args[0], removeFirst(args))) {
            sender.sendMessage(WorldGeneratorApiCommand.ERROR_COLOR + "Correct syntax: /" + label + " " + args[0] + " "
                    + subCommand.getSyntax(args[0]));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return ImmutableList.copyOf(commands.keySet());
        }
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], commands.keySet(), new ArrayList<>());
        }
        // So (args.length >= 2)
        WorldGeneratorApiCommand subCommand = commands.get(args[0]);
        if (command == null) {
            // No such sub command exists
            return ImmutableList.of();
        }
        return subCommand.onTabComplete(sender, args[0], removeFirst(args));
    }
}
