package nl.rutgerkok.worldgeneratorapi;

/**
 * Used to sample temperature, wetness, continentalness, etc.
 *
 * @since 2.0
 */
public interface ClimateSampler {

    /**
     * Represents the climate at a single point in the world. All values range
     * roughly from -1 to +1.
     *
     * @since 2.0
     */
    interface ClimatePoint {

        /**
         * Continentalness: how much inland are we?
         *
         * @return The number.
         * @since 2.0
         */
        float getContinentalness();

        /**
         * Depth. Function not yet known.
         *
         * @return The number.
         * @since 2.0
         */
        float getDepth();

        /**
         * Erosion: the higher the number, the flatter the terrain.
         *
         * @return The number.
         * @since 2.0
         */
        float getErosion();

        /**
         * Used to control the final shape of the terrain. Calculated from
         * {@link #getWeirdness()}, {@link #getErosion()}, {@link #getContinentalness()}
         * and the data packs (worldgen/noise_settings).
         *
         * @return The number.
         * @since 2.0
         */
        float getFinalDensity();

        /**
         * Humidity: how wet the biomes will be.
         *
         * @return The number.
         * @since 2.0
         */
        float getHumidity();

        /**
         * The initial density. Calculated from
         * {@link #getWeirdness()}, {@link #getErosion()}, {@link #getContinentalness()}
         * and the data packs (worldgen/noise_settings).
         *
         * @return The number.
         * @since 2.0
         */
        float getInitialDensity();

        /**
         * Ridges: used to generate jagged peaks. Calculated from
         * {@link #getWeirdness()}, {@link #getErosion()}, {@link #getContinentalness()}
         * and the data packs (worldgen/noise_settings).
         *
         * <p>
         * The "Peaks and Valleys" value on the debug screen is calculated from this
         * value as
         * {@code -(Math.abs(Math.abs(ridges) - 0.6666667F) - 0.33333334F) * 3F}.
         *
         * @return The number.
         * @since 2.0
         */
        float getRidges();

        /**
         * Temperature: how hot the biomes will be.
         *
         * @return The number.
         * @since 2.0
         */
        float getTemperature();

        /**
         * Weirdness: used to generate biome variations and mushroom islands.
         *
         * @return The number.
         * @since 2.0
         */
        float getWeirdness();

    }

    /**
     * Gets the climate at the given point in the world.
     * @param x Block x.
     * @param y Block y.
     * @param z Block z.
     * @return The climate.
     */
    ClimatePoint getClimatePoint(int x, int y, int z);
}
