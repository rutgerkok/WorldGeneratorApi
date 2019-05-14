package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import net.minecraft.server.v1_14_R1.BiomeBase;
import net.minecraft.server.v1_14_R1.BiomeBase.BiomeMeta;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_14_R1.ChunkGeneratorAbstract;
import net.minecraft.server.v1_14_R1.EnumCreatureType;
import net.minecraft.server.v1_14_R1.GeneratorAccess;
import net.minecraft.server.v1_14_R1.GeneratorSettingsDefault;
import net.minecraft.server.v1_14_R1.HeightMap;
import net.minecraft.server.v1_14_R1.HeightMap.Type;
import net.minecraft.server.v1_14_R1.IChunkAccess;
import net.minecraft.server.v1_14_R1.MobSpawnerCat;
import net.minecraft.server.v1_14_R1.MobSpawnerPatrol;
import net.minecraft.server.v1_14_R1.MobSpawnerPhantom;
import net.minecraft.server.v1_14_R1.NoiseGenerator3;
import net.minecraft.server.v1_14_R1.NoiseGeneratorOctaves;
import net.minecraft.server.v1_14_R1.RegionLimitedWorldAccess;
import net.minecraft.server.v1_14_R1.SeededRandom;
import net.minecraft.server.v1_14_R1.SpawnerCreature;
import net.minecraft.server.v1_14_R1.WorldGenStage;
import net.minecraft.server.v1_14_R1.WorldGenerator;
import net.minecraft.server.v1_14_R1.WorldServer;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator.GeneratingChunk;
import nl.rutgerkok.worldgeneratorapi.BaseTerrainGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseTerrainGenerator.HeightType;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.decoration.BaseDecorationType;
import nl.rutgerkok.worldgeneratorapi.internal.BiomeGeneratorImpl;
import nl.rutgerkok.worldgeneratorapi.internal.ReflectionUtil;
import nl.rutgerkok.worldgeneratorapi.internal.WorldDecoratorImpl;

public final class InjectedChunkGenerator extends ChunkGeneratorAbstract<GeneratorSettingsDefault> {

    public static class GeneratingChunkImpl implements GeneratingChunk {

        private final int chunkX;
        private final int chunkZ;
        private final ChunkDataImpl blocks;
        private final BiomeGenerator biomeGenerator;
        private final BiomeGridImpl biomeGrid;
        public final IChunkAccess internal;

        GeneratingChunkImpl(IChunkAccess internal, BiomeGenerator biomeGenerator) {
            this.internal = Objects.requireNonNull(internal, "internal");
            this.chunkX = internal.getPos().x;
            this.chunkZ = internal.getPos().z;
            this.blocks = new ChunkDataImpl(internal);
            this.biomeGrid = new BiomeGridImpl(internal.getBiomeIndex());

            this.biomeGenerator = biomeGenerator;
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

    private static final float[] floatArray;
    static {
        try {
            floatArray = (float[]) ReflectionUtil.getFieldOfType(ChunkGeneratorAbstract.class, float[].class).get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private final org.bukkit.World world;
    /**
     * Could someone ask Mojang why world generation controls these mobs?
     */
    private final MobSpawnerPhantom phantomSpawner = new MobSpawnerPhantom();
    private final MobSpawnerPatrol patrolSpawner = new MobSpawnerPatrol();

    private final MobSpawnerCat catSpawner = new MobSpawnerCat();
    private final NoiseGenerator3 surfaceNoise;

    private final NoiseGeneratorOctaves noiseOctaves16;

    public final WorldDecoratorImpl worldDecorator = new WorldDecoratorImpl();
    private BaseTerrainGenerator baseTerrainGenerator;

    private final BiomeGenerator biomeGenerator;

    public InjectedChunkGenerator(WorldServer world, BaseTerrainGenerator baseChunkGenerator) {
        super(world, world.getChunkProvider().getChunkGenerator().getWorldChunkManager(),
                4, 8, 256, world.getChunkProvider().getChunkGenerator().getSettings(), true);
        this.world = world.getWorld();

        SeededRandom seededrandom = new SeededRandom(this.seed);
        surfaceNoise = new NoiseGenerator3(seededrandom, 4);

        this.e.a(2620);
        this.noiseOctaves16 = new NoiseGeneratorOctaves(this.e, 16);

        this.biomeGenerator = new BiomeGeneratorImpl(world.getChunkProvider()
                .getChunkGenerator().getWorldChunkManager());
        setBaseChunkGenerator(baseChunkGenerator);
    }

    @Override
    protected double a(double d0, double d1, int i) {
        // No idea what this is calculating - we only know that it has got something to
        // do with terrain shape
        double d3 = (i - (8.5D + d0 * 8.5D / 8.0D * 4.0D)) * 12.0D * 128.0D / 256.0D / d1;
        if (d3 < 0.0D) {
            d3 *= 4.0D;
        }

        return d3;
    }

    @Override
    protected void a(double[] adouble, int i, int j) {
        // Main noise calculator
        BaseTerrainGenerator terrainGenerator = this.baseTerrainGenerator;
        if (terrainGenerator instanceof NoiseToTerrainGenerator) {
            ((NoiseToTerrainGenerator) terrainGenerator).a(adouble, i, j);
        }
    }

    @Override
    protected double[] a(int i, int j) {
        // Biome noise calculator
        BaseTerrainGenerator terrainGenerator = this.baseTerrainGenerator;
        if (terrainGenerator instanceof NoiseToTerrainGenerator) {
            // Found a custom one
            return ((NoiseToTerrainGenerator) terrainGenerator).a(i, j);
        }

        // Fall back to copy of vanilla
        double[] adouble = new double[2];
        float f = 0.0F;
        float f1 = 0.0F;
        float f2 = 0.0F;
        float f3 = this.c.b(i, j).g();

        for (int k = -2; k <= 2; ++k) {
            for (int l = -2; l <= 2; ++l) {
                BiomeBase biomebase = this.c.b(i + k, j + l);
                float f4 = biomebase.g();
                float f5 = biomebase.k();

                if (f4 < -1.8F) {
                    f4 = -1.8F;
                }

                float f6 = floatArray[k + 2 + (l + 2) * 5] / (f4 + 2.0F);
                if (biomebase.g() > f3) {
                    f6 /= 2.0F;
                }

                f += f5 * f6;
                f1 += f4 * f6;
                f2 += f6;
            }
        }

        f /= f2;
        f1 /= f2;
        f = f * 0.9F + 0.1F;
        f1 = (f1 * 4.0F - 1.0F) / 8.0F;
        adouble[0] = f1 + this.c(i, j);
        adouble[1] = f;
        return adouble;
    }

    @Override
    public void addDecorations(RegionLimitedWorldAccess populationArea) {
        this.worldDecorator.spawnDecorations(this, populationArea);
    }

    @Override
    public void addMobs(RegionLimitedWorldAccess regionlimitedworldaccess) {
        final int i = regionlimitedworldaccess.a();
        final int j = regionlimitedworldaccess.b();
        final BiomeBase biomebase = regionlimitedworldaccess.getChunkAt(i, j).getBiomeIndex()[0];
        final SeededRandom seededrandom = new SeededRandom();
        seededrandom.a(regionlimitedworldaccess.getSeed(), i << 4, j << 4);
        SpawnerCreature.a(regionlimitedworldaccess, biomebase, i, j, seededrandom);
    }

    @Override
    public void buildBase(IChunkAccess ichunkaccess) {
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
        if (this.worldDecorator.isDefaultEnabled(BaseDecorationType.SURFACE)) {

            BiomeBase[] abiomebase = ichunkaccess.getBiomeIndex();
            for (int i1 = 0; i1 < 16; ++i1) {
                for (int j1 = 0; j1 < 16; ++j1) {
                    int k1 = blockX + i1;
                    int l1 = blockZ + j1;
                    int i2 = ichunkaccess.a(Type.WORLD_SURFACE_WG, i1, j1) + 1;
                    double d1 = this.surfaceNoise.a(k1 * 0.0625D, l1 * 0.0625D, 0.0625D, i1 * 0.0625D);
                    abiomebase[j1 * 16 + i1].a(seededrandom, ichunkaccess, k1, l1, i2, d1, this.getSettings().r(),
                            this.getSettings().s(), this.getSeaLevel(), this.a.getSeed());
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
    public void buildNoise(GeneratorAccess generatoraccess, IChunkAccess ichunkaccess) {
        BaseTerrainGenerator baseTerrainGenerator = this.baseTerrainGenerator;
        if ((baseTerrainGenerator instanceof NoiseToTerrainGenerator)) {
            ((NoiseToTerrainGenerator) baseTerrainGenerator).buildNoise(generatoraccess, ichunkaccess);
        }

        // Do nothing, will be handled in buildBase
    }

    private double c(int i, int j) {
        // No idea what this is calculating
        double d0 = this.noiseOctaves16.a(i * 200, 10.0D, j * 200, 1.0D, 0.0D, true) / 8000.0D;
        if (d0 < 0.0D) {
            d0 = -d0 * 0.3D;
        }

        d0 = d0 * 3.0D - 2.0D;
        if (d0 < 0.0D) {
            d0 /= 28.0D;
        } else {
            if (d0 > 1.0D) {
                d0 = 1.0D;
            }

            d0 /= 40.0D;
        }

        return d0;
    }

    @Override
    public void doCarving(IChunkAccess chunkAccess, WorldGenStage.Features stage) {
        GeneratingChunkImpl generatingChunk = new GeneratingChunkImpl(chunkAccess, biomeGenerator);
        this.worldDecorator.spawnCarvers(generatingChunk, stage, this.getSeaLevel(), this.seed);
    }

    @Override
    public void doMobSpawning(WorldServer worldserver, boolean flag, boolean flag1) {
        this.phantomSpawner.a(worldserver, flag, flag1);
        this.patrolSpawner.a(worldserver, flag, flag1);
        this.catSpawner.a(worldserver, flag, flag1);
    }

    public BaseTerrainGenerator getBaseTerrainGenerator() {
        return baseTerrainGenerator;
    }

    @Override
    public int getBaseHeight(int i, int j, Type heightType) {
        // Shortcut
        BaseTerrainGenerator baseTerrainGenerator = this.baseTerrainGenerator;
        if (baseTerrainGenerator instanceof NoiseToTerrainGenerator) {
            ((NoiseToTerrainGenerator) baseTerrainGenerator).getBaseHeight(i, j, heightType);
        }

        // Ask the base terrain generator
        String typeName = heightType.name().replace("_WG", "");
        return this.baseTerrainGenerator.getHeight(i, j, HeightType.valueOf(typeName));
    }

    public BiomeGenerator getBiomeGenerator() {
        return biomeGenerator;
    }

    @Override
    public List<BiomeMeta> getMobsFor(EnumCreatureType enumcreaturetype, BlockPosition blockposition) {
        if (WorldGenerator.SWAMP_HUT.c(this.a, blockposition)) {
            if (enumcreaturetype == EnumCreatureType.MONSTER) {
                return WorldGenerator.SWAMP_HUT.e();
            }

            if (enumcreaturetype == EnumCreatureType.CREATURE) {
                return WorldGenerator.SWAMP_HUT.f();
            }
        } else if (enumcreaturetype == EnumCreatureType.MONSTER) {
            if (WorldGenerator.PILLAGER_OUTPOST.a(this.a, blockposition)) {
                return WorldGenerator.PILLAGER_OUTPOST.e();
            }

            if (WorldGenerator.OCEAN_MONUMENT.a(this.a, blockposition)) {
                return WorldGenerator.OCEAN_MONUMENT.e();
            }
        }

        return super.getMobsFor(enumcreaturetype, blockposition);
    }

    @Override
    public int getSeaLevel() {
        return world.getSeaLevel();
    }

    @Override
    public int getSpawnHeight() {
        return world.getSeaLevel() + 1;
    }

    public void setBaseChunkGenerator(BaseTerrainGenerator baseTerrainGenerator) {
        this.baseTerrainGenerator = Objects.requireNonNull(baseTerrainGenerator, "baseTerrainGenerator");
    }

}
