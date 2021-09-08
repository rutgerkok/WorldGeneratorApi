package nl.rutgerkok.worldgeneratorapi;

import java.util.Set;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.plugin.Plugin;

import com.google.common.collect.ImmutableSet;

/**
 * This class can tell you what biome ends up where in the world.
 *
 * @deprecated Replaced by new Bukkit API: {@link BiomeProvider} and
 *             {@link Plugin#getDefaultBiomeProvider(String, String)}. Note:
 *             {@link BiomeProvider} uses block coords.
 */
@Deprecated(forRemoval = true)
public interface BiomeGenerator {

    /**
     * The biomes where structures can spawn in, for the overworld, in vanilla
     * Minecraft.
     *
     * @since 0.5
     */
    Set<Biome> VANILLA_OVERWORLD_STRUCTURE_BIOMES = ImmutableSet.of(Biome.OCEAN, Biome.PLAINS, Biome.DESERT,
            Biome.MOUNTAINS, Biome.FOREST, Biome.TAIGA, Biome.SWAMP, Biome.RIVER, Biome.FROZEN_OCEAN,
            Biome.FROZEN_RIVER, Biome.SNOWY_TUNDRA, Biome.SNOWY_MOUNTAINS, Biome.MUSHROOM_FIELDS,
            Biome.MUSHROOM_FIELD_SHORE, Biome.BEACH, Biome.DESERT_HILLS, Biome.WOODED_HILLS, Biome.TAIGA_HILLS,
            Biome.MOUNTAIN_EDGE, Biome.JUNGLE, Biome.JUNGLE_HILLS, Biome.JUNGLE_EDGE, Biome.DEEP_OCEAN,
            Biome.STONE_SHORE, Biome.SNOWY_BEACH, Biome.BIRCH_FOREST, Biome.BIRCH_FOREST_HILLS, Biome.DARK_FOREST,
            Biome.SNOWY_TAIGA, Biome.SNOWY_TAIGA_HILLS, Biome.GIANT_TREE_TAIGA, Biome.GIANT_TREE_TAIGA_HILLS,
            Biome.WOODED_MOUNTAINS, Biome.SAVANNA, Biome.SAVANNA_PLATEAU, Biome.BADLANDS, Biome.WOODED_BADLANDS_PLATEAU,
            Biome.BADLANDS_PLATEAU, Biome.WARM_OCEAN, Biome.LUKEWARM_OCEAN, Biome.COLD_OCEAN, Biome.DEEP_WARM_OCEAN,
            Biome.DEEP_LUKEWARM_OCEAN, Biome.DEEP_COLD_OCEAN, Biome.DEEP_FROZEN_OCEAN, Biome.SUNFLOWER_PLAINS,
            Biome.DESERT_LAKES, Biome.GRAVELLY_MOUNTAINS, Biome.FLOWER_FOREST, Biome.TAIGA_MOUNTAINS, Biome.SWAMP_HILLS,
            Biome.ICE_SPIKES, Biome.MODIFIED_JUNGLE, Biome.MODIFIED_JUNGLE_EDGE, Biome.TALL_BIRCH_FOREST,
            Biome.TALL_BIRCH_HILLS, Biome.DARK_FOREST_HILLS, Biome.SNOWY_TAIGA_MOUNTAINS, Biome.GIANT_SPRUCE_TAIGA,
            Biome.GIANT_SPRUCE_TAIGA_HILLS, Biome.MODIFIED_GRAVELLY_MOUNTAINS, Biome.SHATTERED_SAVANNA,
            Biome.SHATTERED_SAVANNA_PLATEAU, Biome.ERODED_BADLANDS, Biome.MODIFIED_WOODED_BADLANDS_PLATEAU,
            Biome.MODIFIED_BADLANDS_PLATEAU);

    /**
     * Gets normal-scaled biomes for the given region.
     *
     * @param minX
     *            Block x.
     * @param minZ
     *            Block z.
     * @param xSize
     *            X size in blocks.
     * @param zSize
     *            Z size in blocks.
     * @deprecated Biome generators no longer generate things in batches, so this
     *             method is actually less efficient than just calling
     *             {@link #getZoomedOutBiome(int, int, int)}.
     * @return The biome array, ordered as
     *         {@code biome(x,z) = array[z * xSize + x)}.
     * @since 0.3
     */
    @Deprecated
    default Biome[] getBiomes(int minX, int minZ, int xSize, int zSize) {
        int zoomedOutXSize = xSize / 4;
        int zoomedOutZSize = zSize / 4;
        Biome[] zoomedOut = getZoomedOutBiomes(minX / 4, minZ / 4, zoomedOutXSize, zoomedOutZSize);
        Biome[] normalScale = new Biome[xSize * zSize];
        for (int i = 0; i < normalScale.length; i++) {
            int x = i % xSize;
            int z = i / xSize;
            normalScale[i] = zoomedOut[(z / 4) * zoomedOutZSize + x / 4];
        }
        return normalScale;
    }

    /**
     * Gets the biomes in which structures (villages, mineshafts, etc.) are allowed
     * to spawn. Note: structures can be quite large, and may therefore extend into
     * neighboring biomes.
     *
     * <p>
     * The biome generator always needs to return a set containing the same biomes -
     * you are not allowed to let the same biome generator return a different set of
     * biomes at a later time.
     *
     * @return Set of biomes.
     * @since 0.5
     */
    default ImmutableSet<Biome> getStructureBiomes() {
        return ImmutableSet.of();
    }

    /**
     * Gets a single biome at an unspecified height.
     *
     * @param x
     *            Scaled X position. Multiply this value with 4 to get the block x.
     * @param z
     *            Scaled Z position. Multiply this value with 4 to get the block z.
     * @return The biome.
     * @since 0.3
     */
    default Biome getZoomedOutBiome(int x, int z) {
        return getZoomedOutBiome(x, 16, z);
    }

    /**
     * Gets a single biome at the specified location.
     *
     * @param x
     *            Scaled X position. Multiply this value with 4 to get the block x.
     * @param y
     *            Scaled Y position. Multiply this value with 4 to get the block y.
     * @param z
     *            Scaled Z position. Multiply this value with 4 to get the block z.
     * @return The bione.
     * @since 0.5
     */
    Biome getZoomedOutBiome(int x, int y, int z);

    /**
     * Gets zoomed-out biomes for the given region.
     *
     * @param minX
     *            Min x = blockX / 4
     * @param minZ
     *            Min z = blockZ / 4
     * @param xSize
     *            X size in units of 4 blocks. (xSize == 2 ==> blockXSize == 8)
     * @param zSize
     *            Z size in units of 4 blocks.
     * @deprecated Biome generators no longer generate things in batches, so this
     *             method is actually less efficient than just calling
     *             {@link #getZoomedOutBiome(int, int, int)}.
     * @return The biome array, ordered as
     *         {@code biome(x,z) = array[z * xSize + x)}.
     * @since 0.1
     */
    @Deprecated
    default Biome[] getZoomedOutBiomes(int minX, int minZ, int xSize, int zSize) {
        int arrayLength = xSize * zSize;
        Biome[] zoomedOut = new Biome[arrayLength];
        for (int i = 0; i < zoomedOut.length; i++) {
            int x = i % xSize;
            int z = i / xSize;
            zoomedOut[i] = this.getZoomedOutBiome(minX + x, minZ + z);
        }
        return zoomedOut;
    }

}
