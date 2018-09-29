package nl.rutgerkok.worldgeneratorapi;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;
import nl.rutgerkok.worldgeneratorapi.event.WorldGeneratorInitEvent;

/**
 * Represents the world generator of a single world.
 *
 */
public interface WorldGenerator {

    /**
     * Gets the basic chunk generator, which places only stone and water usually.
     * This method only works if a plugin provided a base chunk generator using the
     * {@link #setBaseChunkGenerator(BaseChunkGenerator)} API. It is not yet
     * possible to grab the vanilla base chunk generator yet using this method.
     *
     * @return The basic chunk generator.
     * @throws UnsupportedOperationException
     *             If it is impossible to extract the base chunk generator for this
     *             world. This is the case if the world is not generated using
     *             {@link #setBaseChunkGenerator(BaseChunkGenerator)}.
     */
    BaseChunkGenerator getBaseChunkGenerator() throws UnsupportedOperationException;

    /**
     * Gets the biome generator currently in use. This method will return a valid
     * biome generator for both vanilla and custom worlds.
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
     * flowers and ores to villages and strongholds. At the moment, this method only
     * works if a custom base chunk generator has been set using
     * {@link #setBaseChunkGenerator(BaseChunkGenerator)}.
     *
     * @return The decorator.
     * @throws UnsupportedOperationException
     *             If no base custom chunk generator was set using
     *             {@link #setBaseChunkGenerator(BaseChunkGenerator)}.
     */
    WorldDecorator getWorldDecorator() throws UnsupportedOperationException;;

    /**
     * Gets a reference to the world this generator is active for.
     *
     * @return The world reference.
     */
    WorldRef getWorldRef();

    /**
     * Sets the basic chunk generator. This method should only be called before the
     * world is initialized (so in {@link WorldGeneratorInitEvent} or in
     * {@link Plugin#getDefaultWorldGenerator(String, String)}), otherwise some
     * chunks will generate using the default Minecraft world generator.
     *
     * @param base
     *            The base.
     */
    void setBaseChunkGenerator(BaseChunkGenerator base);

}
