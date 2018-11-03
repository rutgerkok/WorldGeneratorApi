package nl.rutgerkok.worldgeneratorapi;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.plugin.Plugin;

import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;
import nl.rutgerkok.worldgeneratorapi.event.WorldGeneratorInitEvent;

/**
 * Represents the world generator of a single world. The methods in this class
 * should only be called before the world is initialized (so in
 * {@link WorldGeneratorInitEvent} or in
 * {@link Plugin#getDefaultWorldGenerator(String, String)}), otherwise some
 * chunks will generate using the default Minecraft world generator.
 *
 */
public interface WorldGenerator {

    /**
     * Gets the basic chunk generator, which places only stone and water usually.
     *
     * @return The basic chunk generator.
     * @throws UnsupportedOperationException
     *             If it is not possible to gain fine-grained access to all stages
     *             of world generation. This happens if chunks are generated using
     *             Bukkit's {@link ChunkGenerator}, which has merged several
     *             different stages of world generation. This can also happen if
     *             another plugin is poking around in Minecraft internals.
     */
    BaseChunkGenerator getBaseChunkGenerator() throws UnsupportedOperationException;

    /**
     * Gets the biome generator currently in use. This method will return a valid
     * biome generator for both vanilla and custom worlds. Note that modifications
     * to the biomes made in later stages of world generation (using for example
     * {@link BiomeGrid}) will not show up in this biome generator.
     *
     * @return The biome generator.
     */
    BiomeGenerator getBiomeGenerator();

    /**
     * Gets the world this generator is active for.
     *
     * @return The world.
     */
    World getWorld();

    /**
     * Gets the decorator of this world, which generates decorations ranging from
     * flowers and ores to villages and strongholds. At the moment, this method only
     * works if a custom base chunk generator has been set using
     * {@link #setBaseChunkGenerator(BaseChunkGenerator)}.
     *
     * @return The decorator.
     * @throws UnsupportedOperationException
     *             If it is not possible to gain fine-grained access to all stages
     *             of world generation. This happens if chunks are generated using
     *             Bukkit's {@link ChunkGenerator}, which has merged several
     *             different stages of world generation. This can also happen if
     *             another plugin is poking around in Minecraft internals.
     */
    WorldDecorator getWorldDecorator() throws UnsupportedOperationException;;

    /**
     * Gets a reference to the world this generator is active for.
     *
     * @return The world reference.
     */
    WorldRef getWorldRef();

    /**
     * Sets the basic chunk generator.
     *
     * @param base
     *            The base.
     */
    void setBaseChunkGenerator(BaseChunkGenerator base);

}
