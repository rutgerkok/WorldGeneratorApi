package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.Executor;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseTerrainGenerator;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.ChunkDataImpl;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.InjectedChunkGenerator;

/**
 * Wraps a Minecraft base terrain generator into something that
 * WorldGeneratorApi understands.
 *
 */
@Deprecated
public final class BaseTerrainGeneratorImpl implements BaseTerrainGenerator {

    public static Heightmap.Types fromApi(HeightType heightType) {
        switch (heightType) {
            case OCEAN_FLOOR:
                return Heightmap.Types.OCEAN_FLOOR_WG;
            case WORLD_SURFACE:
                return Heightmap.Types.WORLD_SURFACE_WG;
            default:
                throw new UnsupportedOperationException("Unknown HeightType: " + heightType);
        }
    }

    /**
     * Extracts the base chunk generator from a Minecraft world using the currently
     * in use chunk generator. If the chunk generator is provided by us, we can
     * return the original {@link BaseChunkGenerator}. If the chunk generator is
     * provided by Minecraft's {@link NoiseBasedChunkGenerator}, we can wrap
     * Minecraft's base chunk generator. Otherwise, we throw an exception.
     *
     * @param world
     *            The world.
     * @return The base chunk generator.
     * @throws UnsupportedOperationException
     *             If the world has a world generator not provided by us or
     *             Minecraft (so it's a custom one).
     */
    static BaseTerrainGenerator fromMinecraft(World world) {
        ServerLevel worldServer = ((CraftWorld) world).getHandle();
        ChunkGenerator chunkGenerator = worldServer.getChunkProvider().getGenerator();
        StructureFeatureManager structureManager = worldServer.structureFeatureManager();
        if (chunkGenerator instanceof InjectedChunkGenerator) {
            return ((InjectedChunkGenerator) chunkGenerator).getBaseTerrainGenerator();
        }
        if (isSupported(chunkGenerator)) {
            return new BaseTerrainGeneratorImpl(worldServer, chunkGenerator, structureManager);
        }

        throw new UnsupportedOperationException(
                "Cannot extract base chunk generator from " + chunkGenerator.getClass()
                        + ". \nThe usual cause is that WorldGeneratorApi.createCustomGenerator is called without"
                        + " supplying a BaseTerrainGenerator. This BaseTerrainGenerator needs to be set before"
                        + " other things are set, such as the biome generator. This is because of technical reasons."
                        + " \nIf you do not intend to set a BaseTerrainGenerator, don't use createCustomGenerator,"
                        + " but listen to the WorldGeneratorInitEvent instead.");
    }

    private static boolean isSupported(ChunkGenerator chunkGenerator) {
        // Make sure this matches setBlocksInChunk below
        return chunkGenerator instanceof DebugLevelSource || chunkGenerator instanceof FlatLevelSource
                || chunkGenerator instanceof NoiseBasedChunkGenerator;
    }

    private final ChunkGenerator internal;
    private final LevelAccessor world;
    private final StructureFeatureManager structureManager;

    private BaseTerrainGeneratorImpl(LevelAccessor world, ChunkGenerator chunkGenerator,
            StructureFeatureManager structureManager) {
        if (chunkGenerator instanceof InjectedChunkGenerator) {
            throw new IllegalArgumentException("Double-wrapping");
        }
        this.world = Objects.requireNonNull(world, "world");
        this.internal = Objects.requireNonNull(chunkGenerator, "internal");
        this.structureManager = Objects.requireNonNull(structureManager, "structureManager");
    }

    @Override
    public int getHeight(int x, int z, HeightType type) {
        return internal.getBaseHeight(x, z, fromApi(type), world);
    }

    /**
     * Replaces the "world chunk manager" (== biome generator) that's used in the
     * class.
     *
     * @param biomeGenerator
     *            The new biome generator.
     */
    public void replaceWorldChunkManager(BiomeSource biomeGenerator) {
        Field field = ReflectionUtil.getFieldOfType(internal, BiomeSource.class);
        try {
            field.set(internal, biomeGenerator);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to replace biome generator in Minecraft", e);
        }
    }

    @Override
    public void setBlocksInChunk(GeneratingChunk chunk) {
        ChunkDataImpl blocks = (ChunkDataImpl) chunk.getBlocksForChunk();
        WorldgenRandom random = new WorldgenRandom();
        random.setBaseChunkSeed(chunk.getChunkX(), chunk.getChunkZ());
        Executor directly = Runnable::run;
        internal.fillFromNoise(directly, structureManager, blocks.getHandle());
    }

}
