package nl.rutgerkok.worldgeneratorapi.internal;

import java.util.Objects;

import javax.annotation.Nullable;

import org.bukkit.World;

import nl.rutgerkok.worldgeneratorapi.BaseChunkGenerator;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.WorldGenerator;

final class WorldGeneratorImpl implements WorldGenerator {

    private @Nullable BiomeGenerator biomeGenerator;
    private @Nullable BaseChunkGenerator baseChunkGenerator;
    private final World world;

    WorldGeneratorImpl(World world) {
        this.world = world;
    }

    @Override
    public BaseChunkGenerator getBaseChunkGenerator() throws UnsupportedOperationException {
        BaseChunkGenerator base = this.baseChunkGenerator;
        if (base == null) {
            // In the future, it would be interesting to somehow extract this from the world
            throw new UnsupportedOperationException("I don't know what base chunk generator has been set for world \""
                    + world.getName() + "\" - Minecraft itself or some other plugin is used as the world generator.");
        }
        return base;
    }

    @Override
    public BiomeGenerator getBiomeGenerator() {
        BiomeGenerator biomeGenerator = this.biomeGenerator;
        if (biomeGenerator == null) {
            biomeGenerator = new BiomeGeneratorImpl(world);
            this.biomeGenerator = biomeGenerator;
        }
        return biomeGenerator;
    }

    @Override
    public void setBaseChunkGenerator(BaseChunkGenerator base) {
        this.baseChunkGenerator = Objects.requireNonNull(base, "base");
    }

}
