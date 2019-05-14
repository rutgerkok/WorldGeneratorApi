package nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides;

import java.util.Objects;

import net.minecraft.server.v1_14_R1.BiomeBase;
import net.minecraft.server.v1_14_R1.ChunkGeneratorAbstract;
import net.minecraft.server.v1_14_R1.GeneratorAccess;
import net.minecraft.server.v1_14_R1.GeneratorSettingsDefault;
import net.minecraft.server.v1_14_R1.HeightMap.Type;
import net.minecraft.server.v1_14_R1.NoiseGeneratorOctaves;
import net.minecraft.server.v1_14_R1.WorldChunkManager;
import nl.rutgerkok.worldgeneratorapi.BaseNoiseGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseTerrainGenerator;
import nl.rutgerkok.worldgeneratorapi.internal.BaseTerrainGeneratorImpl;
import nl.rutgerkok.worldgeneratorapi.internal.ReflectionUtil;
import nl.rutgerkok.worldgeneratorapi.internal.bukkitoverrides.InjectedChunkGenerator.GeneratingChunkImpl;

/**
 * Expands a noise generator into a BaseTerrainGenerator. This extends
 * ChunkGeneratorAbstract so that it can use the correct noise calculations.
 *
 */
public final class NoiseToTerrainGenerator extends ChunkGeneratorAbstract<GeneratorSettingsDefault>
        implements BaseTerrainGenerator {

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

    public NoiseToTerrainGenerator(GeneratorAccess access, WorldChunkManager biomeGenerator,
            BaseNoiseGenerator generator) {
        super(access, biomeGenerator, 4, 8, 256, new GeneratorSettingsDefault(), true);
        this.noiseGenerator = Objects.requireNonNull(generator, "generator");

        this.e.a(2620);
        this.noiseOctaves16 = new NoiseGeneratorOctaves(this.e, 16);
    }

    @Override
    public double a(double arg0, double arg1, int arg2) {
        throw new UnsupportedOperationException("double a(double, double, double)");
    }
    @Override
    public void a(double[] arg0, int arg1, int arg2) {
        this.noiseGenerator.getNoise(arg0, arg1, arg2);
    }

    @Override
    protected double[] a(int i, int j) {
        // Biome noise calculator
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