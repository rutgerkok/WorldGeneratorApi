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

/**
 * Only records the changes to a world generator, without actually applying
 * them. Useful for capturing settings, and then applying those at a later point
 * in time, or to another world.
 *
 */
public final class RecordingWorldGeneratorImpl implements WorldGenerator {

    private final WorldGenerator internal;

    private BaseTerrainGenerator baseTerrainGeneratorOrNull;
    private BiomeGenerator biomeGeneratorOrNull;
    private RecordingWorldDecoratorImpl worldDecoratorOrNull;


    public RecordingWorldGeneratorImpl(WorldGenerator internal) {
        this.internal = Objects.requireNonNull(internal, "internal");
    }

    /**
     * Applies all settings to the given world generator.
     * @param worldGenerator The world generator.
     */
    public void applyTo(WorldGenerator worldGenerator) {
        BaseTerrainGenerator recordedTerrain = this.baseTerrainGeneratorOrNull;
        if (recordedTerrain != null) {
            worldGenerator.setBaseTerrainGenerator(recordedTerrain);
        }

        BiomeGenerator recordedBiomes = this.biomeGeneratorOrNull;
        if (recordedBiomes != null) {
            worldGenerator.setBiomeGenerator(recordedBiomes);
        }

        RecordingWorldDecoratorImpl recordedDecorations = this.worldDecoratorOrNull;
        if (recordedDecorations != null) {
            recordedDecorations.applyTo(worldGenerator.getWorldDecorator());
        }
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
        RecordingWorldDecoratorImpl worldDecorator = this.worldDecoratorOrNull;
        if (worldDecorator == null) {
            // Need to initialize. This code cannot be moved to the constructor of this
            // class, since for some modded worlds it's not possible to get the world
            // decorator
            worldDecorator = new RecordingWorldDecoratorImpl(internal.getWorldDecorator());
            this.worldDecoratorOrNull = worldDecorator;
        }
        return worldDecorator;
    }

    @Override
    public WorldRef getWorldRef() {
        return internal.getWorldRef();
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
    public void setBaseTerrainGenerator(BaseTerrainGenerator base) {
        this.baseTerrainGeneratorOrNull = Objects.requireNonNull(base, "base");
    }

    @Override
    public void setBiomeGenerator(BiomeGenerator biomeGenerator) {
        this.biomeGeneratorOrNull = Objects.requireNonNull(biomeGenerator, "biomeGenerator");
    }

    @Override
    public BaseTerrainGenerator toBaseTerrainGenerator(BaseNoiseGenerator base) {
        return internal.toBaseTerrainGenerator(base);
    }

}
