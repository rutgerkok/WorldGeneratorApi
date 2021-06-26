package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import com.mojang.serialization.Codec;

import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.levelgen.synth.SurfaceNoise;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator.GeneratingChunk;
import nl.rutgerkok.worldgeneratorapi.BaseTerrainGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseTerrainGenerator.HeightType;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.decoration.BaseDecorationType;
import nl.rutgerkok.worldgeneratorapi.internal.BaseTerrainGeneratorImpl;
import nl.rutgerkok.worldgeneratorapi.internal.BiomeGeneratorImpl;
import nl.rutgerkok.worldgeneratorapi.internal.InjectedBiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.internal.ReflectionUtil;
import nl.rutgerkok.worldgeneratorapi.internal.WorldDecoratorImpl;

/**
 * Standard Minecraft chunk generator (see {@link NoiseBasedChunkGenerator}) -
 * but with support to change some settings.
 *
 */
public final class InjectedChunkGenerator extends ChunkGenerator {
    public static class GeneratingChunkImpl implements GeneratingChunk {

        private final int chunkX;
        private final int chunkZ;
        private final ChunkDataImpl blocks;
        private final BiomeGenerator biomeGenerator;
        private final BiomeGridImpl biomeGrid;
        public final ChunkAccess internal;

        GeneratingChunkImpl(ChunkAccess internal, BiomeGenerator biomeGenerator) {
            this.internal = Objects.requireNonNull(internal, "internal");
            this.chunkX = internal.getPos().x;
            this.chunkZ = internal.getPos().z;
            this.blocks = new ChunkDataImpl(internal);
            this.biomeGrid = new BiomeGridImpl(internal.getBiomes());

            this.biomeGenerator = Objects.requireNonNull(biomeGenerator, "biomeManager");
        }

        @Override
        public BiomeGenerator getBiomeGenerator() {
            return biomeGenerator;
        }

        @Override
        public BiomeGrid getBiomesForChunk() {
            return biomeGrid;
        }

        @Override
        public ChunkData getBlocksForChunk() {
            return blocks;
        }

        @Override
        public int getChunkX() {
            return chunkX;
        }

        @Override
        public int getChunkZ() {
            return chunkZ;
        }

    }

    private static final BlockState k;
    static {
        k = Blocks.AIR.defaultBlockState();
    }

    private final SurfaceNoise surfaceNoise;
    protected final BlockState defaultBlock;
    protected final BlockState defaultFluid;
    protected final NoiseGeneratorSettings settings;
    private final int height;
    public final WorldDecoratorImpl worldDecorator = new WorldDecoratorImpl();
    private BaseTerrainGenerator baseTerrainGenerator;
    private BiomeGenerator biomeGenerator;

    /**
     * Original biome generator, ready to be restored when the plugin unloads.
     */
    private final BiomeGeneratorImpl originalBiomeGenerator;
    private final Registry<Biome> biomeRegistry;

    public InjectedChunkGenerator(BiomeSource worldchunkmanager, Registry<Biome> biomeRegistry,
            BaseTerrainGenerator baseChunkGenerator, long seed, NoiseGeneratorSettings settings) {
        super(worldchunkmanager, worldchunkmanager, settings.structureSettings(), seed);

        this.settings = Objects.requireNonNull(settings);
        NoiseSettings noisesettings = settings.noiseSettings();
        this.height = noisesettings.height();
        this.defaultBlock = settings.getDefaultBlock();
        this.defaultFluid = settings.getDefaultFluid();
        WorldgenRandom var7 = new WorldgenRandom(seed);
        @SuppressWarnings("unused") // To keep the seeds working
        BlendedNoise var8 = new BlendedNoise(var7);
        this.surfaceNoise = noisesettings.useSimplexSurfaceNoise()
                ? new PerlinSimplexNoise(var7, IntStream.rangeClosed(-3, 0))
                : new PerlinNoise(var7, IntStream.rangeClosed(-3, 0));


        this.biomeRegistry = Objects.requireNonNull(biomeRegistry, "biomeRegistry");
        this.originalBiomeGenerator = new BiomeGeneratorImpl(biomeRegistry, this.biomeSource);
        this.biomeGenerator = this.originalBiomeGenerator;

        setBaseChunkGenerator(baseChunkGenerator);
    }

    protected BlockState a(double d0, int i) {
        BlockState iblockdata;
        if (d0 > 0.0D) {
            iblockdata = this.defaultBlock;
        } else if (i < this.getSeaLevel()) {
            iblockdata = this.defaultFluid;
        } else {
            iblockdata = k;
        }

        return iblockdata;
    }

    @Override
    public void applyBiomeDecoration(final WorldGenRegion regionlimitedworldaccess,
            final StructureFeatureManager structuremanager) {
        final ChunkPos chunkcoordintpair = regionlimitedworldaccess.getCenter();
        final int i = chunkcoordintpair.getMinBlockX();
        final int j = chunkcoordintpair.getMinBlockZ();
        final BlockPos blockposition = new BlockPos(i, regionlimitedworldaccess.getMinBuildHeight(), j);
        final Biome biomebase = this.biomeSource.getPrimaryBiome(chunkcoordintpair);
        final WorldgenRandom seededrandom = new WorldgenRandom();
        final long k = seededrandom.setDecorationSeed(regionlimitedworldaccess.getSeed(), i, j);
        try {
            worldDecorator
                    .generate(biomebase, structuremanager, this, regionlimitedworldaccess, k, seededrandom, blockposition);
        } catch (Exception exception) {
            final CrashReport crashreport = CrashReport.forThrowable(exception, "Biome decoration");
            crashreport.addCategory("Generation").setDetail("CenterX", chunkcoordintpair.x)
                    .setDetail("CenterZ", chunkcoordintpair.z).setDetail("Seed", k)
                    .setDetail("Biome", biomebase);
            throw new ReportedException(crashreport);
        }
    }

    @Override
    public void applyCarvers(long i, BiomeManager biomemanager, ChunkAccess ichunkaccess,
            GenerationStep.Carving worldgenstage_features) {
        BaseDecorationType baseDecorationType = worldDecorator.toBaseDecorationType(worldgenstage_features);
        if (this.worldDecorator.isDefaultEnabled(baseDecorationType)) {
            super.applyCarvers(i, biomemanager, ichunkaccess, worldgenstage_features);
        }
        this.worldDecorator
                .spawnCustomBaseDecorations(baseDecorationType, new GeneratingChunkImpl(ichunkaccess, biomeGenerator));
    }

    @Override
    public void buildSurfaceAndBedrock(WorldGenRegion var0, ChunkAccess var1) {
        final ChunkPos var2 = var1.getPos();
        final int var3 = var2.x;
        final int var4 = var2.z;
        final WorldgenRandom var5 = new WorldgenRandom();
        var5.setBaseChunkSeed(var3, var4);

        // Generate base stone
        GeneratingChunkImpl chunk = new GeneratingChunkImpl(var1, biomeGenerator);

        BaseTerrainGenerator baseTerrainGenerator = this.baseTerrainGenerator;
        if (!(baseTerrainGenerator instanceof NoiseToTerrainGenerator)) {
            // If a noise generator is present, then the base terrain was already generated
            // in buildNoise
            baseTerrainGenerator.setBlocksInChunk(chunk);
        }

        // Generate early decorations
        this.worldDecorator.spawnCustomBaseDecorations(BaseDecorationType.RAW_GENERATION, chunk);

        // Generate surface
        if (this.worldDecorator.isDefaultEnabled(BaseDecorationType.SURFACE)) {
            final ChunkPos var6 = var1.getPos();
            final int var7 = var6.getMinBlockX();
            final int var8 = var6.getMinBlockZ();
            final BlockPos.MutableBlockPos var10 = new BlockPos.MutableBlockPos();
            for (int var11 = 0; var11 < 16; ++var11) {
                for (int var12 = 0; var12 < 16; ++var12) {
                    final int var13 = var7 + var11;
                    final int var14 = var8 + var12;
                    final int var15 = var1.getHeight(Heightmap.Types.WORLD_SURFACE_WG, var11, var12) + 1;
                    final double var16 = this.surfaceNoise
                            .getSurfaceNoiseValue(var13 * 0.0625, var14 * 0.0625, 0.0625, var11 * 0.0625) * 15.0;
                    final int var17 = this.settings.getMinSurfaceLevel();
                    var0.getBiome(var10.set(var7 + var11, var15, var8 + var12))
                            .buildSurfaceAt(var5, var1, var13, var14, var15, var16, this.defaultBlock, this.defaultFluid, this
                                    .getSeaLevel(), var17, var0.getSeed());
                }
            }
        }
        this.worldDecorator.spawnCustomBaseDecorations(BaseDecorationType.SURFACE, chunk);

        // Generate bedrock
        if (this.worldDecorator.isDefaultEnabled(BaseDecorationType.BEDROCK)) {
            this.setBedrock(var1, var5);
        }
        this.worldDecorator.spawnCustomBaseDecorations(BaseDecorationType.BEDROCK, chunk);

    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        throw new UnsupportedOperationException("Cannot serialize a custom chunk generator");
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, StructureFeatureManager structureManager,
            ChunkAccess chunkAccess) {
        BaseTerrainGenerator baseTerrainGenerator = this.baseTerrainGenerator;
        if ((baseTerrainGenerator instanceof NoiseToTerrainGenerator)) {
            return ((NoiseToTerrainGenerator) baseTerrainGenerator)
                    .fillFromNoise(executor, structureManager, chunkAccess);
        }

        // Do nothing, will be handled in buildBase
        return CompletableFuture.completedFuture(chunkAccess);
    }

    @Override
    public NoiseColumn getBaseColumn(int blockX, int blockZ, LevelHeightAccessor levelHeight) {
        // Generates a single column.

        if ((baseTerrainGenerator instanceof NoiseToTerrainGenerator)) {
            // Ask the noise generator
            return ((NoiseToTerrainGenerator) baseTerrainGenerator).getBaseColumn(blockX, blockZ, levelHeight);
        }

        // Generate an estimate of the column based on the max height
        int minY = levelHeight.getMinBuildHeight();
        int maxY = this.baseTerrainGenerator.getHeight(biomeGenerator, blockX, blockZ, HeightType.OCEAN_FLOOR);
        int seaLevel = this.getSeaLevel();
        BlockState[] blockData = new BlockState[Math.max(maxY, seaLevel) - minY];
        for (int i = 0; i < blockData.length; i++) {
            int y = i + minY;
            if (y >= maxY) {
                // Water
                blockData[i] = this.defaultFluid;
            } else {
                // Stone
                blockData[i] = this.defaultBlock;
            }
        }
        return new NoiseColumn(minY, blockData);
    }


    @Override
    public int getBaseHeight(int i, int j, Heightmap.Types heightType, LevelHeightAccessor levelHeight) {
        // Shortcut
        BaseTerrainGenerator baseTerrainGenerator = this.baseTerrainGenerator;
        if (baseTerrainGenerator instanceof NoiseToTerrainGenerator) {
            return ((NoiseToTerrainGenerator) baseTerrainGenerator).getBaseHeight(i, j, heightType, levelHeight);
        }

        // Ask the base terrain generator
        HeightType wHeightType = heightType == Heightmap.Types.OCEAN_FLOOR_WG ? HeightType.OCEAN_FLOOR
                : HeightType.WORLD_SURFACE;
        return this.baseTerrainGenerator.getHeight(biomeGenerator, i, j, wHeightType);
    }

    public BaseTerrainGenerator getBaseTerrainGenerator() {
        return baseTerrainGenerator;
    }



    public BiomeGenerator getBiomeGenerator() {
        return biomeGenerator;
    }

    @Override
    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(final Biome var0,
            final StructureFeatureManager var1, final MobCategory var2, final BlockPos var3) {
        // Copied from NoiseBasedChunkGenerator
        if (var1.getStructureAt(var3, true, StructureFeature.SWAMP_HUT).isValid()) {
            if (var2 == MobCategory.MONSTER) {
                return StructureFeature.SWAMP_HUT.getSpecialEnemies();
            }
            if (var2 == MobCategory.CREATURE) {
                return StructureFeature.SWAMP_HUT.getSpecialAnimals();
            }
        }
        if (var2 == MobCategory.MONSTER) {
            if (var1.getStructureAt(var3, false, StructureFeature.PILLAGER_OUTPOST).isValid()) {
                return StructureFeature.PILLAGER_OUTPOST.getSpecialEnemies();
            }
            if (var1.getStructureAt(var3, false, StructureFeature.OCEAN_MONUMENT).isValid()) {
                return StructureFeature.OCEAN_MONUMENT.getSpecialEnemies();
            }
            if (var1.getStructureAt(var3, true, StructureFeature.NETHER_BRIDGE).isValid()) {
                return StructureFeature.NETHER_BRIDGE.getSpecialEnemies();
            }
        }
        if (var2 == MobCategory.UNDERGROUND_WATER_CREATURE
                && var1.getStructureAt(var3, false, StructureFeature.OCEAN_MONUMENT).isValid()) {
            return StructureFeature.OCEAN_MONUMENT
                    .getSpecialUndergroundWaterAnimals();
        }
        return super.getMobsAt(var0, var1, var2, var3);
    }

    private void injectWorldChunkManager(BiomeSource worldChunkManager) {
        try {
            for (Field field : ReflectionUtil.getAllFieldsOfType(getClass().getSuperclass(), BiomeSource.class)) {
                field.set(this, worldChunkManager);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to update the biome generator field", e);
        }
        if (this.biomeSource != worldChunkManager || this.runtimeBiomeSource != worldChunkManager) {
            throw new RuntimeException("Failed to update the biome generator field - old value is still present");
        }
    }
    /**
     * When {@link #setBiomeGenerator(BiomeGenerator)}, the biome generator will get
     * injected into the vanilla terrain generator, if it was active. This method is
     * necessary to reset that.
     */
    public void resetBiomeGenerator() {
        setBiomeGenerator(this.originalBiomeGenerator);
    }

    public void setBaseChunkGenerator(BaseTerrainGenerator baseTerrainGenerator) {
        this.baseTerrainGenerator = Objects.requireNonNull(baseTerrainGenerator, "baseTerrainGenerator");
    }

    private void setBedrock(final ChunkAccess var0, final Random var1) {
        final BlockPos.MutableBlockPos var2 = new BlockPos.MutableBlockPos();
        final int var3 = var0.getPos().getMinBlockX();
        final int var4 = var0.getPos().getMinBlockZ();
        final NoiseGeneratorSettings var5 = this.settings;
        final int var6 = var5.noiseSettings().minY();
        final int var7 = var6 + var5.getBedrockFloorPosition();
        final int var8 = this.height - 1 + var6 - var5.getBedrockRoofPosition();
        final int var10 = var0.getMinBuildHeight();
        final int var11 = var0.getMaxBuildHeight();
        final boolean var12 = var8 + 5 - 1 >= var10 && var8 < var11;
        final boolean var13 = var7 + 5 - 1 >= var10 && var7 < var11;
        if (!var12 && !var13) {
            return;
        }
        for (final BlockPos var14 : BlockPos.betweenClosed(var3, 0, var4, var3 + 15, 0, var4 + 15)) {
            if (var12) {
                for (int var15 = 0; var15 < 5; ++var15) {
                    if (var15 <= var1.nextInt(5)) {
                        var0.setBlockState(var2.set(var14.getX(), var8 - var15, var14.getZ()),
                                Blocks.BEDROCK.defaultBlockState(), false);
                    }
                }
            }
            if (var13) {
                for (int var15 = 4; var15 >= 0; --var15) {
                    if (var15 <= var1.nextInt(5)) {
                        var0.setBlockState(var2.set(var14.getX(), var7 + var15, var14.getZ()),
                                Blocks.BEDROCK.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    public void setBiomeGenerator(BiomeGenerator biomeGenerator) {
        this.biomeGenerator = Objects.requireNonNull(biomeGenerator, "biomeGenerator");

        // Update Minecraft's field too
        BiomeSource worldChunkManager = InjectedBiomeGenerator.wrapOrUnwrap(this.biomeRegistry, biomeGenerator);
        this.injectWorldChunkManager(worldChunkManager);

        // Inject in base terrain generator too
        if (this.baseTerrainGenerator instanceof BaseTerrainGeneratorImpl) {
            ((BaseTerrainGeneratorImpl) this.baseTerrainGenerator).replaceWorldChunkManager(worldChunkManager);
        }
    }

    @Override
    public void spawnOriginalMobs(final WorldGenRegion var0) {
        // Copied from NoiseBaseChunkGenerator
        final ChunkPos var = var0.getCenter();
        final Biome var2 = var0.getBiome(var.getWorldPosition());
        final WorldgenRandom var3 = new WorldgenRandom();
        var3.setDecorationSeed(var0.getSeed(), var.getMinBlockX(), var.getMinBlockZ());
        NaturalSpawner.spawnMobsForChunkGeneration(var0, var2, var, var3);
    }

    @Override
    public ChunkGenerator withSeed(long seed) {
        return this; // Not sure how to properly support this
    }

}
