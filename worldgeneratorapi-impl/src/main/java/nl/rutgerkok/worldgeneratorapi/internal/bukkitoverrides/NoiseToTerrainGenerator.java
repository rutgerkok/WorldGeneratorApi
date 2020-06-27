package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import java.util.Objects;

import org.bukkit.craftbukkit.v1_16_R1.block.data.CraftBlockData;

import net.minecraft.server.v1_16_R1.BiomeBase;
import net.minecraft.server.v1_16_R1.ChunkGenerator;
import net.minecraft.server.v1_16_R1.ChunkGeneratorAbstract;
import net.minecraft.server.v1_16_R1.GeneratorAccess;
import net.minecraft.server.v1_16_R1.GeneratorSettingsDefault;
import net.minecraft.server.v1_16_R1.HeightMap.Type;
import net.minecraft.server.v1_16_R1.NoiseGeneratorOctaves;
import net.minecraft.server.v1_16_R1.WorldChunkManager;
import nl.rutgerkok.worldgeneratorapi.BaseNoiseGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseNoiseGenerator.TerrainSettings;
import nl.rutgerkok.worldgeneratorapi.BaseTerrainGenerator;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.internal.BaseTerrainGeneratorImpl;
import nl.rutgerkok.worldgeneratorapi.internal.ReflectionUtil;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.InjectedChunkGenerator.GeneratingChunkImpl;

/**
 * Expands a noise generator into a BaseTerrainGenerator. This extends
 * ChunkGeneratorAbstract so that it can use the correct noise calculations.
 *
 */
public final class NoiseToTerrainGenerator extends ChunkGenerator implements BaseTerrainGenerator {

    /**
     * Forwards some settings to Minecraft.
     */
    private static class GeneratorSettingsCustom extends GeneratorSettingsDefault {
        public GeneratorSettingsCustom(TerrainSettings terrainSettings) {
            if (terrainSettings.stoneBlock != null) {
                this.r = ((CraftBlockData) terrainSettings.stoneBlock).getState();
            }
            if (terrainSettings.waterBlock != null) {
                this.s = ((CraftBlockData) terrainSettings.waterBlock).getState();
            }
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
    private int spawnHeight = -1;
    private BaseNoiseGenerator noiseGenerator;
    private final NoiseGeneratorOctaves noiseOctaves16;

    private BiomeGenerator biomeGenerator;
    private final int seaLevel;

    public NoiseToTerrainGenerator(GeneratorAccess access, WorldChunkManager worldChunkManager,
            BiomeGenerator biomeGenerator, BaseNoiseGenerator noiseGenerator) {
        super(access, worldChunkManager, 4, 8, 256, new GeneratorSettingsCustom(noiseGenerator.getTerrainSettings()),
                true);
        this.biomeGenerator = Objects.requireNonNull(biomeGenerator, "biomeGenerator");
        this.noiseGenerator = Objects.requireNonNull(noiseGenerator, "noiseGenerator");

        this.e.a(2620);
        this.noiseOctaves16 = new NoiseGeneratorOctaves(this.e, 16, 0);

        // Allow changing sea level
        int seaLevel = noiseGenerator.getTerrainSettings().seaLevel;
        this.seaLevel = seaLevel >= 0 ? seaLevel : super.getSeaLevel();
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
    public void a(double[] arg0, int arg1, int arg2) {
        this.noiseGenerator.getNoise(biomeGenerator, arg0, arg1, arg2);
    }

    @Override
    protected double[] a(int i, int j) {
        // Biome noise calculator
        double[] adouble = new double[2];
        float f = 0.0F;
        float f1 = 0.0F;
        float f2 = 0.0F;
        int k = this.getSeaLevel();
        float f3 = this.c.getBiome(i, k, j).i();

        for (int l = -2; l <= 2; ++l) {
            for (int i1 = -2; i1 <= 2; ++i1) {
                BiomeBase biomebase = this.c.getBiome(i + l, k, j + i1);
                float f4 = biomebase.i();
                float f5 = biomebase.m();

                if (f4 < -1.8F) {
                    f4 = -1.8F;
                }

                float f6 = floatArray[l + 2 + (i1 + 2) * 5] / (f4 + 2.0F);
                if (biomebase.i() > f3) {
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
    public int getHeight(int x, int z, HeightType type) {
        return this.getBaseHeight(x, z, BaseTerrainGeneratorImpl.fromApi(type));
    }

    @Override
    public int getSeaLevel() {
        return seaLevel;
    }

    @Override
    public int getSpawnHeight() {
        if (this.spawnHeight == -1) {
            this.spawnHeight = getBaseHeight(0, 0, Type.WORLD_SURFACE_WG);
        }
        return this.spawnHeight;
    }

    @Override
    public void setBlocksInChunk(GeneratingChunk chunk) {
        GeneratingChunkImpl impl = (GeneratingChunkImpl) chunk;
        this.buildNoise(this.a, impl.internal);
    }
}