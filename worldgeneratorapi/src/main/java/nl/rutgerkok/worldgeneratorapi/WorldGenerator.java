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
     * @return Same object as {@link #getBaseTerrainGenerator()}.
     * @throws UnsupportedOperationException
     *             See description of {@link #getBaseTerrainGenerator()}.
     * @deprecated Replaced by {@link #getBaseTerrainGenerator()}.
     */
    @Deprecated
    BaseChunkGenerator getBaseChunkGenerator() throws UnsupportedOperationException;

    /**
     * Gets the basic terrain generator, which places only stone and water usually.
     * It contains a method to query the height at any point in the world, which is
     * useful for planning structures.
     *
     * @return The basic chunk generator.
     * @throws UnsupportedOperationException
     *             If it is not possible to gain fine-grained access to all stages
     *             of world generation. This happens if chunks are generated using
     *             Bukkit's {@link ChunkGenerator}, which has merged several
     *             different stages of world generation. This can also happen if
     *             another plugin is poking around in Minecraft internals.
     * @since 0.3
     */
    BaseTerrainGenerator getBaseTerrainGenerator() throws UnsupportedOperationException;

    /**
     * Gets the biome generator currently in use. This method will return a valid
     * biome generator for both vanilla and custom worlds. Note that modifications
     * to the biomes made in later stages of world generation (using for example
     * {@link BiomeGrid}) will not show up in this biome generator.
     *
     * @return The biome generator.
     * @since 0.1
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
    WorldDecorator getWorldDecorator() throws UnsupportedOperationException;

    /**
     * Gets a reference to the world this generator is active for.
     *
     * @return The world reference.
     * @since 0.1
     */
    WorldRef getWorldRef();

    /**
     * Sets the basic chunk generator. Replaced by
     * {@link #setBaseTerrainGenerator(BaseTerrainGenerator)}.
     *
     * @param base
     *            The base.
     * @deprecated Use {@link #setBaseTerrainGenerator(BaseTerrainGenerator)}.
     * @since 0.1
     */
    @Deprecated
    void setBaseChunkGenerator(BaseChunkGenerator base);

    /**
     * Sets the basic noise generator. If you don't need block-by-block control over
     * your terrain, use this method. (If you do, use
     * {@link #setBaseTerrainGenerator(BaseTerrainGenerator)}.) The shape of your
     * terrain will automatically be modified to accommodate structures like
     * villages.
     *
     * @param base
     *            The base noise generator.
     * @return The noise generator is transformed into a full, block-by-block base
     *         terrain generator, which is returned here.
     * @since 0.3
     */
    BaseTerrainGenerator setBaseNoiseGenerator(BaseNoiseGenerator base);

    /**
     * Sets the basic terrain generator. This method (unlike
     * {@link #setBaseNoiseGenerator(BaseNoiseGenerator)}) provides you total
     * block-by-block control over the shape of your terrain. However, structures
     * might be placed on unsuitable locations, as unlike
     * {@link #setBaseNoiseGenerator(BaseNoiseGenerator)} the terrain shape is not
     * modified to accommodate for structures like villages.
     *
     * @param base
     *            The base terrain.
     * @since 0.3
     */
    void setBaseTerrainGenerator(BaseTerrainGenerator base);

    /**
     * Sets the biome generator of the world. The biome generator decides where each
     * biome is located.
     *
     * @param biomeGenerator
     *            The biome generator.
     * @since 0.5
     * @throws UnsupportedOperationException
     *             If it is not possible to gain fine-grained access to all stages
     *             of world generation. This happens if chunks are generated using
     *             Bukkit's {@link ChunkGenerator}, which has merged several
     *             different stages of world generation. This can also happen if
     *             another plugin is poking around in Minecraft internals.
     */
    void setBiomeGenerator(BiomeGenerator biomeGenerator);

}
