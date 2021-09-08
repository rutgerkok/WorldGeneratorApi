package nl.rutgerkok.worldgeneratorapi.decoration;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.generator.LimitedRegion;

/**
 * Represents a square area of 32 x 32 blocks which should be populated. (So 2x2
 * chunks.) Objects like trees shouldn't place their trunk too close to the
 * edge, otherwise they risk that their leaves won't fit in the area. Therefore,
 * you should only place objects at most {@value #DECORATION_RADIUS} blocks from
 * the center.
 *
 * <p>
 * Sketch of the area that should be populated:
 *
 * <pre>
 * +----+----+
 * |    |    |
 * |  ##|##  |
 * +----+----+
 * |  ##|##  |
 * |    |    |
 * +----+----+
 * </pre>
 *
 * @since 0.2
 * @deprecated Use the new Bukkit class {@link LimitedRegion}.
 */
@Deprecated(forRemoval = true)
public interface DecorationArea {

    /**
     * The number of blocks from the center of this area where decorations can be
     * started. So for example no tree trunks should be created outside this area,
     * but tree leaves can still extend into the padding.
     *
     * @since 0.2
     */
    public static final int DECORATION_RADIUS = 8;

    /**
     * Gets the biome at the given position.
     *
     * @param x
     *            Block x in the world.
     * @param z
     *            Block z in the world.
     * @return The biome.
     * @since 0.2
     */
    Biome getBiome(int x, int z);

    /**
     * Gets the material at the given position.
     *
     * @param x
     *            Block x in the world.
     * @param y
     *            Block y in the world.
     * @param z
     *            Block z in the world.
     * @return The material.
     * @since 0.2
     */
    Material getBlock(int x, int y, int z);

    /**
     * Gets the full material at the given position.
     *
     * @param x
     *            Block x in the world.
     * @param y
     *            Block y in the world.
     * @param z
     *            Block z in the world.
     * @return The material.
     * @since 0.2
     */
    BlockData getBlockData(int x, int y, int z);

    /**
     * Gets the block state at the given position. This block state can then be cast
     * to a {@link Chest}, {@link Furnace}, etc. Use
     * {@link #setBlockState(int, int, int, BlockState)} after you have made your
     * changes, otherwise your changes will have absolutely no effect.
     *
     * <p>
     * Note that the decoration area is not yet part of the world. Therefore,
     * <strong>no location information is included</strong> in the returned block
     * state. If location information would be included, then various methods of
     * CraftBukkit would mistakenly attempt to access the live world, which would
     * fail: this decoration area is not yet part of the world, after all.
     *
     * <p>
     * As a result, {@link BlockState#isPlaced()} will return false.
     * {@link BlockState#getWorld()} will throw an {@link IllegalStateException} and
     * {@link BlockState#getX()}/Y/Z will all return 0. {@link BlockState#update()}
     * will also fail with an {@link IllegalStateException}. Instead, you must use a
     * {@link #setBlockState(int, int, int, BlockState)}.
     *
     * @param x
     *            Block x in the world.
     * @param y
     *            Block y in the world.
     * @param z
     *            Block z in the world.
     * @return The block state.
     * @since 0.3
     */
    BlockState getBlockState(int x, int y, int z);

    /**
     * Gets the center block x. This value minus 16 is the lowest valid x
     * coordinate, this value plus 15 the highest. Accessing blocks at invalid
     * coordinates will result in an exception being thrown.
     *
     * @return Center block x.
     * @since 0.2
     */
    int getCenterX();

    /**
     * Gets the center block z. This value minus 16 is the lowest valid z
     * coordinate, this value plus 15 the highest. Accessing blocks at invalid
     * coordinates will result in an exception being thrown.
     *
     * @return Center block z.
     * @since 0.2
     */
    int getCenterZ();

    /**
     * Gets the highest non-empty coordinate at the given coordinates.
     *
     * @param x
     *            The block x in the world.
     * @param z
     *            The block z in the world.
     * @return The block y.
     * @since 1.2
     */
    int getHighestBlockYAt(int x, int z);

    /**
     * Sets the material at the given position.
     *
     * @param x
     *            Block x in the world.
     * @param y
     *            Block y in the world.
     * @param z
     *            Block z in the world.
     * @param material
     *            The new material. May not be null, but may be
     *            {@link Material#AIR}.
     * @since 0.2
     */
    void setBlock(int x, int y, int z, Material material);

    /**
     * Sets the full material at the given position.
     *
     * @param x
     *            Block x in the world.
     * @param y
     *            Block y in the world.
     * @param z
     *            Block z in the world.
     * @param blockData
     *            The new material. May not be null, but may be the default block
     *            state of {@link Material#AIR}.
     * @since 0.2
     */
    void setBlockData(int x, int y, int z, BlockData blockData);

    /**
     * Sets the block state at the given position in the world. It is no problem if
     * the block state was retrieved from another position, or even another world.
     * First, the block at the position specified by the x, y and z parameters is
     * set to whatever {@link BlockState#getBlockData()} returns. Then, any NBT data
     * from the block state is copied to the chunk. Modifying the block state after
     * calling this method has no effect on the chunk.
     *
     * @param x
     *            Block x in the world.
     * @param y
     *            Block y in the world.
     * @param z
     *            Block z in the world.
     * @param blockState
     *            The block state.
     * @since 0.3
     */
    void setBlockState(int x, int y, int z, BlockState blockState);

    /**
     * Spawns an entity in the world.
     *
     * @param <T>
     *            Type of the entity.
     * @param entityClass
     *            Class of the entity, for example {@link Creeper}.class.
     * @param x
     *            Block x in the world.
     * @param y
     *            Block y in the world.
     * @param z
     *            Block z in the world.
     * @return The entity, so that you can modify it further.
     * @throws IllegalArgumentException
     *             If spawning entities of this entity type is not possible.
     * @since 1.2
     */
    <T extends Entity> T spawnEntity(Class<T> entityClass, double x, double y, double z)
            throws IllegalArgumentException;
}
