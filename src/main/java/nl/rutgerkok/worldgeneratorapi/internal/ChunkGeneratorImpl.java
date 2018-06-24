package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator.GeneratingChunk;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldGeneratorApi;
import nl.rutgerkok.worldgeneratorapi.WorldRef;

final class ChunkGeneratorImpl extends ChunkGenerator {

    private final Function<World, BaseChunkGenerator> chunkGenerator;
    private final WorldGeneratorApi worldGeneratorApi;
    private @Nullable WorldGenerator worldGenerator;
    private final WorldRef world;

    ChunkGeneratorImpl(WorldGeneratorApi worldGeneratorApi, WorldRef world,
            Function<World, BaseChunkGenerator> baseChunkGenerator) {
        this.world = Objects.requireNonNull(world, "world");
        this.chunkGenerator = Objects.requireNonNull(baseChunkGenerator, "chunkGenerator");
        this.worldGeneratorApi = Objects.requireNonNull(worldGeneratorApi, "worldGeneratorApi");
    }

    @Override
    public ChunkData generateChunkData(@Nullable World world, @Nullable Random random, int chunkX, int chunkZ,
            @Nullable BiomeGrid biomes) {
        return generateChunkData0(Objects.requireNonNull(world), Objects.requireNonNull(random), chunkX, chunkZ,
                Objects.requireNonNull(biomes));
    }

    private ChunkData generateChunkData0(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomes) {
        ChunkData chunkData = this.createChunkData(world);
        WorldGenerator worldGenerator = getOrInitializeWorldGenerator(world);
        GeneratingChunk chunk = new GeneratingChunk() {

            @Override
            public BiomeGenerator getBiomeGenerator() {
                return worldGenerator.getBiomeGenerator();
            }

            @Override
            public BiomeGrid getBiomesForChunk() {
                return biomes;
            }

            @Override
            public ChunkData getBlocksForChunk() {
                return chunkData;
            }

            @Override
            public int getChunkX() {
                return chunkX;
            }

            @Override
            public int getChunkZ() {
                return chunkZ;
            }
        };
        worldGenerator.getBaseChunkGenerator().setBlocksInChunk(chunk);
        return chunkData;
    }

    private WorldGenerator getOrInitializeWorldGenerator(World world) {
        if (!this.world.matches(world)) {
            throw new RuntimeException("This world generator was created for world \"" + this.world.getName()
                    + "\", but used for \"" + world.getName() + "\"");
        }
        WorldGenerator worldGenerator = this.worldGenerator;
        if (worldGenerator != null) {
            return worldGenerator;
        }
        worldGenerator = worldGeneratorApi.getForWorld(world);
        worldGenerator.setBaseChunkGenerator(this.chunkGenerator.apply(world));
        this.worldGenerator = worldGenerator;
        return worldGenerator;
    }
}
