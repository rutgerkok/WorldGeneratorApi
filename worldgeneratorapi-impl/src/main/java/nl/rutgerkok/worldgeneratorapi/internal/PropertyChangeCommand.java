package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.md_5.bungee.api.ChatColor;
import nl.rutgerkok.worldgeneratorapi.WorldRef;
import nl.rutgerkok.worldgeneratorapi.property.AbstractProperty;
import nl.rutgerkok.worldgeneratorapi.property.PropertyRegistry;

/**
 * Syntax:
 *
 * <pre>
 * /&lt;command&gt; set [world] [biome] &lt;property&gt &lt;value&gt;
 * /&lt;command&gt; get [world] [biome] &lt;property&gt
 * </pre>
 */
final class PropertyChangeCommand implements TabExecutor {

    private static class Parameters {
        @Nullable
        World world;
        @Nullable
        Biome biome;
        @Nullable
        AbstractProperty property;

        public Optional<Biome> biome() {
            return Optional.ofNullable(biome);
        }

        public Optional<WorldRef> worldRef() {
            if (this.world == null) {
                return Optional.empty();
            }
            return Optional.of(WorldRef.of(this.world));
        }
    }

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

    /**
     * Chops off the first and last element of the given array. Returns an empty
     * array for arrays with a length of 2 and smaller.
     *
     * @param args
     *            The array.
     * @return A new array.
     */
    private static String[] removeFirstAndLast(String[] args) {
        if (args.length < 2) {
            return new String[0];
        }
        String[] result = new String[args.length - 2];
        System.arraycopy(args, 1, result, 0, result.length);
        return result;
    }

    private final PropertyRegistry propertyRegistry;

    PropertyChangeCommand(PropertyRegistry propertyRegistry) {
        this.propertyRegistry = Objects.requireNonNull(propertyRegistry, "propertyRegistry");
    }

    private Iterable<String> biomeNames(CommandSender sender) {
        List<String> names = new ArrayList<>();
        for (Biome biome : Biome.values()) {
            names.add(biome.getKey().toString());
        }
        return names;
    }

    @Nullable
    private Biome getBiomeWithName(String string) {
        if (!string.toLowerCase(Locale.ROOT).equals(string)) {
            // Biome names must be case sensitive
            return null;
        }

        int colonIndex = string.indexOf(":");
        if (colonIndex != -1 && string.startsWith(NamespacedKey.MINECRAFT + ":")) {
            // Strip off namespace
            string = string.substring(colonIndex + 1);
        }
        try {
            return Biome.valueOf(string.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private AbstractProperty getProperty(String string) {
        int colonIndex = string.indexOf(":");

        NamespacedKey key;
        try {
            if (colonIndex != -1) {
                String namespace = string.substring(0, colonIndex);
                String value = string.substring(colonIndex + 1);
                key = new NamespacedKey(namespace, value);
            } else {
                key = NamespacedKey.minecraft(string);
            }
        } catch (IllegalArgumentException e) {
            return null; // Invalid namespaced key
        }
        return this.propertyRegistry.getRegisteredProperty(key).orElse(null);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args[0].equals("get") && args.length >= 2) {
            Parameters parameters = this.parseParameters(sender.getServer(), removeFirst(args));
            if (parameters.property == null) {
                return false;
            }
            String value = parameters.property.getStringValue(parameters.worldRef(), parameters.biome());
            sender.sendMessage(ChatColor.GREEN + "For biome " + ChatColor.DARK_GREEN
                    + parameters.biome().map(b -> b.getKey().getKey()).orElse("(unspecified)") + ChatColor.GREEN
                    + " and world " + ChatColor.DARK_GREEN
                    + parameters.worldRef().map(w -> w.getName()).orElse("(unspecified)") + ChatColor.GREEN
                    + " the value of " + ChatColor.DARK_GREEN + parameters.property.getKey() + ChatColor.GREEN + " is "
                    + ChatColor.DARK_GREEN + value + ChatColor.GREEN + ".");
            return true;
        }
        if (args[0].equals("set") && args.length >= 3) {
            Parameters parameters = this.parseParameters(sender.getServer(), removeFirstAndLast(args));
            if (parameters.property == null) {
                return false;
            }
            try {
                parameters.property.setStringValue(parameters.worldRef(), parameters.biome(), args[args.length - 1]);
                String newValue = parameters.property.getStringValue(parameters.worldRef(), parameters.biome());
                String warning = "Also note that it is up to the world generator to honor this setting.";
                if (parameters.biome == null) {
                    warning = "Also note that biome-specific settings might override this value.";
                } else if (parameters.world == null) {
                    warning = "Also note that world-specific settings might override this value.";
                }
                sender.sendMessage(ChatColor.GREEN + "For biome " + ChatColor.DARK_GREEN
                        + parameters.biome().map(b -> b.getKey().getKey()).orElse("(unspecified)") + ChatColor.GREEN
                        + " and world " + ChatColor.DARK_GREEN
                        + parameters.worldRef().map(w -> w.getName()).orElse("(unspecified)") + ChatColor.GREEN
                        + " the value of " + ChatColor.DARK_GREEN
                        + parameters.property.getKey() + ChatColor.GREEN + " is now " + ChatColor.DARK_GREEN + newValue
                        + ChatColor.GREEN + ". Note that upon server restart the value will be reset. " + warning);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.DARK_RED + "Could not parse value: " + e.getMessage() + ".");
            } catch (UnsupportedOperationException e) {
                sender.sendMessage(ChatColor.DARK_RED + "This value cannot be changed: " + e.getMessage() + ".");
            }

            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return ImmutableList.of("get", "set");
        }
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], ImmutableList.of("get", "set"), new ArrayList<>());
        }
        if (!args[0].equals("get") && !args[0].equals("set")) {
            // If we have more than 1 argument, require a correct syntax for the first
            // argument
            return ImmutableList.of();
        }
        if (args.length == 2) {
            List<String> suggestions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[1], worldNames(sender), suggestions);
            StringUtil.copyPartialMatches(args[1], biomeNames(sender), suggestions);
            StringUtil.copyPartialMatches(args[1], propertyNames(sender), suggestions);
            return suggestions;
        }
        if (args.length == 3) {
            Parameters parsed = this.parseParameters(sender.getServer(), removeFirst(args));
            List<String> suggestions = new ArrayList<>();
            if (parsed.world != null) {
                StringUtil.copyPartialMatches(args[2], biomeNames(sender), suggestions);
            }
            if ((parsed.world != null || parsed.biome != null) && parsed.property == null) {
                StringUtil.copyPartialMatches(args[2], propertyNames(sender), suggestions);
            }
            return suggestions;
        }
        if (args.length == 4) {
            Parameters parsed = this.parseParameters(sender.getServer(), removeFirst(args));
            if (parsed.world != null && parsed.biome != null) {
                return StringUtil.copyPartialMatches(args[3], propertyNames(sender), new ArrayList<>());
            } else {
                return ImmutableList.of();
            }
        }
        return ImmutableList.of();
    }

    private Parameters parseParameters(Server server, String[] args) {
        Parameters parameters = new Parameters();

        if (args.length == 0) {
            // Nothing to parse
            return parameters;
        }

        // Parse first argument
        parameters.world = server.getWorld(args[0]);
        if (parameters.world == null) {
            // Try as biome
            parameters.biome = getBiomeWithName(args[0]);
            if (parameters.biome == null) {
                // Try as property name
                parameters.property = getProperty(args[0]);
                return parameters; // That was everything we could parse
            }
        }

        if (args.length == 1) {
            // Nothing more to pass
            return parameters;
        }

        // Parse second argument
        if (parameters.biome == null) {
            parameters.biome = getBiomeWithName(args[1]);
        }
        if (parameters.world == null || parameters.biome == null) {
            // Try as property name
            parameters.property = getProperty(args[1]);
            return parameters; // That was everything we could parse
        }

        if (args.length == 2) {
            // Nothing more to parse
            return parameters;
        }

        if (args.length > 3) {
            // Too many parameters, invalid syntax
            return new Parameters();
        }

        // Parse third argument
        parameters.property = getProperty(args[2]);
        return parameters;
    }

    private Iterable<String> propertyNames(CommandSender sender) {
        List<String> names = new ArrayList<>();
        for (AbstractProperty property : propertyRegistry.getAllProperties()) {
            names.add(property.getKey().toString());
        }
        return names;
    }

    private Iterable<String> worldNames(CommandSender sender) {
        return Lists.transform(sender.getServer().getWorlds(), World::getName);
    }

}
