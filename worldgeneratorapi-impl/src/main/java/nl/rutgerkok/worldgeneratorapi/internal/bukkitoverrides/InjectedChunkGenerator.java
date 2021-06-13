package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

import org.bukkit.HeightMap;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.util.noise.NoiseGenerator;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.Features;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
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

    private final NoiseGenerator surfaceNoise;
    protected final BlockState f;
    protected final BlockState g;
    protected final WorldGenSettings h;
    private final int x;
    public final WorldDecoratorImpl worldDecorator = new WorldDecoratorImpl();
    private BaseTerrainGenerator baseTerrainGenerator;
    private BiomeGenerator biomeGenerator;

    /**
     * Original biome generator, ready to be restored when the plugin unloads.
     */
    private final BiomeGeneratorImpl originalBiomeGenerator;
    private final Registry<Biome> biomeRegistry;

    public InjectedChunkGenerator(BiomeSource worldchunkmanager, Registry<Biome> biomeRegistry,
            BaseTerrainGenerator baseChunkGenerator, long seed, WorldGenSettings settings) {
        super(worldchunkmanager, worldchunkmanager, settings.a(), seed);

        this.h = settings;
        NoiseSettings noisesettings = settings.b();
        this.x = noisesettings.a();
        this.f = settings.c();
        this.g = settings.d();
        WorldgenRandom e = new WorldgenRandom(seed);
        new NoiseGeneratorOctaves(e, IntStream.rangeClosed(-15, 0));
        new NoiseGeneratorOctaves(e, IntStream.rangeClosed(-15, 0));
        new NoiseGeneratorOctaves(e, IntStream.rangeClosed(-7, 0));
        this.surfaceNoise = noisesettings.i()
                ? new NoiseGenerator3(e, IntStream.rangeClosed(-3, 0))
                : new NoiseGeneratorOctaves(e, IntStream.rangeClosed(-3, 0));

        this.biomeRegistry = Objects.requireNonNull(biomeRegistry, "biomeRegistry");
        this.originalBiomeGenerator = new BiomeGeneratorImpl(biomeRegistry, this.c);
        this.biomeGenerator = this.originalBiomeGenerator;

        setBaseChunkGenerator(baseChunkGenerator);
    }

    @Override
    protected Codec<? extends ChunkGenerator> a() {
        throw new UnsupportedOperationException("Cannot serialize a custom chunk generator");
    }

    private void a(ChunkAccess ichunkaccess, Random random) {
        // Bedrock

        MutableBlockPos blockposition_mutableblockposition = new MutableBlockPosition();
        int i = ichunkaccess.getPos().d();
        int j = ichunkaccess.getPos().e();
        int k = this.h.f();
        int l = this.x - 1 - this.h.e();
        boolean flag1 = l + 4 >= 0 && l < this.x;
        boolean flag2 = k + 4 >= 0 && k < this.x;
        if (flag1 || flag2) {
            Iterator<BlockPosition> iterator = BlockPosition.b(i, 0, j, i + 15, 0, j + 15).iterator();

            while (true) {
                BlockPosition blockposition;
                int i1;
                do {
                    if (!iterator.hasNext()) {
                        return;
                    }

                    blockposition = iterator.next();
                    if (flag1) {
                        for (i1 = 0; i1 < 5; ++i1) {
                            if (i1 <= random.nextInt(5)) {
                                ichunkaccess.setType(blockposition_mutableblockposition.d(blockposition.getX(), l - i1,
                                        blockposition.getZ()), Blocks.BEDROCK.getBlockData(), false);
                            }
                        }
                    }
                } while (!flag2);

                for (i1 = 4; i1 >= 0; --i1) {
                    if (i1 <= random.nextInt(5)) {
                        ichunkaccess.setType(blockposition_mutableblockposition.d(blockposition.getX(), k + i1,
                                blockposition.getZ()), Blocks.BEDROCK.getBlockData(), false);
                    }
                }
            }
        }
    }

    protected BlockState a(double d0, int i) {
        BlockState iblockdata;
        if (d0 > 0.0D) {
            iblockdata = this.f;
        } else if (i < this.getSeaLevel()) {
            iblockdata = this.g;
        } else {
            iblockdata = k;
        }

        return iblockdata;
    }

    @Override
    public IBlockAccess a(int blockX, int blockZ) {
        // Generates a single column.

        if ((baseTerrainGenerator instanceof NoiseToTerrainGenerator)) {
            // Ask the noise generator
            return ((NoiseToTerrainGenerator) baseTerrainGenerator).a(blockX, blockZ);
        }

        // Generate an estimate of the column based on the max height
        int maxY = this.baseTerrainGenerator.getHeight(biomeGenerator, blockX, blockZ, HeightType.OCEAN_FLOOR);
        int seaLevel = this.getSeaLevel();
        IBlockData[] blockData = new IBlockData[Math.max(maxY, seaLevel)];
        for (int i = 0; i < blockData.length; i++) {
            if (i >= maxY) {
                // Water
                blockData[i] = this.g;
            } else {
                // Stone
                blockData[i] = this.f;
            }
        }
        return new BlockColumn(blockData);
    }

    @Override
    public void addDecorations(RegionLimitedWorldAccess regionlimitedworldaccess, StructureManager structuremanager) {
        this.worldDecorator.spawnDecorations(this, this.b, structuremanager, regionlimitedworldaccess);
    }

    @Override
    public void addMobs(RegionLimitedWorldAccess regionlimitedworldaccess) {
        int i = regionlimitedworldaccess.a();
        int j = regionlimitedworldaccess.b();
        Biome biomebase = regionlimitedworldaccess.getBiome((new ChunkPos(i, j)).getMiddleBlockPosition());
        WorldgenRandom seededrandom = new WorldgenRandom();
        seededrandom.a(regionlimitedworldaccess.getSeed(), i << 4, j << 4);
        SpawnerCreature.a(regionlimitedworldaccess, biomebase, i, j, seededrandom);
    }

    @Override
    public void buildBase(RegionLimitedWorldAccess regionlimitedworldaccess, IChunkAccess ichunkaccess) {
        ChunkCoordIntPair chunkcoordintpair1 = ichunkaccess.getPos();
        int blockX = chunkcoordintpair1.d();
        int blockZ = chunkcoordintpair1.e();
        SeededRandom seededrandom = new SeededRandom();
        seededrandom.a(blockX, blockZ);

        // Generate base stone
        GeneratingChunkImpl chunk = new GeneratingChunkImpl(ichunkaccess, biomeGenerator);

        BaseTerrainGenerator baseTerrainGenerator = this.baseTerrainGenerator;
        if (!(baseTerrainGenerator instanceof NoiseToTerrainGenerator)) {
            // If a noise generator is present, then the base terrain was already generated
            // in buildNoise
            baseTerrainGenerator.setBlocksInChunk(chunk);
        }

        // Generate early decorations
        this.worldDecorator.spawnCustomBaseDecorations(BaseDecorationType.RAW_GENERATION, chunk);

        // Heightmap calculations
        HeightMap.a(ichunkaccess, EnumSet.of(HeightMap.Type.WORLD_SURFACE_WG, HeightMap.Type.OCEAN_FLOOR_WG));

        // Generate surface
        MutableBlockPos blockposition_mutableblockposition = new MutableBlockPos();
        if (this.worldDecorator.isDefaultEnabled(BaseDecorationType.SURFACE)) {
            for (int i1 = 0; i1 < 16; ++i1) {
                for (int j1 = 0; j1 < 16; ++j1) {
                    int k1 = blockX + i1;
                    int l1 = blockZ + j1;
                    int i2 = ichunkaccess.getHighestBlock(Heightmap.Types.WORLD_SURFACE_WG, i1, j1) + 1;
                    double d1 = this.surfaceNoise.a(k1 * 0.0625D, l1 * 0.0625D, 0.0625D, i1 * 0.0625D) * 15.0D;
                    regionlimitedworldaccess
                            .getBiome(blockposition_mutableblockposition.d(blockX + i1, i2, blockZ + j1)).a(
                            seededrandom, ichunkaccess, k1, l1, i2, d1, this.f, this.g, this.getSeaLevel(),
                            regionlimitedworldaccess.getSeed());
                }
            }

        }
        this.worldDecorator.spawnCustomBaseDecorations(BaseDecorationType.SURFACE, chunk);

        // Generate bedrock
        if (this.worldDecorator.isDefaultEnabled(BaseDecorationType.BEDROCK)) {
            this.a(ichunkaccess, seededrandom);
        }
        this.worldDecorator.spawnCustomBaseDecorations(BaseDecorationType.BEDROCK, chunk);
    }

    @Override
    public void buildNoise(LevelAccessor generatoraccess, StructureManager structureManager,
            ChunkAccess ichunkaccess) {
        BaseTerrainGenerator baseTerrainGenerator = this.baseTerrainGenerator;
        if ((baseTerrainGenerator instanceof NoiseToTerrainGenerator)) {
            ((NoiseToTerrainGenerator) baseTerrainGenerator).buildNoise(generatoraccess, structureManager,
                    ichunkaccess);
        }

        // Do nothing, will be handled in buildBase

    }

    @Override
    public void doCarving(long seed, BiomeManager biomeManager, ChunkAccess chunkAccess,
            Features stage) {
        BiomeManager soupedUpBiomeManager = biomeManager.a(this.b); // No idea why this is necessary
        GeneratingChunkImpl generatingChunk = new GeneratingChunkImpl(chunkAccess, biomeGenerator);
        this.worldDecorator.spawnCarvers(soupedUpBiomeManager, generatingChunk, stage, this.getSeaLevel(), seed);
    }

    @Override
    public int getBaseHeight(int i, int j, Heightmap.Types heightType) {
        // Shortcut
        BaseTerrainGenerator baseTerrainGenerator = this.baseTerrainGenerator;
        if (baseTerrainGenerator instanceof NoiseToTerrainGenerator) {
            return ((NoiseToTerrainGenerator) baseTerrainGenerator).getBaseHeight(i, j, heightType);
        }

        // Ask the base terrain generator
        HeightType wHeightType = heightType == Type.OCEAN_FLOOR_WG ? HeightType.OCEAN_FLOOR : HeightType.WORLD_SURFACE;
        return this.baseTerrainGenerator.getHeight(biomeGenerator, i, j, wHeightType);
    }


    public BaseTerrainGenerator getBaseTerrainGenerator() {
        return baseTerrainGenerator;
    }

    public BiomeGenerator getBiomeGenerator() {
        return biomeGenerator;
    }

    @Override
    public int getGenerationDepth() {
        return this.x;
    }

    @Override
    public List<BiomeSettingsMobs.c> getMobsFor(Biome biomebase, StructureManager structuremanager,
            EnumCreatureType enumcreaturetype, BlockPosition blockposition) {
        if (structuremanager.a(blockposition, true, StructureGenerator.SWAMP_HUT).e()) {
            if (enumcreaturetype == EnumCreatureType.MONSTER) {
                return StructureGenerator.SWAMP_HUT.c();
            }

            if (enumcreaturetype == EnumCreatureType.CREATURE) {
                return StructureGenerator.SWAMP_HUT.j();
            }
        }

        if (enumcreaturetype == EnumCreatureType.MONSTER) {
            if (structuremanager.a(blockposition, false, StructureGenerator.PILLAGER_OUTPOST).e()) {
                return StructureGenerator.PILLAGER_OUTPOST.c();
            }

            if (structuremanager.a(blockposition, false, StructureGenerator.MONUMENT).e()) {
                return StructureGenerator.MONUMENT.c();
            }

            if (structuremanager.a(blockposition, true, StructureGenerator.FORTRESS).e()) {
                return StructureGenerator.FORTRESS.c();
            }
        }

        return super.getMobsFor(biomebase, structuremanager, enumcreaturetype, blockposition);
    }

    @Override
    public int getSeaLevel() {
        return this.h.g();
    }

    @Override
    public int getSpawnHeight() {
        return getSeaLevel() + 1;
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

}
