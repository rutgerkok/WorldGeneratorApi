package nl.rutgerkok.worldgeneratorapi;

import org.bukkit.block.Biome;

/**
 * This class can tell you what biome ends up where in the world.
 *
 */
public interface BiomeGenerator {

    /**
     * Gets zoomed-out biomes for the given region.
     * 
     * @param minX
     *            Min x = blockX / 4
     * @param minZ
     *            Min z = blockZ / 4
     * @param xSize
     *            X size in units of 4 blocks. (xSize == 2 ==> blockXSize == 8)
     * @param zSize
     *            Z size in units of 4 blocks
     * @return The biomes.
     */
    Biome[] getZoomedOutBiomes(int minX, int minZ, int xSize, int zSize);

}
