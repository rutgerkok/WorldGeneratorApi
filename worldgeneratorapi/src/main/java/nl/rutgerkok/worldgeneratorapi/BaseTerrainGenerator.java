package nl.rutgerkok.worldgeneratorapi;

import org.bukkit.generator.ChunkGenerator;

/**
 * Represents a basic terrain generator. This is used to generate the raw
 * terrain shape and to probe the height of terrain that is not yet generated.
 *
 * <p>
 * Note: you need to override the
 * {@link #getHeight(BiomeGenerator, int, int, HeightType)} method. In the
 * future, this will become an abstract method, but for old code we also provide
 * the possibility of overriding {@link #getHeight(int, int, HeightType)}
 * instead.
 *
 * @since 0.3
 * @deprecated Replaced by the new methods in {@link ChunkGenerator}.
 */
@Deprecated(forRemoval = true)
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
     * @param biomeGenerator
     *            The biome generator that is currently in use. Keep in mind that
     *            multiple plugins may change things in the world generator. That
     *            means that even if your plugin has provided a biome generator, it
     *            may have been replaced by another one by some other plugin. So
     *            please use the biome generator provided here.
     * @param x
     *            Block X in the world.
     * @param z
     *            Block Z in the world.
     * @param type
     *            The type of height map.
     * @return The height, for example 63.
     * @since 0.6
     */
    default int getHeight(BiomeGenerator biomeGenerator, int x, int z, HeightType type) {
        // Fall back to old method
        return getHeight(x, z, type);
    }

    /**
     * Predicts the height for the given x and z coordinate, which can be anywhere
     * in the world. For a superflat world, this is just a constant value, but for
     * other world types the calculation will be more complex.
     *
     * @param x
     *            Block X in the world.
     * @param z
     *            Block Z in the world.
     * @param type
     *            The type of height map.
     * @return The height, for example 63.
     * @since 0.3
     * @deprecated Use {@link #getHeight(BiomeGenerator, int, int, HeightType)},
     *             which includes a reference to the biome generator that is
     *             currently in use.
     */
    @Deprecated
    default int getHeight(int x, int z, HeightType type) {
        throw new RuntimeException("Deprecated - use getHeight(BiomeGenerator, int, int, HeightType) instead.");
    }

}
