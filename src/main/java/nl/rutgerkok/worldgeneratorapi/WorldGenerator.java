package nl.rutgerkok.worldgeneratorapi;

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
     * Sets the basic chunk generator. <strong>Calling this method only has effect
     * if the world was created using {@link WorldGeneratorApi}.</strong>
     * 
     * @param base
     *            The base.
     */
    void setBaseChunkGenerator(BaseChunkGenerator base);

}
