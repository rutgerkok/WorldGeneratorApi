package nl.rutgerkok.worldgeneratorapi;

/**
 * Represents a basic terrain generator.
 *
 */
public interface BaseTerrainGenerator extends BaseChunkGenerator {

    enum HeightType {
        /**
         * Surface of the world. Water is included in the height.
         */
        WORLD_SURFACE,
        /**
         * Ignores water, so the height of the ocean floor is returned if there is
         * water.
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
     */
    int getHeight(int x, int z, HeightType type);

}
