package nl.rutgerkok.worldgeneratorapi;

import java.util.function.Consumer;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import nl.rutgerkok.worldgeneratorapi.event.WorldGeneratorInitEvent;
import nl.rutgerkok.worldgeneratorapi.property.PropertyRegistry;

/**
 * Main entry point of the API. See the
 * {@link #createCustomGenerator(WorldRef, Consumer)} method for how to get
 * started.
 *
 * @since 0.1
 */
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
     * @since 0.1
     */
    static WorldGeneratorApi getInstance(Plugin plugin, int major, int minor) {
        // If someone accidentally includes WorldGeneratorApi in their plugin,
        // then this will fail with a ClassCastException
        JavaPlugin ourselves = JavaPlugin.getProvidingPlugin(WorldGeneratorApi.class);
        WorldGeneratorApi api = (WorldGeneratorApi) ourselves;
        if (!api.getApiVersion().isCompatibleWith(major, minor)) {
            ourselves.getLogger().warning(plugin.getName() + " expects "
                    + ourselves.getName() + " v" + major + "." + minor
                    + ". However, this is version v" + api.getApiVersion()
                    + ", which is not compatible. Things may break horribly!"
                    + " You have been warned.");
        }
        return api;
    }


    /**
     * Registers a custom world generator for the given world. Once the world is
     * loaded, the code passed as the second argument of this method will be run.
     * This code must first set the base chunk generator using
     * {@link WorldGenerator#setBaseChunkGenerator(BaseChunkGenerator)}, and can
     * then (optionally) make other modifications.
     *
     * <p>
     * This method is intended to be called from within the
     * {@link Plugin#getDefaultWorldGenerator(String, String)} method. It returns a
     * dummy {@link ChunkGenerator} that can be passed back to Bukkit. This returned
     * {@link ChunkGenerator} is not actually capable of generating terrain. You
     * need to register a base terrain generator
     * ({@link WorldGenerator#setBaseTerrainGenerator(BaseTerrainGenerator)}), which
     * will poke around in Minecraft internals so you base terrain generator is
     * used.
     *
     * <p>
     * If you don't want to modify the base terrain, then you must not use this
     * method. You must also not override Bukkit's
     * {@link Plugin#getDefaultWorldGenerator(String, String)} method. Instead, you
     * must listen for the {@link WorldGeneratorInitEvent} and modify the world
     * generator inside that event.
     *
     * @param world
     *            The world to create a custom world generator for.
     * @param consumer
     *            The custom world generator.
     * @return The world generator.
     * @since 0.2
     */
    ChunkGenerator createCustomGenerator(WorldRef world, Consumer<WorldGenerator> consumer);

    /**
     * Gets the version of the API.
     *
     * @return The version.
     * @since 0.1
     */
    Version getApiVersion();

    /**
     * Gets the world generator of the given world.
     *
     * @param world
     *            The world.
     * @return The world generator.
     * @since 0.1
     */
    WorldGenerator getForWorld(World world);

    /**
     * Gets the property registry.
     *
     * @return The property registry.
     * @since 0.1
     */
    PropertyRegistry getPropertyRegistry();
}
