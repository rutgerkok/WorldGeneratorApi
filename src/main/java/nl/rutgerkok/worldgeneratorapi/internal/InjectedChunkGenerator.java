package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R2.generator.CraftChunkData;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import net.minecraft.server.v1_13_R2.BiomeBase;
import net.minecraft.server.v1_13_R2.BiomeBase.BiomeMeta;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_13_R2.ChunkGeneratorAbstract;
import net.minecraft.server.v1_13_R2.ChunkSection;
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
import net.minecraft.server.v1_13_R2.WorldGenerator;
import net.minecraft.server.v1_13_R2.WorldServer;

import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator.GeneratingChunk;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;

final class InjectedChunkGenerator extends ChunkGeneratorAbstract<GeneratorSettingsDefault> {

    private static class CustomBiomeGrid implements BiomeGrid {
        private final BiomeBase[] biomeArray;

        private CustomBiomeGrid(BiomeBase[] biome) {
            this.biomeArray = biome;
        }

        @Override
        public Biome getBiome(final int x, final int z) {
            return CraftBlock.biomeBaseToBiome(this.biomeArray[z << 4 | x]);
        }

        @Override
        public void setBiome(final int x, final int z, final Biome bio) {
            this.biomeArray[z << 4 | x] = CraftBlock.biomeToBiomeBase(bio);
        }
    }

    private final org.bukkit.World world;

    /**
     * Could someone ask Mojang why world generation controls Phantom spawning?
     */
    private final MobSpawnerPhantom phantomSpawner = new MobSpawnerPhantom();
    private final GeneratorSettingsDefault defaultSettings = new GeneratorSettingsDefault();
    private final NoiseGenerator3 surfaceNoise;
    final WorldDecoratorImpl worldDecorator = new WorldDecoratorImpl();

    private BaseChunkGenerator baseChunkGenerator;
    private final BiomeGenerator biomeGenerator;

    InjectedChunkGenerator(WorldServer world, BaseChunkGenerator baseChunkGenerator) {
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
    public void addMobs(RegionLimitedWorldAccess regionlimitedworldaccess) {
        final int i = regionlimitedworldaccess.a();
        final int j = regionlimitedworldaccess.b();
        final BiomeBase biomebase = regionlimitedworldaccess.b(i, j).getBiomeIndex()[0];
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
        CustomBiomeGrid biomes = new CustomBiomeGrid(this.c.getBiomeBlock(i * 16, j * 16, 16, 16));

        // Generate blocks
        this.generateBaseChunk(i, j, ichunkaccess, biomes);
        ichunkaccess.a(biomes.biomeArray);
        ichunkaccess.a(HeightMap.Type.WORLD_SURFACE_WG, HeightMap.Type.OCEAN_FLOOR_WG);

        // Generate surface
        this.a(ichunkaccess, biomes.biomeArray, seededrandom, world.getSeaLevel());

        // Generate bedrock
        this.a(ichunkaccess, seededrandom);
        ichunkaccess.a(HeightMap.Type.WORLD_SURFACE_WG, HeightMap.Type.OCEAN_FLOOR_WG);
        ichunkaccess.a(ChunkStatus.BASE);
    }

    private void generateBaseChunk(int i, int j, IChunkAccess ichunkaccess, BiomeGrid biomes) {
        int chunkX = ichunkaccess.getPos().x;
        int chunkZ = ichunkaccess.getPos().z;
        CraftChunkData chunkData = new CraftChunkData(this.world);
        GeneratingChunk chunk = new GeneratingChunk() {

            @Override
            public BiomeGenerator getBiomeGenerator() {
                return biomeGenerator;
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

        // Generate blocks and update chunk
        baseChunkGenerator.setBlocksInChunk(chunk);
        try {
            Field sectionsField = CraftChunkData.class.getDeclaredField("sections");
            sectionsField.setAccessible(true);
            ChunkSection[] sections = (ChunkSection[]) sectionsField.get(chunkData);
            ChunkSection[] csect = ichunkaccess.getSections();
            for (int scnt = Math.min(csect.length, sections.length), sec = 0; sec < scnt; ++sec) {
                if (sections[sec] != null) {
                    final ChunkSection section = sections[sec];
                    csect[sec] = section;
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    BaseChunkGenerator getBaseChunkGenerator() {
        return baseChunkGenerator;
    }

    BiomeGenerator getBiomeGenerator() {
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

    void setBaseChunkGenerator(BaseChunkGenerator baseChunkGenerator) {
        this.baseChunkGenerator = Objects.requireNonNull(baseChunkGenerator, "baseChunkGenerator");
    }

}
