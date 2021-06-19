package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import org.bukkit.craftbukkit.v1_17_R1.block.data.CraftBlockData;

import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseModifier;
import net.minecraft.world.level.levelgen.NoiseSampler;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import nl.rutgerkok.worldgeneratorapi.BaseNoiseGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseNoiseGenerator.TerrainSettings;
import nl.rutgerkok.worldgeneratorapi.BaseTerrainGenerator;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.internal.InjectedBiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.internal.ReflectionUtil;

public final class NoiseToTerrainGenerator implements BaseTerrainGenerator {

    private class OurNoiseSampler extends NoiseSampler {

        public OurNoiseSampler(BiomeSource biomeSource, int cellWidth, int cellHeight, int cellCountY,
                NoiseSettings noisesettings,
                BlendedNoise blendednoise, @Nullable SimplexNoise noisegenerator3handler,
                PerlinNoise noisegeneratoroctaves, NoiseModifier noisemodifier) {
            super(biomeSource, cellWidth, cellHeight, cellCountY, noisesettings, blendednoise, noisegenerator3handler,
                    noisegeneratoroctaves, noisemodifier);
        }

        @Override
        public void fillNoiseColumn(double[] buffer, int i, int j, NoiseSettings noisesettings, int k, int l, int i1) {
            baseNoiseGenerator.getNoise(currentBiomeGenerator.get(), buffer, i, j);

            // Noise caves should be added back here
            // We should probably do something with params k (cell y?), l and i1
        }
    }

    private static NoiseGeneratorSettings createNoiseSettings(TerrainSettings settings) {
        // Practically everything in that class is private, final or protected, so we
        // need to modify the serialized data :(

        // Change default settings into NBT tag
        Tag result = NoiseGeneratorSettings.DIRECT_CODEC
                .encode(NoiseGeneratorSettings.bootstrap(), NbtOps.INSTANCE, new CompoundTag()).result().orElseThrow();

        // Modify that tag
        CompoundTag serialized = (CompoundTag) result;
        serialized.put("default_block", NbtUtils.writeBlockState(((CraftBlockData) settings.stoneBlock).getState()));
        serialized.put("default_fluid", NbtUtils.writeBlockState(((CraftBlockData) settings.waterBlock).getState()));
        if (settings.seaLevel != -1) {
            serialized.putInt("sea_level", settings.seaLevel);
        }

        // And encode again
        return NoiseGeneratorSettings.DIRECT_CODEC.decode(NbtOps.INSTANCE, serialized).result().orElseThrow()
                .getFirst();
    }

    private final NoiseBasedChunkGenerator internal;
    private final BaseNoiseGenerator baseNoiseGenerator;
    private final ServerLevel world;

    private final Supplier<BiomeGenerator> currentBiomeGenerator;

    public NoiseToTerrainGenerator(ServerLevel world, StructureManager structureManager,
            Supplier<BiomeGenerator> biomeGenerator, BaseNoiseGenerator baseNoiseGenerator, long seed) {
        this.world = Objects.requireNonNull(world, "world");
        this.baseNoiseGenerator = Objects.requireNonNull(baseNoiseGenerator, "baseNoiseGenerator");
        this.currentBiomeGenerator = Objects.requireNonNull(biomeGenerator, "biomeGenerator");

        BiomeSource biomeSource = InjectedBiomeGenerator
                .wrapOrUnwrap(world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), biomeGenerator.get());

        NoiseGeneratorSettings noiseGeneratorSettings = createNoiseSettings(baseNoiseGenerator.getTerrainSettings());
        NoiseSettings noiseSettings = noiseGeneratorSettings.noiseSettings();
        this.internal = new NoiseBasedChunkGenerator(biomeSource, seed, () -> noiseGeneratorSettings);

        // Faithfully reconstruct noise generator - but with our noise in it
        int cellWidth = QuartPos.toBlock(noiseSettings.noiseSizeHorizontal());
        int cellHeight = QuartPos.toBlock(noiseSettings.noiseSizeVertical());
        int cellCountY = noiseSettings.height() / cellHeight;

        WorldgenRandom var7 = new WorldgenRandom(seed);
        BlendedNoise var8 = new BlendedNoise(var7);

        // Keep to not alter random number stream
        if (noiseSettings.useSimplexSurfaceNoise()) {
            new PerlinSimplexNoise(var7, IntStream.rangeClosed(-3, 0));
        } else {
            new PerlinNoise(var7, IntStream.rangeClosed(-3, 0));
        }

        var7.consumeCount(2620);
        PerlinNoise var9 = new PerlinNoise(var7, IntStream.rangeClosed(-15, 0));
        SimplexNoise var10;
        if (noiseSettings.islandNoiseOverride()) {
            WorldgenRandom var11 = new WorldgenRandom(seed);
            var11.consumeCount(17292);
            var10 = new SimplexNoise(var11);
        } else {
            var10 = null;
        }

        // Here we would implement noise caves, by creating a different NoiseModifer

        NoiseSampler noiseSampler = new OurNoiseSampler(biomeSource, cellWidth, cellHeight, cellCountY, noiseSettings,
                var8,
                var10, var9, NoiseModifier.PASSTHROUGH);
        try {
            ReflectionUtil.getFieldOfType(this.internal, NoiseSampler.class)
                    .set(this.internal, noiseSampler);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to inject noise generator", e);
        }
    }

    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, StructureFeatureManager structureManager,
            ChunkAccess chunkAccess) {
        return internal.fillFromNoise(executor, structureManager, chunkAccess);
    }

    public NoiseColumn getBaseColumn(int blockX, int blockZ, LevelHeightAccessor levelHeight) {
        return internal.getBaseColumn(blockX, blockZ, levelHeight);
    }

    public int getBaseHeight(int var0, int var1, Heightmap.Types var2, LevelHeightAccessor var3) {
        return this.internal.getBaseHeight(var0, var1, var2, var3);
    }

    @Override
    public void setBlocksInChunk(GeneratingChunk chunk) {
        Executor directly = Runnable::run;
        ChunkDataImpl impl = (ChunkDataImpl) chunk.getBlocksForChunk();

        internal.fillFromNoise(directly, world.structureFeatureManager(), impl.getHandle());
    }

}
