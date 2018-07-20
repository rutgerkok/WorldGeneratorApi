package nl.rutgerkok.worldgeneratorapi;

import org.bukkit.World;

import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;

/**
 * Represents the world generator of a single world.
 *
 */
public interface WorldGenerator {

    /**
     * Gets the basic chunk generator, which places only stone and water usually.
     *
     * @return The basic chunk generator.
     * @throws UnsupportedOperationException
     *             If it is impossible to extract the base chunk generator for this
     *             world. This is the case if the world is not generated using this
     *             API.
     */
    BaseChunkGenerator getBaseChunkGenerator() throws UnsupportedOperationException;

    /**
     * Gets the biome generator currently in use.
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
     * Gets the decorator of this wolrd, which generates decorations ranging from
     * flowers and ores to villages and strongholds.
     * 
     * @return The decorator.
     */
    WorldDecorator getWorldDecorator();

    /**
     * Gets a reference to the world this generator is active for.
     *
     * @return The world reference.
     */
    WorldRef getWorldRef();

    /**
     * Sets the basic chunk generator. <strong>Calling this method only has effect
     * if the world was created using {@link WorldGeneratorApi}.</strong>
     *
     * @param base
     *            The base.
     */
    void setBaseChunkGenerator(BaseChunkGenerator base);

}
