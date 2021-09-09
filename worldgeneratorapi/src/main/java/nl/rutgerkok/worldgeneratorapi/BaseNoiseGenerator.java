package nl.rutgerkok.worldgeneratorapi;

import javax.annotation.Nullable;

import org.bukkit.block.data.BlockData;
import org.bukkit.generator.WorldInfo;

import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;

/**
 * Supplies noise values - values lower than 0 results in air or water, values
 * higher than 0 in stone.
 *
 * @since 0.3
 * @deprecated This interface uses the old {@link BiomeGenerator} class. You
 *             should therefore use {@link BaseNoiseProvider} instead.
 */
@Deprecated(forRemoval = true)
public interface BaseNoiseGenerator {

    /**
     * Terrain settings for the noise generator.
     *
     * @since 0.3
     *
     */
    public static final class TerrainSettings {
        /**
         * The block used as stone. Set to null to use the server default.
         *
         * @since 0.3
         */
        @Nullable
        public BlockData stoneBlock = null;

        /**
         * The block used as water. Set to null to use the server default.
         *
         * @since 0.3
         */
        @Nullable
        public BlockData waterBlock = null;

        /**
         * The water level. Set to a negative value to use the server default.
         *
         * @since 0.3
         */
        public int seaLevel = -1;


    }

    /**
     * Calculates the noise for a column of 4x4 blocks wide.
     *
     * @param biomeGenerator
     *            The biome generator, in case you want to have biome-specific
     *            noise. Note that you can directly pass the x and z received in
     *            this method to {@link BiomeGenerator#getZoomedOutBiome(int, int)}
     *            to get the biome.
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
     * @since 0.3
     * @deprecated This method uses the old biome generator class. Use
     *             {@link BaseNoiseProvider#getNoise(WorldInfo, double[], int, int)}.
     */
    @Deprecated(forRemoval = true)
    void getNoise(BiomeGenerator biomeGenerator, double[] buffer, int x, int z);


    /**
     * Gets the desired terrain settings. This method must return a new instance,
     * but with the same settings every time it is called. (If you want different
     * chunks to have different settings, use {@link WorldDecorator}.)
     *
     * @return The terrain settings.
     * @since 0.3
     */
    default TerrainSettings getTerrainSettings() {
        return new TerrainSettings();
    }
}
