package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import java.util.List;
import java.util.Objects;

import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import net.minecraft.server.v1_13_R2.BiomeBase;
import net.minecraft.server.v1_13_R2.BiomeBase.BiomeMeta;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_13_R2.ChunkGeneratorAbstract;
import net.minecraft.server.v1_13_R2.ChunkStatus;
import net.minecraft.server.v1_13_R2.EnumCreatureType;
import net.minecraft.server.v1_13_R2.GeneratorSettingsDefault;
import net.minecraft.server.v1_13_R2.HeightMap;
import net.minecraft.server.v1_13_R2.IChunkAccess;
import net.minecraft.server.v1_13_R2.MobSpawnerPhantom;
import net.minecraft.server.v1_13_R2.NoiseGenerator3;
import net.minecraft.server.v1_13_R2.RegionLimitedWorldAccess;
import net.minecraft.server.v1_13_R2.SeededRandom;
import net.minecraft.server.v1_13_R2.SpawnerCreature;
import net.minecraft.server.v1_13_R2.World;
import net.minecraft.server.v1_13_R2.WorldGenFeatureSwampHut;
import net.minecraft.server.v1_13_R2.WorldGenStage;
import net.minecraft.server.v1_13_R2.WorldGenerator;
import net.minecraft.server.v1_13_R2.WorldServer;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator.GeneratingChunk;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.decoration.BaseDecorationType;
import nl.rutgerkok.worldgeneratorapi.internal.BiomeGeneratorImpl;
import nl.rutgerkok.worldgeneratorapi.internal.WorldDecoratorImpl;

public final class InjectedChunkGenerator extends ChunkGeneratorAbstract<GeneratorSettingsDefault> {

    private static class GeneratingChunkImpl implements GeneratingChunk {

        private final int chunkX;
        private final int chunkZ;
        private final ChunkDataImpl blocks;
        private final BiomeGenerator biomeGenerator;
        private final BiomeGridImpl biomeGrid;

        GeneratingChunkImpl(IChunkAccess internal, BiomeGenerator biomeGenerator) {
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

    private final org.bukkit.World world;
    /**
     * Could someone ask Mojang why world generation controls Phantom spawning?
     */
    private final MobSpawnerPhantom phantomSpawner = new MobSpawnerPhantom();
    private final GeneratorSettingsDefault defaultSettings = new GeneratorSettingsDefault();
    private final NoiseGenerator3 surfaceNoise;

    public final WorldDecoratorImpl worldDecorator = new WorldDecoratorImpl();
    private BaseChunkGenerator baseChunkGenerator;

    private final BiomeGenerator biomeGenerator;

    public InjectedChunkGenerator(WorldServer world, BaseChunkGenerator baseChunkGenerator) {
        super(world, world.getChunkProvider().getChunkGenerator().getWorldChunkManager());
        this.world = world.getWorld();

        SeededRandom seededrandom = new SeededRandom(this.b);
        surfaceNoise = new NoiseGenerator3(seededrandom, 4);

        this.biomeGenerator = new BiomeGeneratorImpl(world.getChunkProvider()
                .getChunkGenerator().getWorldChunkManager());
        setBaseChunkGenerator(baseChunkGenerator);
    }

    @Override
    public double[] a(int i, int j) {
        return this.surfaceNoise.a(i << 4, j << 4, 16, 16, 0.0625, 0.0625, 1.0);
    }

    @Override
    public int a(World world, boolean flag, boolean flag1) {
        final byte b0 = 0;
        final int i = b0 + this.phantomSpawner.a(world, flag, flag1);
        return i;
    }

    @Override
    public void addDecorations(RegionLimitedWorldAccess populationArea) {
        this.worldDecorator.spawnDecorations(this, populationArea);
    }

    @Override
    public void addFeatures(RegionLimitedWorldAccess world, WorldGenStage.Features stage) {
        this.worldDecorator.spawnCarvers(world, stage, new SeededRandom(this.b));
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
    public void createChunk(IChunkAccess ichunkaccess) {
        ChunkCoordIntPair chunkcoordintpair = ichunkaccess.getPos();
        int i = chunkcoordintpair.x;
        int j = chunkcoordintpair.z;
        SeededRandom seededrandom = new SeededRandom();
        seededrandom.a(i, j);

        // Generate zoomed-in biomes
        ichunkaccess.a(this.c.getBiomeBlock(i * 16, j * 16, 16, 16));

        // Generate blocks
        GeneratingChunkImpl chunk = new GeneratingChunkImpl(ichunkaccess, biomeGenerator);
        baseChunkGenerator.setBlocksInChunk(chunk);

        // Generate early decorations
        this.worldDecorator.spawnCustomBaseDecorations(BaseDecorationType.RAW_GENERATION, chunk);

        // Heightmap calculations
        ichunkaccess.a(HeightMap.Type.WORLD_SURFACE_WG, HeightMap.Type.OCEAN_FLOOR_WG);

        // Generate surface
        if (this.worldDecorator.isDefaultEnabled(BaseDecorationType.SURFACE)) {
            this.a(ichunkaccess, ichunkaccess.getBiomeIndex(), seededrandom, world.getSeaLevel());
        }
        this.worldDecorator.spawnCustomBaseDecorations(BaseDecorationType.SURFACE, chunk);

        // Generate bedrock
        if (this.worldDecorator.isDefaultEnabled(BaseDecorationType.BEDROCK)) {
            this.a(ichunkaccess, seededrandom);
        }
        this.worldDecorator.spawnCustomBaseDecorations(BaseDecorationType.BEDROCK, chunk);

        ichunkaccess.a(HeightMap.Type.WORLD_SURFACE_WG, HeightMap.Type.OCEAN_FLOOR_WG);
        ichunkaccess.a(ChunkStatus.BASE);
    }

    public BaseChunkGenerator getBaseChunkGenerator() {
        return baseChunkGenerator;
    }

    public BiomeGenerator getBiomeGenerator() {
        return biomeGenerator;
    }

    @Override
    public List<BiomeMeta> getMobsFor(EnumCreatureType enumcreaturetype, BlockPosition blockposition) {
        final BiomeBase biomebase = this.a.getBiome(blockposition);
        return (enumcreaturetype == EnumCreatureType.MONSTER
                && ((WorldGenFeatureSwampHut) WorldGenerator.l).d(this.a, blockposition))
                ? WorldGenerator.l.d()
                        : ((enumcreaturetype == EnumCreatureType.MONSTER && WorldGenerator.n.b(this.a, blockposition))
                                ? WorldGenerator.n.d()
                                        : biomebase.getMobs(enumcreaturetype));

    }

    @Override
    public GeneratorSettingsDefault getSettings() {
        return defaultSettings;
    }

    @Override
    public int getSpawnHeight() {
        return world.getSeaLevel() + 1;
    }



    public void setBaseChunkGenerator(BaseChunkGenerator baseChunkGenerator) {
        this.baseChunkGenerator = Objects.requireNonNull(baseChunkGenerator, "baseChunkGenerator");
    }

}
