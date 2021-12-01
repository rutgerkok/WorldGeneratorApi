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
         * Erosion: the higher the number, the flatter the terrain.
         *
         * @return The number.
         * @since 2.0
         */
        float getErosion();

        /**
         * Factor: used to make the terrain more hilly. Calculated from
         * {@link #getWeirdness()}, {@link #getErosion()}, {@link #getContinentalness()}
         * and the data packs (worldgen/noise_settings).
         *
         * @return The number.
         * @since 2.0
         */
        float getFactor();

        /**
         * Humidity: how wet the biomes will be.
         *
         * @return The number.
         * @since 2.0
         */
        float getHumidity();

        /**
         * Jaggedness: used to generate jagged peaks. Calculated from
         * {@link #getWeirdness()}, {@link #getErosion()}, {@link #getContinentalness()}
         * and the data packs (worldgen/noise_settings).
         *
         * @return The number.
         * @since 2.0
         */
        float getJaggedness();

        /**
         * Offset: the base height of the terrain.Calculated from
         * {@link #getWeirdness()}, {@link #getErosion()}, {@link #getContinentalness()}
         * and the data packs (worldgen/noise_settings).
         *
         * @return The number.
         * @since 2.0
         */
        float getOffset();

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
