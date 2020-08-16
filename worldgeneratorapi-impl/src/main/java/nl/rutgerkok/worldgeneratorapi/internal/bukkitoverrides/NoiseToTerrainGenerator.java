package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.bukkit.craftbukkit.v1_16_R2.block.data.CraftBlockData;

import net.minecraft.server.v1_16_R2.BlockColumn;
import net.minecraft.server.v1_16_R2.BlockPosition.MutableBlockPosition;
import net.minecraft.server.v1_16_R2.Blocks;
import net.minecraft.server.v1_16_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R2.ChunkSection;
import net.minecraft.server.v1_16_R2.GeneratorAccess;
import net.minecraft.server.v1_16_R2.GeneratorSettingBase;
import net.minecraft.server.v1_16_R2.HeightMap;
import net.minecraft.server.v1_16_R2.HeightMap.Type;
import net.minecraft.server.v1_16_R2.IBlockAccess;
import net.minecraft.server.v1_16_R2.IBlockData;
import net.minecraft.server.v1_16_R2.IChunkAccess;
import net.minecraft.server.v1_16_R2.MathHelper;
import net.minecraft.server.v1_16_R2.NoiseSettings;
import net.minecraft.server.v1_16_R2.ProtoChunk;
import net.minecraft.server.v1_16_R2.SectionPosition;
import net.minecraft.server.v1_16_R2.StructureBoundingBox;
import net.minecraft.server.v1_16_R2.StructureGenerator;
import net.minecraft.server.v1_16_R2.StructureManager;
import net.minecraft.server.v1_16_R2.StructurePiece;
import net.minecraft.server.v1_16_R2.SystemUtils;
import net.minecraft.server.v1_16_R2.WorldGenFeatureDefinedStructureJigsawJunction;
import net.minecraft.server.v1_16_R2.WorldGenFeatureDefinedStructurePoolTemplate.Matching;
import net.minecraft.server.v1_16_R2.WorldGenFeaturePillagerOutpostPoolPiece;
import nl.rutgerkok.worldgeneratorapi.BaseNoiseGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseNoiseGenerator.TerrainSettings;
import nl.rutgerkok.worldgeneratorapi.BaseTerrainGenerator;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.InjectedChunkGenerator.GeneratingChunkImpl;

public final class NoiseToTerrainGenerator implements BaseTerrainGenerator {
    private static final float[] i = SystemUtils.a(new float[13824], (afloat) -> {
        for (int i = 0; i < 24; ++i) {
            for (int j = 0; j < 24; ++j) {
                for (int k = 0; k < 24; ++k) {
                    afloat[i * 24 * 24 + j * 24 + k] = (float) b(j - 12, k - 12, i - 12);
                }
            }
        }

    });

    private static final IBlockData k;
    static {
        k = Blocks.AIR.getBlockData();
    }

    private static double a(int i, int j, int k) {
        int l = i + 12;
        int i1 = j + 12;
        int j1 = k + 12;
        return l >= 0 && l < 24
                ? (i1 >= 0 && i1 < 24
                        ? (j1 >= 0 && j1 < 24 ? (double) NoiseToTerrainGenerator.i[j1 * 24 * 24 + l * 24 + i1] : 0.0)
                        : 0.0)
                : 0.0;
    }

    private static double b(int i, int j, int k) {
        double d0 = i * i + k * k;
        double d1 = j + 0.5D;
        double d2 = d1 * d1;
        double d3 = Math.pow(2.718281828459045D, -(d2 / 16.0D + d0 / 16.0D));
        double d4 = -d1 * MathHelper.i(d2 / 2.0D + d0 / 2.0D) / 2.0D;
        return d4 * d3;
    }

    private static NoiseSettings defaultNoiseSettings() {
        return GeneratorSettingBase.i().b();
    }

    private final int l;
    private final int m;
    private final int n;
    private final int o;

    private final int p;
    /**
     * Stone block.
     */
    private final IBlockData f;

    /**
     * Water block.
     */
    private final IBlockData g;
    /**
     * WorldGeneratorApi - custom noise function
     */
    private final BaseNoiseGenerator noiseGenerator;

    /**
     * WorldGeneratorApi - custom or vanilla biome generator
     */
    private final BiomeGenerator biomeGenerator;

    /**
     * WorldGeneratorApi - custom sea level
     */
    private final int seaLevel;
    private final GeneratorAccess generatorAccess;

    private final StructureManager structureManager;

    public NoiseToTerrainGenerator(GeneratorAccess generatorAccess, StructureManager structureManager,
            BiomeGenerator biomeGenerator, BaseNoiseGenerator noiseGenerator, long seed) {
        this.structureManager = Objects.requireNonNull(structureManager, "structureManager");
        this.generatorAccess = Objects.requireNonNull(generatorAccess, "generatorAccess");
        this.biomeGenerator = Objects.requireNonNull(biomeGenerator, "biomeGenerator");
        this.noiseGenerator = Objects.requireNonNull(noiseGenerator, "noiseGenerator");

        TerrainSettings settings = noiseGenerator.getTerrainSettings();
        this.seaLevel = settings.seaLevel >= 0 ? settings.seaLevel : 63;

        NoiseSettings noisesettings = defaultNoiseSettings();
        this.l = noisesettings.f() * 4;
        this.m = noisesettings.e() * 4;
        this.f = settings.stoneBlock != null ? ((CraftBlockData) settings.stoneBlock).getState()
                : Blocks.STONE.getBlockData();
        this.g = settings.waterBlock != null ? ((CraftBlockData) settings.waterBlock).getState()
                : Blocks.WATER.getBlockData();
        this.n = 16 / this.m;
        this.o = noisesettings.a() / this.l;
        this.p = 16 / this.m;
    }

    private IBlockData a(double d0, int i) {
        IBlockData iblockdata;
        if (d0 > 0.0D) {
            iblockdata = this.f;
        } else if (i < this.seaLevel) {
            iblockdata = this.g;
        } else {
            iblockdata = k;
        }

        return iblockdata;
    }

    private void a(double[] adouble, int i, int j) {
        // WorldGeneratorApi - call noise function
        noiseGenerator.getNoise(biomeGenerator, adouble, i, j);
    }

    IBlockAccess a(int i, int j) {
        IBlockData[] aiblockdata = new IBlockData[this.o * this.l];
        this.a(i, j, aiblockdata, (Predicate<IBlockData>) null);
        return new BlockColumn(aiblockdata);
    }

    private int a(int i, int j, @Nullable IBlockData[] aiblockdata, @Nullable Predicate<IBlockData> predicate) {
        int k = Math.floorDiv(i, this.m);
        int l = Math.floorDiv(j, this.m);
        int i1 = Math.floorMod(i, this.m);
        int j1 = Math.floorMod(j, this.m);
        double d0 = (double) i1 / (double) this.m;
        double d1 = (double) j1 / (double) this.m;
        double[][] adouble = new double[][] { this.b(k, l), this.b(k, l + 1), this.b(k + 1, l), this.b(k + 1, l + 1) };

        for (int k1 = this.o - 1; k1 >= 0; --k1) {
            double d2 = adouble[0][k1];
            double d3 = adouble[1][k1];
            double d4 = adouble[2][k1];
            double d5 = adouble[3][k1];
            double d6 = adouble[0][k1 + 1];
            double d7 = adouble[1][k1 + 1];
            double d8 = adouble[2][k1 + 1];
            double d9 = adouble[3][k1 + 1];

            for (int l1 = this.l - 1; l1 >= 0; --l1) {
                double d10 = (double) l1 / (double) this.l;
                double d11 = MathHelper.a(d10, d0, d1, d2, d6, d4, d8, d3, d7, d5, d9);
                int i2 = k1 * this.l + l1;
                IBlockData iblockdata = this.a(d11, i2);
                if (aiblockdata != null) {
                    aiblockdata[i2] = iblockdata;
                }

                if (predicate != null && predicate.test(iblockdata)) {
                    return i2 + 1;
                }
            }
        }

        return 0;
    }

    private double[] b(int i, int j) {
        double[] adouble = new double[this.o + 1];
        this.a(adouble, i, j);
        return adouble;
    }


    private void back(ListIterator<?> it, int amount) {
        for (int i = 0; i < amount; i++) {
            it.previous();
        }
    }

    void buildNoise(GeneratorAccess generatoraccess, StructureManager structuremanager,
            IChunkAccess ichunkaccess) {
        List<StructurePiece> objectlist = new ArrayList<StructurePiece>(10);
        List<WorldGenFeatureDefinedStructureJigsawJunction> objectlist1 = new ArrayList<WorldGenFeatureDefinedStructureJigsawJunction>(
                32);
        ChunkCoordIntPair chunkcoordintpair = ichunkaccess.getPos();
        int i = chunkcoordintpair.x;
        int j = chunkcoordintpair.z;
        int k = i << 4;
        int l = j << 4;
        Iterator<StructureGenerator<?>> iterator = StructureGenerator.t.iterator();

        while (iterator.hasNext()) {
            StructureGenerator<?> structuregenerator = iterator.next();
            structuremanager.a(SectionPosition.a(chunkcoordintpair, 0), structuregenerator)
                    .forEach((structurestart) -> {
                        Iterator<StructurePiece> iterator1 = structurestart.d().iterator();

                        while (true) {
                            while (true) {
                                StructurePiece structurepiece;
                                do {
                                    if (!iterator1.hasNext()) {
                                        return;
                                    }

                                    structurepiece = iterator1.next();
                                } while (!structurepiece.a(chunkcoordintpair, 12));

                                if (structurepiece instanceof WorldGenFeaturePillagerOutpostPoolPiece) {
                                    WorldGenFeaturePillagerOutpostPoolPiece worldgenfeaturepillageroutpostpoolpiece = (WorldGenFeaturePillagerOutpostPoolPiece) structurepiece;
                                    Matching worldgenfeaturedefinedstructurepooltemplate_matching = worldgenfeaturepillageroutpostpoolpiece
                                            .b().e();
                                    if (worldgenfeaturedefinedstructurepooltemplate_matching == Matching.RIGID) {
                                        objectlist.add(worldgenfeaturepillageroutpostpoolpiece);
                                    }

                                    Iterator<?> iterator2 = worldgenfeaturepillageroutpostpoolpiece.e().iterator();

                                    while (iterator2.hasNext()) {
                                        WorldGenFeatureDefinedStructureJigsawJunction worldgenfeaturedefinedstructurejigsawjunction = (WorldGenFeatureDefinedStructureJigsawJunction) iterator2
                                                .next();
                                        int i1 = worldgenfeaturedefinedstructurejigsawjunction.a();
                                        int j1 = worldgenfeaturedefinedstructurejigsawjunction.c();
                                        if (i1 > k - 12 && j1 > l - 12 && i1 < k + 15 + 12 && j1 < l + 15 + 12) {
                                            objectlist1.add(worldgenfeaturedefinedstructurejigsawjunction);
                                        }
                                    }
                                } else {
                                    objectlist.add(structurepiece);
                                }
                            }
                        }
                    });
        }

        double[][][] adouble = new double[2][this.p + 1][this.o + 1];

        for (int i1 = 0; i1 < this.p + 1; ++i1) {
            adouble[0][i1] = new double[this.o + 1];
            this.a(adouble[0][i1], i * this.n, j * this.p + i1);
            adouble[1][i1] = new double[this.o + 1];
        }

        ProtoChunk protochunk = (ProtoChunk) ichunkaccess;
        HeightMap heightmap = protochunk.a(Type.OCEAN_FLOOR_WG);
        HeightMap heightmap1 = protochunk.a(Type.WORLD_SURFACE_WG);
        MutableBlockPosition blockposition_mutableblockposition = new MutableBlockPosition();
        ListIterator<StructurePiece> objectlistiterator = objectlist.listIterator();
        ListIterator<WorldGenFeatureDefinedStructureJigsawJunction> objectlistiterator1 = objectlist1.listIterator();

        for (int j1 = 0; j1 < this.n; ++j1) {
            int k1;
            for (k1 = 0; k1 < this.p + 1; ++k1) {
                this.a(adouble[1][k1], i * this.n + j1 + 1, j * this.p + k1);
            }

            for (k1 = 0; k1 < this.p; ++k1) {
                ChunkSection chunksection = protochunk.a(15);
                chunksection.a();

                for (int l1 = this.o - 1; l1 >= 0; --l1) {
                    double d0 = adouble[0][k1][l1];
                    double d1 = adouble[0][k1 + 1][l1];
                    double d2 = adouble[1][k1][l1];
                    double d3 = adouble[1][k1 + 1][l1];
                    double d4 = adouble[0][k1][l1 + 1];
                    double d5 = adouble[0][k1 + 1][l1 + 1];
                    double d6 = adouble[1][k1][l1 + 1];
                    double d7 = adouble[1][k1 + 1][l1 + 1];

                    for (int i2 = this.l - 1; i2 >= 0; --i2) {
                        int j2 = l1 * this.l + i2;
                        int k2 = j2 & 15;
                        int l2 = j2 >> 4;
                        if (chunksection.getYPosition() >> 4 != l2) {
                            chunksection.b();
                            chunksection = protochunk.a(l2);
                            chunksection.a();
                        }

                        double d8 = (double) i2 / (double) this.l;
                        double d9 = MathHelper.d(d8, d0, d4);
                        double d10 = MathHelper.d(d8, d2, d6);
                        double d11 = MathHelper.d(d8, d1, d5);
                        double d12 = MathHelper.d(d8, d3, d7);

                        for (int i3 = 0; i3 < this.m; ++i3) {
                            int j3 = k + j1 * this.m + i3;
                            int k3 = j3 & 15;
                            double d13 = (double) i3 / (double) this.m;
                            double d14 = MathHelper.d(d13, d9, d10);
                            double d15 = MathHelper.d(d13, d11, d12);

                            for (int l3 = 0; l3 < this.m; ++l3) {
                                int i4 = l + k1 * this.m + l3;
                                int j4 = i4 & 15;
                                double d16 = (double) l3 / (double) this.m;
                                double d17 = MathHelper.d(d16, d14, d15);
                                double d18 = MathHelper.a(d17 / 200.0D, -1.0D, 1.0D);

                                int k4;
                                int l4;
                                int i5;
                                for (d18 = d18 / 2.0D - d18 * d18 * d18 / 24.0D; objectlistiterator
                                        .hasNext(); d18 += a(k4, l4, i5) * 0.8D) {
                                    StructurePiece structurepiece = objectlistiterator.next();
                                    StructureBoundingBox structureboundingbox = structurepiece.g();
                                    k4 = Math.max(0,
                                            Math.max(structureboundingbox.a - j3, j3 - structureboundingbox.d));
                                    l4 = j2 - (structureboundingbox.b
                                            + (structurepiece instanceof WorldGenFeaturePillagerOutpostPoolPiece
                                                    ? ((WorldGenFeaturePillagerOutpostPoolPiece) structurepiece).d()
                                                    : 0));
                                    i5 = Math.max(0,
                                            Math.max(structureboundingbox.c - i4, i4 - structureboundingbox.f));
                                }

                                back(objectlistiterator, objectlist.size());

                                while (objectlistiterator1.hasNext()) {
                                    WorldGenFeatureDefinedStructureJigsawJunction worldgenfeaturedefinedstructurejigsawjunction = objectlistiterator1
                                            .next();
                                    int j5 = j3 - worldgenfeaturedefinedstructurejigsawjunction.a();
                                    k4 = j2 - worldgenfeaturedefinedstructurejigsawjunction.b();
                                    l4 = i4 - worldgenfeaturedefinedstructurejigsawjunction.c();
                                    d18 += a(j5, k4, l4) * 0.4D;
                                }

                                back(objectlistiterator1, objectlist1.size());
                                IBlockData iblockdata = this.a(d18, j2);
                                if (iblockdata != NoiseToTerrainGenerator.k) {
                                    if (iblockdata.f() != 0) {
                                        blockposition_mutableblockposition.d(j3, j2, i4);
                                        protochunk.j(blockposition_mutableblockposition);
                                    }

                                    chunksection.setType(k3, k2, j4, iblockdata, false);
                                    heightmap.a(k3, j2, j4, iblockdata);
                                    heightmap1.a(k3, j2, j4, iblockdata);
                                }
                            }
                        }
                    }
                }

                chunksection.b();
            }

            double[][] adouble1 = adouble[0];
            adouble[0] = adouble[1];
            adouble[1] = adouble1;
        }

    }

    int getBaseHeight(int i, int j, Type heightmap_type) {
        // Copied from ChunkGeneratorAbstract
        return this.a(i, j, (IBlockData[]) null, heightmap_type.e());
    }

    @Override
    public int getHeight(BiomeGenerator biomeGenerator, int x, int z, HeightType type) {
        return getBaseHeight(x, z, type == HeightType.OCEAN_FLOOR ? Type.OCEAN_FLOOR_WG : Type.WORLD_SURFACE_WG);
    }

    @Override
    public void setBlocksInChunk(GeneratingChunk chunk) {
        GeneratingChunkImpl chunkImpl = (GeneratingChunkImpl) chunk;
        this.buildNoise(generatorAccess, structureManager, chunkImpl.internal);
    }
}