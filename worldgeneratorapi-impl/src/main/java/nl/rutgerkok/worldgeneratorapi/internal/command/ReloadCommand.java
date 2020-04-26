package nl.rutgerkok.worldgeneratorapi.internal.command;

import java.util.List;
import java.util.Objects;

import org.bukkit.command.CommandSender;

import com.google.common.collect.ImmutableList;

/**
 * Command to reload all custom world generators.
 *
 */
final class ReloadCommand extends WorldGeneratorApiCommand {

    private final Runnable reloader;

    ReloadCommand(Runnable reloader) {
        this.reloader = Objects.requireNonNull(reloader, "reloader");
    }

    @Override
    String getSyntax(String label) {
        return "";
    }

    @Override
    boolean onCommand(CommandSender sender, String label, String[] args) {
        if (args.length != 0) {
            return false;
        }
        reloader.run();
        sender.sendMessage(BASE_COLOR + "All world generators have been reloaded!");
        return true;
    }

    @Override
    List<String> onTabComplete(CommandSender sender, String label, String[] args) {
        return ImmutableList.of();
    }

}
