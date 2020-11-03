package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.util.Objects;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;

import net.minecraft.server.v1_16_R3.ChunkGenerator;
import net.minecraft.server.v1_16_R3.ChunkGeneratorAbstract;
import net.minecraft.server.v1_16_R3.ChunkProviderDebug;
import net.minecraft.server.v1_16_R3.ChunkProviderFlat;
import net.minecraft.server.v1_16_R3.GeneratorAccess;
import net.minecraft.server.v1_16_R3.HeightMap;
import net.minecraft.server.v1_16_R3.SeededRandom;
import net.minecraft.server.v1_16_R3.StructureManager;
import net.minecraft.server.v1_16_R3.WorldChunkManager;
import net.minecraft.server.v1_16_R3.WorldServer;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseTerrainGenerator;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.ChunkDataImpl;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.InjectedChunkGenerator;

/**
 * Wraps a Minecraft base terrain generator into something that
 * WorldGeneratorApi understands.
 *
 */
public final class BaseTerrainGeneratorImpl implements BaseTerrainGenerator {

    public static HeightMap.Type fromApi(HeightType heightType) {
        switch (heightType) {
            case OCEAN_FLOOR:
                return HeightMap.Type.OCEAN_FLOOR_WG;
            case WORLD_SURFACE:
                return HeightMap.Type.WORLD_SURFACE_WG;
            default:
                throw new UnsupportedOperationException("Unknown HeightType: " + heightType);
        }
    }

    /**
     * Extracts the base chunk generator from a Minecraft world using the currently
     * in use chunk generator. If the chunk generator is provided by us, we can
     * return the original {@link BaseChunkGenerator}. If the chunk generator is
     * provided by Minecraft's {@link ChunkGeneratorAbstract}, we can wrap
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
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        ChunkGenerator chunkGenerator = worldServer.getChunkProvider().getChunkGenerator();
        StructureManager structureManager = worldServer.getStructureManager();
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
        return chunkGenerator instanceof ChunkProviderDebug || chunkGenerator instanceof ChunkProviderFlat
                || chunkGenerator instanceof ChunkGeneratorAbstract;
    }

    private final ChunkGenerator internal;
    private final GeneratorAccess world;
    private final StructureManager structureManager;

    private BaseTerrainGeneratorImpl(GeneratorAccess world, ChunkGenerator chunkGenerator,
            StructureManager structureManager) {
        if (chunkGenerator instanceof InjectedChunkGenerator) {
            throw new IllegalArgumentException("Double-wrapping");
        }
        this.world = Objects.requireNonNull(world, "world");
        this.internal = Objects.requireNonNull(chunkGenerator, "internal");
        this.structureManager = Objects.requireNonNull(structureManager, "structureManager");
    }

    @Override
    public int getHeight(int x, int z, HeightType type) {
        return internal.getBaseHeight(x, z, fromApi(type));
    }

    /**
     * Replaces the "world chunk manager" (== biome generator) that's used in the
     * class.
     *
     * @param biomeGenerator
     *            The new biome generator.
     */
    public void replaceWorldChunkManager(WorldChunkManager biomeGenerator) {
        Field field = ReflectionUtil.getFieldOfType(internal, WorldChunkManager.class);
        try {
            field.set(internal, biomeGenerator);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to replace biome generator in Minecraft", e);
        }
    }

    @Override
    public void setBlocksInChunk(GeneratingChunk chunk) {
        ChunkDataImpl blocks = (ChunkDataImpl) chunk.getBlocksForChunk();
        SeededRandom random = new SeededRandom();
        random.a(chunk.getChunkX(), chunk.getChunkZ());

        // Make sure this matches isSupported above
        if (internal instanceof ChunkGeneratorAbstract) {
            ((ChunkGeneratorAbstract) internal).buildNoise(world, structureManager, blocks.getHandle());
        } else if (internal instanceof ChunkProviderFlat) {
            ((ChunkProviderFlat) internal).buildNoise(world, structureManager, blocks.getHandle());
        } else if (internal instanceof ChunkProviderDebug) {
            // Generate nothing - there is no base terrain
        } else {
            throw new UnsupportedOperationException("Didn't recognize " + internal.getClass());
        }
    }

}
