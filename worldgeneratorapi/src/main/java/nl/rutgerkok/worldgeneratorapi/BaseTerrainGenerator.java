package nl.rutgerkok.worldgeneratorapi;

/**
 * Represents a basic terrain generator. This is used to generate the raw
 * terrain shape and to probe the height of terrain that is not yet generated.
 *
 * @since 0.3
 */
public interface BaseTerrainGenerator extends BaseChunkGenerator {

    /**
     * Different ways of measuring the height of the world at a particular x/z
     * location.
     *
     * @since 0.3
     */
    enum HeightType {
        /**
         * Surface of the world. Water is included in the height.
         *
         * @since 0.3
         */
        WORLD_SURFACE,
        /**
         * Ignores water, so the height of the ocean floor is returned if there is
         * water.
         *
         * @since 0.3
         */
        OCEAN_FLOOR
    }

    /**
     * Predicts the height for the given x and z coordinate, which can be anywhere
     * in the world. For a superflat world, this is just a constant value, but for
     * other world types the calculation will be more complex.
     *
     * @param x
     *            X in the world.
     * @param z
     *            Y in the world.
     * @param type
     *            The type of heightmap.
     * @return The height, for example 63.
     * @since 0.3
     */
    int getHeight(int x, int z, HeightType type);

}
