package nl.rutgerkok.worldgeneratorapi;

import javax.annotation.Nullable;

import org.bukkit.block.data.BlockData;

import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;

public interface BaseNoiseGenerator {

    public static final class TerrainSettings {
        /**
         * The block used as stone. Set to null to use the server default.
         */
        @Nullable
        public BlockData stoneBlock = null;

        /**
         * The block used as water. Set to null to use the server default.
         */
        @Nullable
        public BlockData waterBlock = null;

        /**
         * The water level. Set to a negative value to use the server default.
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
     *            Every entry represents 4x4x4 blocks. Entry zero is at bedrock
     *            level and from there it goes upwards. The array has a length of
     *            33.
     * @param x
     *            Column x = blockX / 4
     * @param z
     *            Column z = blockZ / 4
     */
    void getNoise(BiomeGenerator biomeGenerator, double[] buffer, int x, int z);

    /**
     * Gets the desired terrain settings. This method must return a new instance,
     * but with the same settings every time it is called. (If you want different
     * chunks to have different settings, use {@link WorldDecorator}.)
     *
     * @return The terrain settings.
     */
    default TerrainSettings getTerrainSettings() {
        return new TerrainSettings();
    }
}
