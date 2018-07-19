package nl.rutgerkok.worldgeneratorapi;

import java.util.function.Consumer;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import nl.rutgerkok.worldgeneratorapi.internal.WorldGeneratorApiImpl;
import nl.rutgerkok.worldgeneratorapi.property.PropertyRegistry;

public interface WorldGeneratorApi {

    /**
     * Gets the API instance.
     *
     * @param plugin
     *            The plugin using the world generator API.
     * @param major
     *            The expected major version. If you expect version "1.4 or newer",
     *            the major version is 1.
     * @param minor
     *            The expected minor version. If you expect version "1.4 or newer",
     *            the major version is 4.
     *
     * @return The API.
     */
    public static WorldGeneratorApi getInstance(Plugin plugin, int major, int minor) {
        // If someone accidentally includes WorldGeneratorApi in their plugin,
        // then this will fail with a ClassCastException
        WorldGeneratorApiImpl api = JavaPlugin.getPlugin(WorldGeneratorApiImpl.class);
        if (!api.getApiVersion().isCompatibleWith(major, minor)) {
            api.getLogger().warning(plugin.getName() + " expects "
                    + api.getName() + " v" + major + "." + minor + ". However, this is version v" + api.getApiVersion()
                    + ", which is not compatible. Things may break horribly! You have been warned.");
        }
        return api;
    }

    /**
     * Creates a custom world generator for the given world.
     *
     * @param world
     *            The world to create a custom world generator for.
     * @param consumer
     *            The custom world generator.
     * @return The world generator.
     */
    ChunkGenerator createCustomGenerator(WorldRef world, Consumer<WorldGenerator> consumer);

    /**
     * Gets the version of the API.
     *
     * @return The version.
     */
    Version getApiVersion();

    /**
     * Gets the world generator of the given world.
     *
     * @param world
     *            The world.
     * @return The world generator.
     */
    WorldGenerator getForWorld(World world);

    /**
     * Gets the property registry.
     *
     * @return The property registry.
     */
    PropertyRegistry getPropertyRegistry();
}
