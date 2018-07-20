package nl.rutgerkok.worldgeneratorapi.decoration;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;

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
 */
public interface DecorationArea {

    /**
     * The number of blocks from the center of this area where decorations can be
     * started. So for example no tree trunks should be created outside this area,
     * but tree leaves can still extend into the padding.
     */
    public static final int DECORATION_RADIUS = 8;

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
     */
    BlockData getBlockData(int x, int y, int z);

    /**
     * Gets the center block x. This value minus 16 is the lowest valid x
     * coordinate, this value plus 15 the highest. Accessing blocks at invalid
     * coordinates will result in an exception being thrown.
     *
     * @return Center block x.
     */
    int getCenterX();

    /**
     * Gets the center block z. This value minus 16 is the lowest valid z
     * coordinate, this value plus 15 the highest. Accessing blocks at invalid
     * coordinates will result in an exception being thrown.
     *
     * @return Center block z.
     */
    int getCenterZ();

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
     */
    void setBlockData(int x, int y, int z, BlockData blockData);
}
