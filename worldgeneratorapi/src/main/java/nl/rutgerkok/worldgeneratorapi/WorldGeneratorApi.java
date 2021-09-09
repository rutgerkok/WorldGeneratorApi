package nl.rutgerkok.worldgeneratorapi;

import java.util.Random;
import java.util.function.Consumer;

import org.bukkit.HeightMap;
import org.bukkit.World;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

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

        // Detect old plugins
        if (major < 1 || (major == 1 && minor < 3)) {
            ourselves.getLogger().warning(plugin.getName() + " v" + plugin.getDescription().getVersion()
                    + ", which was built for " + ourselves.getName() + " v" + major + "." + minor
                    + ", will not be compatible with " + ourselves.getName() + " v2.0. Please look for an update of "
                    + plugin.getName());
        }
        return api;
    }


    /**
     * Creates a terrain generator from a noise function. To use this from your
     * {@link ChunkGenerator} implementation, call
     * {@link BasePopulator#generateNoise(WorldInfo, Random, int, int, ChunkData)}
     * from
     * {@link ChunkGenerator#generateNoise(WorldInfo, Random, int, int, ChunkData)},
     * and also call
     * {@link BasePopulator#getBaseHeight(WorldInfo, Random, int, int, HeightMap)}
     * from
     * {@link ChunkGenerator#getBaseHeight(WorldInfo, Random, int, int, HeightMap)}
     * .
     *
     * @param noiseProvider
     *            The noise provider.
     * @return The terrain generator.
     * @since 1.3
     */
    BasePopulator createBasePopulatorFromNoiseFunction(BaseNoiseProvider noiseProvider);

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
     * must listen for the WorldGeneratorInitEvent and modify the world generator
     * inside that event.
     *
     * @param world
     *            The world to create a custom world generator for.
     * @param consumer
     *            The custom world generator.
     * @return The world generator.
     * @since 0.2
     * @deprecated Implement {@link ChunkGenerator} yourself - it nowadays has an
     *             expanded API. If you used a {@link BaseNoiseGenerator}, we have
     *             {@link #createBasePopulatorFromNoiseFunction(BaseNoiseProvider)}
     *             for you.
     */
    @Deprecated(forRemoval = true)
    ChunkGenerator createCustomGenerator(WorldRef world, Consumer<WorldGenerator> consumer);

    /**
     * Gets the version of the API.
     *
     * @return The version.
     * @since 0.1
     */
    Version getApiVersion();

    /**
     * Gets the used biome provider for the given world. This method can only be
     * called once the world is loaded. In practice, that means that you need to
     * call this method "just in time", so once you're actually generating terrain.
     *
     * <p>
     * This method will either directly return the biome provider set by a plugin,
     * or (unlike Bukkit's b {@link World#getBiomeProvider()}) a wrapper around the
     * vanilla biome provider. You can store the biome provider returned by this
     * method, in case you want to use it for your own biome provider.
     *
     * @param world
     *            The world.
     * @return The biome provider.
     * @throws IllegalStateException
     *             If the world isn't accessible yet.
     * @see #setBiomeProvider(World, BiomeProvider) For setting the biome provider
     *      during {@link WorldInitEvent}.
     * @since 1.3
     */
    BiomeProvider getBiomeProvider(WorldInfo world) throws IllegalStateException;

    /**
     * Gets the world generator of the given world.
     *
     * @param world
     *            The world.
     * @return The world generator.
     * @since 0.1
     * @deprecated Use the new Bukkit methods in {@link ChunkGenerator}, use
     *             {@link Plugin#getDefaultBiomeProvider(String, String)}, or add
     *             your {@link BlockPopulator} to the world.
     */
    @Deprecated(forRemoval = true)
    WorldGenerator getForWorld(World world);

    /**
     * Gets the property registry.
     *
     * @return The property registry.
     * @since 0.1
     */
    PropertyRegistry getPropertyRegistry();

    /**
     * Injects a biome provider in the world. This method is intended to be called
     * during the {@link WorldInitEvent}.
     *
     * <p>
     * Note: you can also just set the biome provider by overriding the
     * {@link Plugin#getDefaultBiomeProvider(String, String)} method, or by
     * overriding the {@link ChunkGenerator#getDefaultBiomeProvider(WorldInfo)}.
     * This method is only useful if your biome provider needs to make a few
     * modifications to the existing one, retrieved using
     * {@link #getBiomeProvider(WorldInfo)}. For example, you could create a biome
     * provider that changes all forests into flower forests.
     *
     * @param world
     *            The world.
     * @param biomeProvider
     *            The biome provider.
     * @since 1.3
     */
    void setBiomeProvider(World world, BiomeProvider biomeProvider);
}
