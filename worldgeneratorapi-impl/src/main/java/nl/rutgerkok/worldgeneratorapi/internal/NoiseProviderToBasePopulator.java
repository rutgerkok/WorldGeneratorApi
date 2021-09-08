package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.craftbukkit.v1_17_R1.CraftHeightMap;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_17_R1.generator.CraftChunkData;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.generator.WorldInfo;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult.PartialResult;

import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseModifier;
import net.minecraft.world.level.levelgen.NoiseSampler;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import nl.rutgerkok.worldgeneratorapi.BaseNoiseProvider;
import nl.rutgerkok.worldgeneratorapi.BaseNoiseProvider.TerrainConfig;
import nl.rutgerkok.worldgeneratorapi.BasePopulator;

final class NoiseProviderToBasePopulator implements BasePopulator {

    private static class Inner implements BasePopulator {
        private final NoiseBasedChunkGenerator internal;
        private final StructureFeatureManager structureFeatureManager;

        private Inner(BaseNoiseProvider baseNoiseGenerator, ServerLevel world) {
            BiomeSource biomeSource = world.getChunkProvider().getGenerator().getBiomeSource();
            long seed = world.getSeed();

            this.structureFeatureManager = world.structureFeatureManager();

            NoiseGeneratorSettings noiseGeneratorSettings = createNoiseSettings(baseNoiseGenerator
                    .getTerrainSettings());
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

            NoiseSampler noiseSampler = new OurNoiseSampler(baseNoiseGenerator, world.getWorld(), biomeSource,
                    cellWidth, cellHeight, cellCountY, noiseSettings, var8, var10, var9, NoiseModifier.PASSTHROUGH);
            try {
                ReflectionUtil.getFieldOfType(this.internal, NoiseSampler.class)
                        .set(this.internal, noiseSampler);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to inject noise generator", e);
            }
        }

        @Override
        public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
            CraftChunkData impl = (CraftChunkData) chunkData;
            Executor directly = Runnable::run;
            internal.fillFromNoise(directly, this.structureFeatureManager, impl.getHandle());
        }

        @Override
        public int getBaseHeight(WorldInfo world, Random random, int blockX, int blockZ, HeightMap heightMap) {
            int maxHeight = world.getMaxHeight();
            int minHeight = world.getMinHeight();
            LevelHeightAccessor levelHeight = new LevelHeightAccessor() {

                @Override
                public int getHeight() {
                    return maxHeight - minHeight;
                }

                @Override
                public int getMinBuildHeight() {
                    return minHeight;
                }
            };
            return this.internal.getBaseHeight(blockX, blockZ, CraftHeightMap.toNMS(heightMap), levelHeight);
        }
    }

    private static class OurNoiseSampler extends NoiseSampler {

        private final BaseNoiseProvider baseNoiseProvider;
        private final WorldInfo worldInfo;

        public OurNoiseSampler(BaseNoiseProvider noiseProvider, WorldInfo worldInfo, BiomeSource biomeSource,
                int cellWidth, int cellHeight, int cellCountY,
                NoiseSettings noisesettings, BlendedNoise blendednoise, @Nullable SimplexNoise noisegenerator3handler,
                PerlinNoise noisegeneratoroctaves, NoiseModifier noisemodifier) {
            super(biomeSource, cellWidth, cellHeight, cellCountY, noisesettings, blendednoise, noisegenerator3handler,
                    noisegeneratoroctaves, noisemodifier);

            this.worldInfo = Objects.requireNonNull(worldInfo, "worldInfo");
            this.baseNoiseProvider = Objects.requireNonNull(noiseProvider, "noiseProvider");
        }

        @Override
        public void fillNoiseColumn(double[] buffer, int i, int j, NoiseSettings noisesettings, int k, int l, int i1) {
            baseNoiseProvider.getNoise(worldInfo, buffer, i, j);

            // Noise caves should be added back here
            // We should probably do something with params k (cell y?), l and i1
        }
    }

    private static NoiseGeneratorSettings createNoiseSettings(TerrainConfig settings) {
        // Practically everything in that class is private, final or protected, so we
        // need to modify the serialized data :(

        // Change default settings into NBT tag
        Either<Tag, PartialResult<Tag>> result = NoiseGeneratorSettings.DIRECT_CODEC
                .encode(NoiseGeneratorSettings.bootstrap(), NbtOps.INSTANCE, new CompoundTag())
                .get();
        Tag resultTag = result.left()
                .orElseThrow(() -> new RuntimeException("Failed to generate default NoiseGeneratorSettings: "
                        + result.right().map(PartialResult::message).orElse(null)));

        // Modify that tag
        CompoundTag tag = (CompoundTag) resultTag;
        if (settings.stoneBlock != null) {
            tag.put("default_block", NbtUtils.writeBlockState(((CraftBlockData) settings.stoneBlock).getState()));
        }
        if (settings.waterBlock != null) {
            tag.put("default_fluid", NbtUtils.writeBlockState(((CraftBlockData) settings.waterBlock).getState()));
        }
        if (settings.seaLevel != -1) {
            tag.putInt("sea_level", settings.seaLevel);
        }

        // And encode again
        return NoiseGeneratorSettings.DIRECT_CODEC.decode(NbtOps.INSTANCE, tag).result().orElseThrow()
                .getFirst();
    }

    private final BaseNoiseProvider baseNoiseGenerator;
    private Inner theGenerator;

    public NoiseProviderToBasePopulator(BaseNoiseProvider baseNoiseProvider) {
        this.baseNoiseGenerator = Objects.requireNonNull(baseNoiseProvider, "baseNoiseProvider");
    }

    @Override
    public void generateNoise(WorldInfo world, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        inner(world).generateNoise(world, random, chunkX, chunkZ, chunkData);
    }

    @Override
    public int getBaseHeight(WorldInfo world, Random random, int blockX, int blockZ, HeightMap heightMap) {
        return inner(world).getBaseHeight(world, random, blockX, blockZ, heightMap);
    }

    private Inner inner(WorldInfo world) {
        Inner inner = this.theGenerator;
        if (inner != null) {
            // Already initialized
            return inner;
        }

        // Need to fetch server level first (hopefully this isn't too thread unsafe)
        ServerLevel serverLevel;
        if (world instanceof CraftWorld craftWorld) {
            serverLevel = craftWorld.getHandle();
        } else {
            serverLevel = ((CraftWorld) Bukkit.getWorld(world.getUID())).getHandle();
        }

        inner = new Inner(baseNoiseGenerator, serverLevel);
        this.theGenerator = inner;
        return inner;
    }


}
