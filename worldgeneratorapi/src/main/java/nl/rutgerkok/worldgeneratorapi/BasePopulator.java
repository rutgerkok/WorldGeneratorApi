package nl.rutgerkok.worldgeneratorapi;

import java.util.Random;

import org.bukkit.HeightMap;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.generator.WorldInfo;

/**
 * Used to generate blocks.
 *
 * @since 1.3
 */
public interface BasePopulator {

    /**
     * Generates the base blocks (usually stone, water and air) that are needed for
     * {@link #generateNoise(WorldInfo, Random, int, int, ChunkData)}.
     *
     * @param worldInfo
     *            The world to generate in.
     * @param random
     *            Random number generator.
     * @param chunkX
     *            The chunk x.
     * @param chunkZ
     *            The chunk z.
     * @param chunkData
     *            The chunk data.
     */
    void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData);

    /**
     * Gets the base height at the given location.
     *
     * @param worldInfo
     *            The world to generate in.
     * @param random
     *            Random number generator.
     * @param blockX
     *            Block x.
     * @param blockZ
     *            Block z.
     * @param heighMap
     *            The desired height type.
     * @return The base height.
     */
    int getBaseHeight(WorldInfo worldInfo, Random random, int blockX, int blockZ, HeightMap heighMap);

}
