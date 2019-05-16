package nl.rutgerkok.worldgeneratorapi;

import org.bukkit.block.Biome;

/**
 * This class can tell you what biome ends up where in the world.
 *
 */
public interface BiomeGenerator {

    /**
     * Gets normal-scaled biomes for the given region.
     *
     * @param minX
     *            Block x.
     * @param minZ
     *            Block z.
     * @param xSize
     *            X size in blocks.
     * @param zSize
     *            Z size in blocks.
     * @return The biome array, ordered as
     *         {@code biome(x,z) = array[z * xSize + x)}.
     */
    default Biome[] getBiomes(int minX, int minZ, int xSize, int zSize) {
        int zoomedOutXSize = xSize / 4;
        int zoomedOutZSize = zSize / 4;
        Biome[] zoomedOut = getZoomedOutBiomes(minX / 4, minZ / 4, zoomedOutXSize, zoomedOutZSize);
        Biome[] normalScale = new Biome[xSize * zSize];
        for (int i = 0; i < normalScale.length; i++) {
            int x = i % xSize;
            int z = i / xSize;
            normalScale[i] = zoomedOut[(z / 4) * zoomedOutZSize + x / 4];
        }
        return normalScale;
    }

    /**
     * Gets a single biome.
     * @param x X position = blockX / 4.
     * @param z Z position = blockZ / 4.
     * @return The biome.
     */
    default Biome getZoomedOutBiome(int x, int z) {
        return getZoomedOutBiomes(x, z, 1, 1)[0];
    }

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
     * @return The biome array, ordered as
     *         {@code biome(x,z) = array[z * xSize + x)}.
     */
    Biome[] getZoomedOutBiomes(int minX, int minZ, int xSize, int zSize);

}
