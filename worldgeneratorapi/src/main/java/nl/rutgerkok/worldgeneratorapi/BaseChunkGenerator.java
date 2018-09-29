package nl.rutgerkok.worldgeneratorapi;

import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import nl.rutgerkok.worldgeneratorapi.property.PropertyRegistry;

/**
 * Generates the basic blocks (just air, stone and water usually) of a chunk.
 *
 */
public interface BaseChunkGenerator {

    /**
     * Contains all information of a generating chunk.
     *
     */
    interface GeneratingChunk {
        /**
         * A biome generator, used for querying biomes outside the chunk.
         *
         * @return The biome generator.
         */
        BiomeGenerator getBiomeGenerator();

        /**
         * The biome grid. Represents the biomes of this chunk. Can be modified.
         * (Although if you want to change biome generation, it is better to register
         * your own {@link BiomeGenerator} instead.)
         *
         * @return The biome grid for the chunk.
         */
        BiomeGrid getBiomesForChunk();

        /**
         * The blocks of the chunk. You should fill the chunk with water and its base
         * material (usually stone).
         *
         * @return The blocks.
         */
        ChunkData getBlocksForChunk();

        /**
         * Gets the chunk x, in chunk coordinates.
         *
         * @return The chunk x.
         */
        int getChunkX();

        /**
         * Gets the chunk z, in chunk coordinates.
         *
         * @return The chunk z.
         */
        int getChunkZ();
    }

    /**
     * Sets the basic blocks (air, stone and water usually) in the chunk. No
     * decorations are applied yet.
     *
     * <p>
     * Note: <strong>this method can be called on any thread</strong>, including the
     * main server thread. As long as you only use the methods contained in the
     * chunk and in the {@link PropertyRegistry property registry}, there's no need
     * to worry about this. However, if you use/call code from other areas (like the
     * rest of the world or an ordinary hash map from your plugin) you will get into
     * trouble. Exceptions may be thrown, or worse: your world may be corrupted
     * silently.
     *
     * @param chunk
     *            The chunk.
     */
    void setBlocksInChunk(GeneratingChunk chunk);
}
