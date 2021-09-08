package nl.rutgerkok.worldgeneratorapi;

import javax.annotation.Nullable;

import org.bukkit.block.data.BlockData;
import org.bukkit.generator.WorldInfo;


/**
 * Supplies noise values - values lower than 0 results in air or water, values
 * higher than 0 in stone.
 *
 * @since 1.3
 */
public interface BaseNoiseProvider {

    /**
     * Terrain settings for the noise generator.
     *
     * @since 1.3
     *
     */
    public static final class TerrainConfig {
        /**
         * The block used as stone. Set to null to use the server default.
         *
         * @since 1.3
         */
        @Nullable
        public BlockData stoneBlock = null;

        /**
         * The block used as water. Set to null to use the server default.
         *
         * @since 1.3
         */
        @Nullable
        public BlockData waterBlock = null;

        /**
         * The water level. Set to a negative value to use the server default.
         *
         * @since 1.3
         */
        public int seaLevel = -1;


    }

    /**
     * Calculates the noise for a column of 4x4 blocks wide.
     *
     * @param worldInfo
     *            The world the noise is generated for.
     * @param buffer
     *            Every entry represents (x y z) 4x8x4 blocks. Entry zero is at
     *            bedrock level and from there it goes upwards. Values lower than 0
     *            result in air or water, values above 0 in stone. The array has a
     *            length of 33.
     * @param x
     *            Column x = blockX / 4
     * @param z
     *            Column z = blockZ / 4
     * @see #getTerrainSettings() Changing the stone block, the water block and the
     *      water height.
     * @since 1.3
     */
    void getNoise(WorldInfo worldInfo, double[] buffer, int x, int z);

    /**
     * Gets the desired terrain settings. This method must return a new instance,
     * but with the same settings every time it is called. (If you want different
     * chunks to have different settings, replace blocks afterwards.)
     *
     * @return The terrain settings.
     * @since 0.3
     */
    default TerrainConfig getTerrainSettings() {
        return new TerrainConfig();
    }

}
