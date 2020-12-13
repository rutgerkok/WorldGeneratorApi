package nl.rutgerkok.worldgeneratorapi.internal.recording;

import java.util.Objects;

import org.bukkit.World;

import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseNoiseGenerator;
import nl.rutgerkok.worldgeneratorapi.BaseTerrainGenerator;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldRef;
import nl.rutgerkok.worldgeneratorapi.decoration.WorldDecorator;

public class RecordingWorldGeneratorImpl implements WorldGenerator {

    private final WorldGenerator internal;

    private BaseTerrainGenerator baseTerrainGeneratorOrNull;
    private BaseNoiseGenerator baseNoiseGeneratorOrNull;
    private BiomeGenerator biomeGeneratorOrNull;

    public RecordingWorldGeneratorImpl(WorldGenerator internal) {
        this.internal = Objects.requireNonNull(internal, "internal");
    }

    @Override
    public BaseChunkGenerator getBaseChunkGenerator() throws UnsupportedOperationException {
        return this.getBaseTerrainGenerator();
    }

    @Override
    public BaseTerrainGenerator getBaseTerrainGenerator() throws UnsupportedOperationException {
        BaseTerrainGenerator recorded = this.baseTerrainGeneratorOrNull;
        if (recorded != null) {
            return recorded;
        }
        return internal.getBaseTerrainGenerator();
    }

    @Override
    public BiomeGenerator getBiomeGenerator() {
        BiomeGenerator recorded = this.biomeGeneratorOrNull;
        if (recorded != null) {
            return recorded;
        }
        return internal.getBiomeGenerator();
    }

    @Override
    public World getWorld() {
        return internal.getWorld();
    }

    @Override
    public WorldDecorator getWorldDecorator() throws UnsupportedOperationException {
        return internal.getWorldDecorator();
    }

    @Override
    public WorldRef getWorldRef() {
        return internal.getWorldRef();
    }

    public void reapply(WorldGenerator worldGenerator) {
        BaseNoiseGenerator recordedNoise = this.baseNoiseGeneratorOrNull;
        if (recordedNoise != null) {
            worldGenerator.setBaseNoiseGenerator(recordedNoise);
        }

        BaseTerrainGenerator recordedTerrain = this.baseTerrainGeneratorOrNull;
        if (recordedTerrain != null) {
            worldGenerator.setBaseTerrainGenerator(recordedTerrain);
        }

        BiomeGenerator recordedBiomes = this.biomeGeneratorOrNull;
        if (recordedBiomes != null) {
            worldGenerator.setBiomeGenerator(recordedBiomes);
        }
    }

    @Override
    public void setBaseChunkGenerator(BaseChunkGenerator base) {
        setBaseTerrainGenerator(new BaseTerrainGenerator() {

            @Override
            public int getHeight(BiomeGenerator biomeGenerator, int x, int z, HeightType type) {
                return  65; // Whatever, we don't know
            }

            @Override
            public void setBlocksInChunk(GeneratingChunk chunk) {
                base.setBlocksInChunk(chunk);
            }
        });
    }

    @Override
    public BaseTerrainGenerator setBaseNoiseGenerator(BaseNoiseGenerator base) {
        this.baseNoiseGeneratorOrNull = Objects.requireNonNull(base, "base");
        this.baseTerrainGeneratorOrNull = null; // Only one of the two can be active
        return internal.setBaseNoiseGenerator(base);
    }

    @Override
    public void setBaseTerrainGenerator(BaseTerrainGenerator base) {
        this.baseTerrainGeneratorOrNull = Objects.requireNonNull(base, "base");
        this.baseNoiseGeneratorOrNull = null; // Only one of the two can be active
        internal.setBaseTerrainGenerator(base);
    }

    @Override
    public void setBiomeGenerator(BiomeGenerator biomeGenerator) {
        this.biomeGeneratorOrNull = Objects.requireNonNull(biomeGenerator, "biomeGenerator");
        internal.setBiomeGenerator(biomeGenerator);
    }

}
